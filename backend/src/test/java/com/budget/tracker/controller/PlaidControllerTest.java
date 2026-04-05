package com.budget.tracker.controller;

import com.budget.tracker.dto.plaid.BankAccountResponse;
import com.budget.tracker.dto.plaid.LinkTokenResponse;
import com.budget.tracker.dto.plaid.SyncResponse;
import com.budget.tracker.filter.JwtAuthenticationFilter;
import com.budget.tracker.model.Role;
import com.budget.tracker.model.User;
import com.budget.tracker.service.JwtService;
import com.budget.tracker.service.PlaidService;
import com.budget.tracker.service.UserDetailsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice tests for PlaidController using @WebMvcTest.
 *
 * Security notes:
 *   - All /api/plaid/** endpoints (except /api/plaid/webhook which is not tested here) require
 *     authentication per SecurityConfig; SecurityMockMvcRequestPostProcessors.user() injects a
 *     User principal without going through the JWT filter.
 *   - JwtAuthenticationFilter and UserDetailsServiceImpl are @MockBean to satisfy SecurityConfig's
 *     constructor injection.
 *   - CSRF is disabled in SecurityConfig so no csrf() post-processor is needed.
 *   - ExchangeTokenRequest has fields: publicToken, institutionName, accountName, accountMask,
 *     accountType. All are plain strings with no validation constraints.
 */
@WebMvcTest(PlaidController.class)
class PlaidControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PlaidService plaidService;

    // Required by SecurityConfig constructor injection
    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .username("alice")
                .email("alice@test.com")
                .password("encoded")
                .role(Role.USER)
                .build();
    }

    /** Shorthand to inject the mock User as the authenticated principal. */
    private SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor withMockUser() {
        return SecurityMockMvcRequestPostProcessors.user(mockUser);
    }

    private BankAccountResponse buildBankAccountResponse(Long id, String institution, String accountName, String mask) {
        return BankAccountResponse.builder()
                .id(id)
                .institutionName(institution)
                .accountName(accountName)
                .accountMask(mask)
                .accountType("depository")
                .connectedAt(LocalDateTime.of(2026, 3, 1, 10, 0))
                .build();
    }

    // -------------------------------------------------------------------------
    // POST /api/plaid/link-token
    // -------------------------------------------------------------------------

    @Test
    void createLinkToken_returnsLinkToken() throws Exception {
        when(plaidService.createLinkToken(any())).thenReturn(new LinkTokenResponse("link-token-abc"));

        mockMvc.perform(post("/api/plaid/link-token")
                        .with(withMockUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkToken").value("link-token-abc"));
    }

    @Test
    void createLinkToken_withoutAuth_returns401or403() throws Exception {
        mockMvc.perform(post("/api/plaid/link-token"))
                .andExpect(status().is4xxClientError());
    }

    // -------------------------------------------------------------------------
    // GET /api/plaid/accounts
    // -------------------------------------------------------------------------

    @Test
    void getAccounts_returnsAccountList() throws Exception {
        BankAccountResponse account = buildBankAccountResponse(1L, "Chase", "Checking", "1234");
        when(plaidService.getConnectedAccounts(any())).thenReturn(List.of(account));

        mockMvc.perform(get("/api/plaid/accounts")
                        .with(withMockUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].institutionName").value("Chase"))
                .andExpect(jsonPath("$[0].accountName").value("Checking"))
                .andExpect(jsonPath("$[0].accountMask").value("1234"))
                .andExpect(jsonPath("$[0].accountType").value("depository"));
    }

    @Test
    void getAccounts_withMultipleAccounts_returnsAll() throws Exception {
        List<BankAccountResponse> accounts = List.of(
                buildBankAccountResponse(1L, "Chase", "Checking", "1234"),
                buildBankAccountResponse(2L, "Wells Fargo", "Savings", "5678")
        );
        when(plaidService.getConnectedAccounts(any())).thenReturn(accounts);

        mockMvc.perform(get("/api/plaid/accounts")
                        .with(withMockUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].institutionName").value("Wells Fargo"))
                .andExpect(jsonPath("$[1].accountMask").value("5678"));
    }

    @Test
    void getAccounts_withoutAuth_returns401or403() throws Exception {
        mockMvc.perform(get("/api/plaid/accounts"))
                .andExpect(status().is4xxClientError());
    }

    // -------------------------------------------------------------------------
    // POST /api/plaid/exchange-token
    // -------------------------------------------------------------------------

    @Test
    void exchangeToken_returns200WithAccountResponse() throws Exception {
        BankAccountResponse account = buildBankAccountResponse(2L, "Wells Fargo", "Savings", "5678");
        when(plaidService.exchangePublicToken(any(), any())).thenReturn(account);

        mockMvc.perform(post("/api/plaid/exchange-token")
                        .with(withMockUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "publicToken", "public-sandbox-abc",
                                "institutionName", "Wells Fargo",
                                "accountName", "Savings",
                                "accountMask", "5678",
                                "accountType", "depository"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.institutionName").value("Wells Fargo"))
                .andExpect(jsonPath("$.accountName").value("Savings"))
                .andExpect(jsonPath("$.accountMask").value("5678"));
    }

    @Test
    void exchangeToken_withoutAuth_returns401or403() throws Exception {
        mockMvc.perform(post("/api/plaid/exchange-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is4xxClientError());
    }

    // -------------------------------------------------------------------------
    // DELETE /api/plaid/accounts/{id}
    // -------------------------------------------------------------------------

    @Test
    void disconnectAccount_returns204() throws Exception {
        doNothing().when(plaidService).disconnectAccount(eq(1L), any());

        mockMvc.perform(delete("/api/plaid/accounts/1")
                        .with(withMockUser()))
                .andExpect(status().isNoContent());
    }

    @Test
    void disconnectAccount_withoutAuth_returns401or403() throws Exception {
        mockMvc.perform(delete("/api/plaid/accounts/1"))
                .andExpect(status().is4xxClientError());
    }

    // -------------------------------------------------------------------------
    // POST /api/plaid/sync
    // -------------------------------------------------------------------------

    @Test
    void syncTransactions_returnsSyncResponse() throws Exception {
        SyncResponse syncResponse = new SyncResponse(15, 2, "Synced 1 accounts: 15 new transactions");
        when(plaidService.syncAll(any())).thenReturn(syncResponse);

        mockMvc.perform(post("/api/plaid/sync")
                        .with(withMockUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(15))
                .andExpect(jsonPath("$.skipped").value(2))
                .andExpect(jsonPath("$.message").value("Synced 1 accounts: 15 new transactions"));
    }

    @Test
    void syncTransactions_withoutAuth_returns401or403() throws Exception {
        mockMvc.perform(post("/api/plaid/sync"))
                .andExpect(status().is4xxClientError());
    }

    // -------------------------------------------------------------------------
    // Authentication wall — consolidated check across all guarded endpoints
    // -------------------------------------------------------------------------

    @Test
    void allMutatingEndpoints_requireAuthentication() throws Exception {
        // POST /link-token
        mockMvc.perform(post("/api/plaid/link-token"))
                .andExpect(status().is4xxClientError());

        // GET /accounts
        mockMvc.perform(get("/api/plaid/accounts"))
                .andExpect(status().is4xxClientError());

        // POST /exchange-token
        mockMvc.perform(post("/api/plaid/exchange-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is4xxClientError());

        // DELETE /accounts/1
        mockMvc.perform(delete("/api/plaid/accounts/1"))
                .andExpect(status().is4xxClientError());

        // POST /sync
        mockMvc.perform(post("/api/plaid/sync"))
                .andExpect(status().is4xxClientError());
    }
}
