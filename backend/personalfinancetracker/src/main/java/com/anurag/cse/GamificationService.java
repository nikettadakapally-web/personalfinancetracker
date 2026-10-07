package com.anurag.cse;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class GamificationService {
    private static final double SAVINGS_WEIGHT = 0.40;
    private static final double BUDGET_WEIGHT = 0.35;
    private static final double DEBT_WEIGHT = 0.25;

    public Map<String, Object> calculate(List<Expense> expenses, List<Budget> budgets,
                                         List<SavingGoal> goals, LocalDate today) {
        LocalDate periodStart = today.minusDays(29);
        YearMonth currentMonth = YearMonth.from(today);
        double income = 0;
        double spending = 0;
        double debtPayments = 0;
        double monthlyBudget = 0;
        double budgetedSpending = 0;
        Map<String, Double> budgetByCategory = new HashMap<>();
        Set<LocalDate> discretionarySpendDays = new HashSet<>();
        LocalDate earliestTransaction = null;

        for (Budget budget : budgets) {
            if (budget.getCategoryKey() == null || budget.getMonthlyBudget() == null
                    || budget.getMonthlyBudget() <= 0) {
                continue;
            }
            budgetByCategory.put(budget.getCategoryKey().toLowerCase(Locale.ROOT), budget.getMonthlyBudget());
            monthlyBudget += budget.getMonthlyBudget();
        }

        for (Expense expense : expenses) {
            LocalDate date = expense.getDate();
            if (date == null || date.isAfter(today)) {
                continue;
            }
            if (earliestTransaction == null || date.isBefore(earliestTransaction)) {
                earliestTransaction = date;
            }

            double amount = expense.getAmount() == null ? 0 : Math.max(0, expense.getAmount());
            String type = expense.getType() == null ? "expense" : expense.getType();
            String category = expense.getCategory() == null ? "" : expense.getCategory().toLowerCase(Locale.ROOT);
            if ("income".equalsIgnoreCase(type)) {
                if (!date.isBefore(periodStart)) {
                    income += amount;
                }
                continue;
            }

            if (isDiscretionary(expense, category)) {
                discretionarySpendDays.add(date);
            }
            if (!date.isBefore(periodStart)) {
                spending += amount;
                if (isDebtCategory(category)) {
                    debtPayments += amount;
                }
            }
        }

        for (Expense expense : expenses) {
            if (expense.getDate() == null || !YearMonth.from(expense.getDate()).equals(currentMonth)
                    || expense.getDate().isAfter(today) || "income".equalsIgnoreCase(expense.getType())) {
                continue;
            }
            String category = expense.getCategory() == null ? "" : expense.getCategory().toLowerCase(Locale.ROOT);
            if (budgetByCategory.containsKey(category)) {
                budgetedSpending += Math.max(0, expense.getAmount() == null ? 0 : expense.getAmount());
            }
        }

        Double savingsRate = income > 0 ? ((income - spending) / income) * 100 : null;
        Double budgetAdherence = monthlyBudget > 0
                ? (budgetedSpending <= monthlyBudget ? 100 : (monthlyBudget / budgetedSpending) * 100)
                : null;
        Double debtToIncome = income > 0 ? (debtPayments / income) * 100 : null;

        double savingsScore = savingsRate == null ? 50 : clamp(savingsRate / 20 * 100);
        double budgetScore = budgetAdherence == null ? 50 : budgetAdherence;
        double debtScore = debtToIncome == null ? 50 : clamp((50 - debtToIncome) / 30 * 100);
        int score = (int) Math.round(savingsScore * SAVINGS_WEIGHT
                + budgetScore * BUDGET_WEIGHT + debtScore * DEBT_WEIGHT);

        Map<String, Object> result = new HashMap<>();
        result.put("score", score);
        result.put("savingsRate", round(savingsRate));
        result.put("budgetAdherence", round(budgetAdherence));
        result.put("debtToIncome", round(debtToIncome));
        result.put("savingsScore", Math.round(savingsScore));
        result.put("budgetScore", Math.round(budgetScore));
        result.put("debtScore", Math.round(debtScore));
        result.put("noSpendStreak", calculateNoSpendStreak(today, earliestTransaction, discretionarySpendDays));
        result.put("monthlySavings", Math.max(0, income - spending));
        result.put("goals", calculateGoalMilestones(goals, Math.max(0, income - spending), today));
        return result;
    }

    private List<Map<String, Object>> calculateGoalMilestones(List<SavingGoal> goals, double monthlySavings,
                                                               LocalDate today) {
        List<SavingGoal> activeGoals = goals.stream()
                .filter(goal -> value(goal.getTargetAmount()) > value(goal.getCurrentAmount()))
                .toList();
        double monthlyContribution = activeGoals.isEmpty() ? 0 : monthlySavings / activeGoals.size();
        List<Map<String, Object>> milestones = new ArrayList<>();
        for (SavingGoal goal : goals) {
            double target = value(goal.getTargetAmount());
            double current = value(goal.getCurrentAmount());
            double remaining = Math.max(0, target - current);
            Map<String, Object> milestone = new HashMap<>();
            milestone.put("id", goal.getId());
            milestone.put("title", goal.getTitle());
            milestone.put("progress", target > 0 ? Math.min(100, Math.round(current / target * 100)) : 0);
            milestone.put("monthlyContribution", monthlyContribution);
            milestone.put("estimatedCompletionDate", remaining == 0 ? today.toString()
                    : monthlyContribution > 0
                            ? YearMonth.from(today).plusMonths((long) Math.ceil(remaining / monthlyContribution))
                                    .atDay(1).toString()
                            : null);
            milestones.add(milestone);
        }
        return milestones;
    }

    private int calculateNoSpendStreak(LocalDate today, LocalDate earliestTransaction,
                                       Set<LocalDate> discretionarySpendDays) {
        if (earliestTransaction == null) {
            return 0;
        }
        LocalDate latestSpend = discretionarySpendDays.stream()
                .filter(date -> !date.isAfter(today))
                .max(LocalDate::compareTo)
                .orElse(null);
        long days = latestSpend == null
                ? ChronoUnit.DAYS.between(earliestTransaction, today) + 1
                : ChronoUnit.DAYS.between(latestSpend, today);
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, days));
    }

    private boolean isDiscretionary(Expense expense, String category) {
        if (expense.getDiscretionary() != null) {
            return expense.getDiscretionary();
        }
        return "shopping".equals(category) || "entertainment".equals(category);
    }

    private boolean isDebtCategory(String category) {
        return category.contains("debt") || category.contains("loan") || category.contains("credit");
    }

    private double value(Double amount) {
        return amount == null ? 0 : Math.max(0, amount);
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(100, value));
    }

    private Double round(Double value) {
        return value == null ? null : Math.round(value * 10) / 10.0;
    }
}
