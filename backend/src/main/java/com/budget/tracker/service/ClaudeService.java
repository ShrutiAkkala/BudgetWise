package com.budget.tracker.service;

import com.budget.tracker.dto.summary.CategorySummary;
import com.budget.tracker.dto.summary.MonthlySummaryResponse;
import com.budget.tracker.model.Transaction;
import com.budget.tracker.model.User;
import com.budget.tracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaudeService {

    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;
    private final RestTemplate restTemplate;

    @Value("${anthropic.api-key}")
    private String apiKey;

    @Value("${anthropic.model}")
    private String model;

    private static final String ANTHROPIC_URL = "https://api.anthropic.com/v1/messages";

    /**
     * RAG: Retrieve user's financial data, augment the prompt, generate answer.
     */
    public String chat(String userQuestion, User user) {
        // ── RETRIEVAL ──────────────────────────────────────────────
        // Get current month summary
        LocalDate now = LocalDate.now();
        MonthlySummaryResponse summary = transactionService.getSummary(
                user, now.getYear(), now.getMonthValue());

        // Get last 3 months of transactions for richer context
        LocalDate threeMonthsAgo = now.minusMonths(3).withDayOfMonth(1);
        List<Transaction> recentTx = transactionRepository
                .findByUserIdAndDateBetweenOrderByDateDesc(
                        user.getId(), threeMonthsAgo, now);

        // ── AUGMENTATION ───────────────────────────────────────────
        String context = buildContext(summary, recentTx, now);

        // ── GENERATION ─────────────────────────────────────────────
        return callClaude(userQuestion, context);
    }

    private String buildContext(MonthlySummaryResponse summary,
                                List<Transaction> transactions,
                                LocalDate now) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== USER FINANCIAL DATA ===\n\n");

        // Monthly summary
        sb.append("CURRENT MONTH (").append(now.getMonth()).append(" ").append(now.getYear()).append("):\n");
        sb.append("  Total Income:   $").append(summary.getTotalIncome()).append("\n");
        sb.append("  Total Expenses: $").append(summary.getTotalExpenses()).append("\n");
        sb.append("  Net Balance:    $").append(summary.getNetBalance()).append("\n\n");

        // Category breakdown
        if (summary.getExpensesByCategory() != null && !summary.getExpensesByCategory().isEmpty()) {
            sb.append("EXPENSES BY CATEGORY THIS MONTH:\n");
            summary.getExpensesByCategory().stream()
                    .sorted((a, b) -> b.getTotal().compareTo(a.getTotal()))
                    .forEach(c -> sb.append("  ")
                            .append(c.getCategory()).append(": $").append(c.getTotal()).append("\n"));
            sb.append("\n");
        }

        // Recent transactions (last 30 entries)
        sb.append("RECENT TRANSACTIONS (last 3 months):\n");
        transactions.stream().limit(30).forEach(t ->
                sb.append("  ").append(t.getDate())
                  .append(" | ").append(t.getType())
                  .append(" | $").append(t.getAmount())
                  .append(" | ").append(t.getCategory())
                  .append(" | ").append(t.getDescription())
                  .append("\n")
        );

        return sb.toString();
    }

    private String callClaude(String question, String context) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        String systemPrompt = """
                You are a smart, friendly personal finance assistant.
                You have access to the user's real transaction data below.

                Rules:
                - Answer ONLY based on the data provided — never make up numbers.
                - Be specific: quote actual dollar amounts from the data.
                - Give 1-3 concrete, actionable suggestions when asked about cutting back.
                - Keep answers concise (3-5 sentences max).
                - If the data doesn't have enough info to answer, say so honestly.
                """;

        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", 512,
                "system", systemPrompt,
                "messages", List.of(
                        Map.of("role", "user",
                               "content", "Here is my financial data:\n\n" + context
                                          + "\n\nMy question: " + question)
                )
        );

        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(ANTHROPIC_URL, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().get("content");
                if (content != null && !content.isEmpty()) {
                    return (String) content.get(0).get("text");
                }
            }
        } catch (Exception e) {
            log.error("Claude API call failed: {}", e.getMessage());
            return "Sorry, I couldn't connect to the AI service right now. Make sure your ANTHROPIC_API_KEY is set.";
        }

        return "Sorry, I couldn't get a response. Please try again.";
    }
}
