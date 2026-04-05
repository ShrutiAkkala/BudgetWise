package com.budget.tracker.service;

import com.budget.tracker.dto.plaid.*;
import com.budget.tracker.model.*;
import com.budget.tracker.repository.BankAccountRepository;
import com.budget.tracker.repository.TransactionRepository;
import com.plaid.client.request.PlaidApi;
import com.plaid.client.model.CountryCode;
import com.plaid.client.model.ItemPublicTokenExchangeRequest;
import com.plaid.client.model.ItemPublicTokenExchangeResponse;
import com.plaid.client.model.ItemRemoveRequest;
import com.plaid.client.model.LinkTokenCreateRequest;
import com.plaid.client.model.LinkTokenCreateRequestUser;
import com.plaid.client.model.LinkTokenCreateResponse;
import com.plaid.client.model.PersonalFinanceCategory;
import com.plaid.client.model.Products;
import com.plaid.client.model.TransactionsSyncRequest;
import com.plaid.client.model.TransactionsSyncResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import retrofit2.Response;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaidService {

    private final PlaidApi plaidClient;
    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;

    @Value("${plaid.client-id}")
    private String clientId;

    public LinkTokenResponse createLinkToken(User user) throws IOException {
        LinkTokenCreateRequestUser linkUser = new LinkTokenCreateRequestUser()
                .clientUserId(user.getId().toString());

        LinkTokenCreateRequest request = new LinkTokenCreateRequest()
                .user(linkUser)
                .clientName("Budget Tracker")
                .products(List.of(Products.TRANSACTIONS))
                .countryCodes(List.of(CountryCode.US))
                .language("en");

        Response<LinkTokenCreateResponse> response = plaidClient.linkTokenCreate(request).execute();
        if (!response.isSuccessful() || response.body() == null) {
            throw new RuntimeException("Failed to create Plaid link token");
        }
        return new LinkTokenResponse(response.body().getLinkToken());
    }

    public BankAccountResponse exchangePublicToken(ExchangeTokenRequest request, User user) throws IOException {
        ItemPublicTokenExchangeRequest exchangeRequest = new ItemPublicTokenExchangeRequest()
                .publicToken(request.getPublicToken());

        Response<ItemPublicTokenExchangeResponse> response = plaidClient
                .itemPublicTokenExchange(exchangeRequest).execute();

        if (!response.isSuccessful() || response.body() == null) {
            throw new RuntimeException("Failed to exchange Plaid public token");
        }

        String accessToken = response.body().getAccessToken();
        String itemId = response.body().getItemId();

        BankAccount account = BankAccount.builder()
                .user(user)
                .plaidItemId(itemId)
                .plaidAccessToken(accessToken)
                .institutionName(request.getInstitutionName() != null ? request.getInstitutionName() : "Bank")
                .accountName(request.getAccountName() != null ? request.getAccountName() : "Checking Account")
                .accountMask(request.getAccountMask())
                .accountType(request.getAccountType() != null ? request.getAccountType() : "depository")
                .build();

        account = bankAccountRepository.save(account);
        // Trigger initial sync
        syncTransactions(account);
        return toBankAccountResponse(account);
    }

    public List<BankAccountResponse> getConnectedAccounts(User user) {
        return bankAccountRepository.findByUserId(user.getId())
                .stream().map(this::toBankAccountResponse).collect(Collectors.toList());
    }

    public void disconnectAccount(Long accountId, User user) throws IOException {
        BankAccount account = bankAccountRepository.findByIdAndUserId(accountId, user.getId())
                .orElseThrow(() -> new RuntimeException("Bank account not found"));

        // Remove from Plaid
        ItemRemoveRequest removeRequest = new ItemRemoveRequest()
                .accessToken(account.getPlaidAccessToken());
        plaidClient.itemRemove(removeRequest).execute();

        bankAccountRepository.delete(account);
    }

    public SyncResponse syncAll(User user) throws IOException {
        List<BankAccount> accounts = bankAccountRepository.findByUserId(user.getId());
        int totalImported = 0;
        int totalSkipped = 0;
        for (BankAccount account : accounts) {
            int[] result = syncTransactions(account);
            totalImported += result[0];
            totalSkipped += result[1];
        }
        return new SyncResponse(totalImported, totalSkipped,
                String.format("Synced %d accounts: %d new transactions", accounts.size(), totalImported));
    }

    private int[] syncTransactions(BankAccount account) throws IOException {
        int imported = 0;
        int skipped = 0;
        boolean hasMore = true;
        String cursor = account.getNextCursor();

        while (hasMore) {
            TransactionsSyncRequest syncRequest = new TransactionsSyncRequest()
                    .accessToken(account.getPlaidAccessToken());
            if (cursor != null) {
                syncRequest.cursor(cursor);
            }

            Response<TransactionsSyncResponse> response = plaidClient.transactionsSync(syncRequest).execute();
            if (!response.isSuccessful() || response.body() == null) {
                log.error("Failed to sync transactions for account {}", account.getId());
                break;
            }

            TransactionsSyncResponse body = response.body();
            for (com.plaid.client.model.Transaction plaidTx : body.getAdded()) {
                if (transactionRepository.existsByPlaidTransactionId(plaidTx.getTransactionId())) {
                    skipped++;
                    continue;
                }

                com.budget.tracker.model.Transaction tx = com.budget.tracker.model.Transaction.builder()
                        .user(account.getUser())
                        .bankAccount(account)
                        .plaidTransactionId(plaidTx.getTransactionId())
                        .plaidImported(true)
                        .amount(BigDecimal.valueOf(Math.abs(plaidTx.getAmount())))
                        .description(plaidTx.getName())
                        .category(mapPlaidCategory(plaidTx.getPersonalFinanceCategory()))
                        .type(plaidTx.getAmount() > 0 ? TransactionType.EXPENSE : TransactionType.INCOME)
                        .date(LocalDate.parse(plaidTx.getDate().toString()))
                        .build();

                transactionRepository.save(tx);
                imported++;
            }

            cursor = body.getNextCursor();
            hasMore = body.getHasMore();
        }

        account.setNextCursor(cursor);
        bankAccountRepository.save(account);
        return new int[]{imported, skipped};
    }

    private com.budget.tracker.model.Category mapPlaidCategory(PersonalFinanceCategory pfc) {
        if (pfc == null) return com.budget.tracker.model.Category.OTHER;
        String primary = pfc.getPrimary().toUpperCase();
        if (primary.contains("FOOD") || primary.contains("RESTAURANT")) return com.budget.tracker.model.Category.FOOD;
        if (primary.contains("TRANSPORT") || primary.contains("TRAVEL")) return com.budget.tracker.model.Category.TRANSPORT;
        if (primary.contains("RENT") || primary.contains("HOUSING") || primary.contains("HOME")) return com.budget.tracker.model.Category.HOUSING;
        if (primary.contains("ENTERTAINMENT") || primary.contains("RECREATION")) return com.budget.tracker.model.Category.ENTERTAINMENT;
        if (primary.contains("HEALTH") || primary.contains("MEDICAL")) return com.budget.tracker.model.Category.HEALTH;
        if (primary.contains("SHOP") || primary.contains("MERCH")) return com.budget.tracker.model.Category.SHOPPING;
        if (primary.contains("EDUCATION")) return com.budget.tracker.model.Category.EDUCATION;
        return com.budget.tracker.model.Category.OTHER;
    }

    private BankAccountResponse toBankAccountResponse(BankAccount a) {
        return BankAccountResponse.builder()
                .id(a.getId())
                .institutionName(a.getInstitutionName())
                .accountName(a.getAccountName())
                .accountMask(a.getAccountMask())
                .accountType(a.getAccountType())
                .connectedAt(a.getConnectedAt())
                .build();
    }
}
