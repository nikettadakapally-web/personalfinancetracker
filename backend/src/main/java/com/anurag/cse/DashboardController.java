package com.anurag.cse;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;
    private final SavingGoalRepository goalRepository;
    private final AuthService authService;
    private final GamificationService gamificationService;

    public DashboardController(ExpenseRepository expenseRepository, BudgetRepository budgetRepository,
                               SavingGoalRepository goalRepository, AuthService authService,
                               GamificationService gamificationService) {
        this.expenseRepository = expenseRepository;
        this.budgetRepository = budgetRepository;
        this.goalRepository = goalRepository;
        this.authService = authService;
        this.gamificationService = gamificationService;
    }

    @GetMapping("/summary")
    public Map<String, Object> getFinancialSummary(@RequestHeader(value = "Authorization", required = false) String authorization) {
        List<Expense> all = expenseRepository.findByOwnerId(authService.requireUser(authorization).getId());

        double totalIncome = 0.0;
        double totalExpenses = 0.0;
        Map<String, Double> categoryBreakdown = new HashMap<>();

        for (Expense e : all) {
            double amt = (e.getAmount() != null) ? e.getAmount() : 0.0;
            String type = (e.getType() != null) ? e.getType() : "expense";
            String category = (e.getCategory() != null) ? e.getCategory() : "general";

            if ("income".equalsIgnoreCase(type)) {
                totalIncome += amt;
            } else {
                totalExpenses += amt;
                categoryBreakdown.put(category, categoryBreakdown.getOrDefault(category, 0.0) + amt);
            }
        }

        double netSavings = totalIncome - totalExpenses;
        double savingsRate = (totalIncome > 0) ? (netSavings / totalIncome) * 100.0 : 0.0;

        Map<String, Object> response = new HashMap<>();
        response.put("totalIncome", totalIncome);
        response.put("totalExpenses", totalExpenses);
        response.put("netSavings", netSavings);
        response.put("savingsRate", Math.round(savingsRate * 10.0) / 10.0);
        response.put("categoryBreakdown", categoryBreakdown);

        return response;
    }

    @GetMapping("/gamification")
    public Map<String, Object> getGamification(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Long ownerId = authService.requireUser(authorization).getId();
        return gamificationService.calculate(
                expenseRepository.findByOwnerId(ownerId),
                budgetRepository.findByOwnerId(ownerId),
                goalRepository.findByOwnerId(ownerId),
                java.time.LocalDate.now());
    }
}
