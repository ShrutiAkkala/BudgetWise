package com.budget.tracker.controller;

import com.budget.tracker.dto.summary.DailySpending;
import com.budget.tracker.dto.summary.MonthlySummaryResponse;
import com.budget.tracker.dto.transaction.TransactionResponse;
import com.budget.tracker.filter.JwtAuthenticationFilter;
import com.budget.tracker.model.Category;
import com.budget.tracker.model.Role;
import com.budget.tracker.model.TransactionType;
import com.budget.tracker.model.User;
import com.budget.tracker.service.JwtService;
import com.budget.tracker.service.TransactionService;
import com.budget.tracker.service.UserDetailsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice tests for TransactionController using @WebMvcTest.
 *
 * Security notes:
 *   - SecurityConfig requires JwtAuthenticationFilter and UserDetailsServiceImpl, both provided
 *     as @MockBean so the application context loads successfully.
 *   - Authenticated endpoints use SecurityMockMvcRequestPostProcessors.user(mockUser) to inject
 *     a real User (implements UserDetails) as @AuthenticationPrincipal directly, bypassing JWT.
 *   - CSRF is disabled in SecurityConfig so no csrf() post-processor is needed for mutating
 *     requests; it is omitted for clarity.
 *   - The ObjectMapper is manually constructed with JavaTimeModule in @BeforeEach to handle
 *     LocalDate serialization in request bodies (the autowired one from the test context also
 *     works, but explicit registration makes the dependency clear).
 */
@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Required by SecurityConfig constructor injection
    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private TransactionService transactionService;

    private ObjectMapper objectMapper;
    private User mockUser;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
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

    private TransactionResponse buildResponse() {
        return TransactionResponse.builder()
                .id(1L)
                .amount(new BigDecimal("50.00"))
                .description("Lunch")
                .category(Category.FOOD)
                .type(TransactionType.EXPENSE)
                .date(LocalDate.of(2026, 3, 10))
                .plaidImported(false)
                .build();
    }

    // -------------------------------------------------------------------------
    // POST /api/transactions
    // -------------------------------------------------------------------------

    @Test
    void createTransaction_withValidRequest_returns201() throws Exception {
        when(transactionService.create(any(), any())).thenReturn(buildResponse());

        mockMvc.perform(post("/api/transactions")
                        .with(withMockUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "amount", 50.00,
                                "description", "Lunch",
                                "category", "FOOD",
                                "type", "EXPENSE",
                                "date", "2026-03-10"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").value("Lunch"))
                .andExpect(jsonPath("$.category").value("FOOD"))
                .andExpect(jsonPath("$.type").value("EXPENSE"));
    }

    @Test
    void createTransaction_withMissingDescription_returns400() throws Exception {
        // TransactionRequest: @NotBlank on description — omitting the field sends null
        mockMvc.perform(post("/api/transactions")
                        .with(withMockUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "amount", 50.00,
                                "category", "FOOD",
                                "type", "EXPENSE",
                                "date", "2026-03-10"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTransaction_withMissingAmount_returns400() throws Exception {
        // TransactionRequest: @NotNull on amount
        mockMvc.perform(post("/api/transactions")
                        .with(withMockUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "description", "Lunch",
                                "category", "FOOD",
                                "type", "EXPENSE",
                                "date", "2026-03-10"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTransaction_withoutAuth_returns401or403() throws Exception {
        // No user principal — SecurityConfig requires authentication for /api/transactions
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is4xxClientError());
    }

    // -------------------------------------------------------------------------
    // GET /api/transactions
    // -------------------------------------------------------------------------

    @Test
    void getTransactions_returnsListForAuthenticatedUser() throws Exception {
        when(transactionService.getAll(any(), isNull(), isNull()))
                .thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/api/transactions")
                        .with(withMockUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].description").value("Lunch"))
                .andExpect(jsonPath("$[0].category").value("FOOD"));
    }

    @Test
    void getTransactions_withMonthYearFilter_passesParamsToService() throws Exception {
        when(transactionService.getAll(any(), eq(2026), eq(3)))
                .thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/api/transactions?year=2026&month=3")
                        .with(withMockUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].description").value("Lunch"));
    }

    @Test
    void getTransactions_withoutAuth_returns401or403() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().is4xxClientError());
    }

    // -------------------------------------------------------------------------
    // PUT /api/transactions/{id}
    // -------------------------------------------------------------------------

    @Test
    void updateTransaction_returns200() throws Exception {
        when(transactionService.update(eq(1L), any(), any())).thenReturn(buildResponse());

        mockMvc.perform(put("/api/transactions/1")
                        .with(withMockUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "amount", 60.00,
                                "description", "Dinner",
                                "category", "FOOD",
                                "type", "EXPENSE",
                                "date", "2026-03-10"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateTransaction_withoutAuth_returns401or403() throws Exception {
        mockMvc.perform(put("/api/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is4xxClientError());
    }

    // -------------------------------------------------------------------------
    // DELETE /api/transactions/{id}
    // -------------------------------------------------------------------------

    @Test
    void deleteTransaction_returns204() throws Exception {
        mockMvc.perform(delete("/api/transactions/1")
                        .with(withMockUser()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTransaction_withoutAuth_returns401or403() throws Exception {
        mockMvc.perform(delete("/api/transactions/1"))
                .andExpect(status().is4xxClientError());
    }

    // -------------------------------------------------------------------------
    // GET /api/transactions/summary
    // -------------------------------------------------------------------------

    @Test
    void getSummary_returnsMonthlyData() throws Exception {
        MonthlySummaryResponse summary = MonthlySummaryResponse.builder()
                .year(2026)
                .month(3)
                .totalIncome(new BigDecimal("3000.00"))
                .totalExpenses(new BigDecimal("1200.00"))
                .netBalance(new BigDecimal("1800.00"))
                .expensesByCategory(List.of())
                .incomeByCategory(List.of())
                .build();
        when(transactionService.getSummary(any(), eq(2026), eq(3))).thenReturn(summary);

        mockMvc.perform(get("/api/transactions/summary?year=2026&month=3")
                        .with(withMockUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").value(3))
                .andExpect(jsonPath("$.totalIncome").value(3000.00))
                .andExpect(jsonPath("$.totalExpenses").value(1200.00))
                .andExpect(jsonPath("$.netBalance").value(1800.00));
    }

    @Test
    void getSummary_withoutAuth_returns401or403() throws Exception {
        mockMvc.perform(get("/api/transactions/summary?year=2026&month=3"))
                .andExpect(status().is4xxClientError());
    }

    // -------------------------------------------------------------------------
    // GET /api/transactions/daily
    // -------------------------------------------------------------------------

    @Test
    void getDailySpending_returnsDailyData() throws Exception {
        when(transactionService.getDailySpending(any(), eq(2026), eq(3)))
                .thenReturn(List.of(
                        new DailySpending(5, new BigDecimal("120.00")),
                        new DailySpending(15, new BigDecimal("80.00"))
                ));

        mockMvc.perform(get("/api/transactions/daily?year=2026&month=3")
                        .with(withMockUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].day").value(5))
                .andExpect(jsonPath("$[0].amount").value(120.00))
                .andExpect(jsonPath("$[1].day").value(15))
                .andExpect(jsonPath("$[1].amount").value(80.00));
    }

    @Test
    void getDailySpending_withoutAuth_returns401or403() throws Exception {
        mockMvc.perform(get("/api/transactions/daily?year=2026&month=3"))
                .andExpect(status().is4xxClientError());
    }
}
