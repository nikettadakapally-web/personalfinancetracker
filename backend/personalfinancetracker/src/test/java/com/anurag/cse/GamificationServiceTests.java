package com.anurag.cse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GamificationServiceTests {
    private final GamificationService service = new GamificationService();
    private final LocalDate today = LocalDate.of(2026, 10, 7);

    @Test
    void calculatesHealthScoreStreakAndGoalEstimate() {
        List<Expense> expenses = List.of(
                expense("Salary", 10_000, "income", "income", today.minusDays(6), null),
                expense("Utilities", 2_000, "expense", "bills", today.minusDays(5), false),
                expense("Loan payment", 1_000, "expense", "debt", today.minusDays(4), false),
                expense("Shopping", 500, "expense", "shopping", today.minusDays(3), null));
        Budget bills = budget("bills", 1_000);
        Budget debt = budget("debt", 1_000);
        Budget shopping = budget("shopping", 500);
        SavingGoal goal = new SavingGoal("Emergency Fund", 1_100.0, 100.0, null, "emergency");
        goal.setId(7L);

        Map<String, Object> result = service.calculate(expenses, List.of(bills, debt, shopping),
                List.of(goal), today);

        assertEquals(90, result.get("score"));
        assertEquals(65.0, result.get("savingsRate"));
        assertEquals(71.4, result.get("budgetAdherence"));
        assertEquals(10.0, result.get("debtToIncome"));
        assertEquals(3, result.get("noSpendStreak"));
        @SuppressWarnings("unchecked")
        Map<String, Object> milestone = ((List<Map<String, Object>>) result.get("goals")).get(0);
        assertEquals(9L, milestone.get("progress"));
        assertEquals("2026-11-01", milestone.get("estimatedCompletionDate"));
    }

    @Test
    void leavesUnavailableMetricsUnreportedAndUsesNeutralScoreComponents() {
        Map<String, Object> result = service.calculate(List.of(), List.of(), List.of(), today);

        assertEquals(50, result.get("score"));
        assertNull(result.get("savingsRate"));
        assertNull(result.get("budgetAdherence"));
        assertNull(result.get("debtToIncome"));
        assertEquals(0, result.get("noSpendStreak"));
    }

    @Test
    void discretionarySpendingTodayResetsTheStreak() {
        Expense purchase = expense("Shopping", 50, "expense", "shopping", today, true);

        Map<String, Object> result = service.calculate(List.of(purchase), List.of(), List.of(), today);

        assertEquals(0, result.get("noSpendStreak"));
    }

    private Expense expense(String title, double amount, String type, String category,
                            LocalDate date, Boolean discretionary) {
        Expense expense = new Expense(title, amount, category, type, null, date, null);
        expense.setDiscretionary(discretionary);
        return expense;
    }

    private Budget budget(String category, double amount) {
        Budget budget = new Budget();
        budget.setCategoryKey(category);
        budget.setMonthlyBudget(amount);
        return budget;
    }
}
