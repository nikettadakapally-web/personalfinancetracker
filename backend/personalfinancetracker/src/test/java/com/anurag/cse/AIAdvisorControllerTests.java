package com.anurag.cse;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AIAdvisorControllerTests {

    @Test
    void answersCasualChatWithoutInventingFinancialData() {
        AIAdvisorController controller = controllerWithNoFinancialData();

        Map<String, Object> response = controller.askAIAdvisor(
                "Bearer test-token",
                new AIAdvisorController.ChatRequest("hey", List.of()));

        assertEquals("success", response.get("status"));
        assertTrue(!response.get("reply").toString().isBlank());
    }

    @Test
    void avoidsRepeatingThePreviousJokeInTheSameConversation() {
        AIAdvisorController controller = controllerWithNoFinancialData();
        String firstReply = reply(controller, "tell me a joke", List.of());

        String secondReply = reply(controller, "tell me a joke",
                List.of(new AIAdvisorController.ChatTurn("assistant", firstReply)));

        assertNotEquals(firstReply, secondReply);
    }

    @Test
    void variesGreetingWhenPreviousGreetingIsInChatHistory() {
        AIAdvisorController controller = controllerWithNoFinancialData();
        String firstReply = reply(controller, "hey", List.of());

        String secondReply = reply(controller, "hey",
                List.of(new AIAdvisorController.ChatTurn("assistant", firstReply)));

        assertNotEquals(firstReply, secondReply);
    }

    @Test
    void understandsAnotherAsARequestForAnotherJoke() {
        AIAdvisorController controller = controllerWithNoFinancialData();
        String firstReply = reply(controller, "tell me a joke", List.of());
        List<AIAdvisorController.ChatTurn> history = List.of(
                new AIAdvisorController.ChatTurn("user", "tell me a joke"),
                new AIAdvisorController.ChatTurn("assistant", firstReply));

        String secondReply = reply(controller, "another one", history);

        assertNotEquals(firstReply, secondReply);
        assertTrue(secondReply.length() > 20);
    }

    @Test
    void rejectsBlankChatMessages() {
        AIAdvisorController controller = controllerWithNoFinancialData();

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () ->
                controller.askAIAdvisor("Bearer test-token",
                        new AIAdvisorController.ChatRequest("  ", List.of())));

        assertEquals(400, error.getStatusCode().value());
    }

    @Test
    void answersTaxQuestionsDirectlyAndAsksForJurisdiction() {
        String response = reply(controllerWithNoFinancialData(), "how do i manage my taxes", List.of());

        assertTrue(response.contains("country or region"));
        assertTrue(response.contains("tax year"));
        assertTrue(response.contains("tax authority"));
        assertTrue(!response.contains("what got you thinking"));
    }

    @Test
    void keepsTaxContextForVagueFollowUps() {
        List<AIAdvisorController.ChatTurn> history = List.of(
                new AIAdvisorController.ChatTurn("user", "how do i manage my taxes"),
                new AIAdvisorController.ChatTurn("assistant", "Which country are you filing in?"));

        String response = reply(controllerWithNoFinancialData(), "i have trouble with it", history);

        assertTrue(response.contains("tax rules depend"));
        assertTrue(response.contains("filing, deductions"));
    }

    @Test
    void financialQuestionWithGreetingIsNotHandledAsOnlyAGreeting() {
        String response = reply(controllerWithNoFinancialData(), "hey, how do I manage my taxes?", List.of());

        assertTrue(response.contains("country or region"));
        assertTrue(!response.startsWith("Hey, good to see you"));
    }

    @Test
    void summaryIdentifiesTheDataAsRecordedTransactionsNotCurrentBankBalance() {
        Expense income = new Expense();
        income.setAmount(3000.0);
        income.setType("income");
        Expense expense = new Expense();
        expense.setAmount(1250.0);
        expense.setType("expense");

        String response = reply(controllerWithData(List.of(income, expense), List.of()),
                "give me my financial summary", List.of());

        assertTrue(response.contains("₹3,000.00"));
        assertTrue(response.contains("₹1,250.00"));
        assertTrue(response.contains("not necessarily a single month"));
        assertTrue(response.contains("not a live bank balance"));
    }

    @Test
    void answersSpendingQuestionWithRecordedTotalAndPeriodCaveat() {
        Expense expense = new Expense();
        expense.setAmount(1250.0);
        expense.setType("expense");

        String response = reply(controllerWithData(List.of(expense), List.of()),
                "how much did i spend", List.of());

        assertTrue(response.contains("₹1,250.00"));
        assertTrue(response.contains("not necessarily a single month"));
        assertTrue(response.contains("live bank data"));
    }

    @Test
    void currentMonthTotalExcludesUndatedEntries() {
        Expense datedExpense = new Expense();
        datedExpense.setAmount(100.0);
        datedExpense.setType("expense");
        datedExpense.setDate(LocalDate.now());
        Expense undatedExpense = new Expense();
        undatedExpense.setAmount(500.0);
        undatedExpense.setType("expense");

        String response = reply(controllerWithData(List.of(datedExpense, undatedExpense), List.of()),
                "how much did i spend this month", List.of());

        assertTrue(response.contains("₹100.00"));
        assertTrue(response.contains("this month to date"));
        assertTrue(response.contains("1 transaction without a date was excluded"));
    }

    @Test
    void subscriptionMonthlyEstimateConvertsYearlyAndWeeklyBilling() {
        Subscription yearly = new Subscription();
        yearly.setAmount(1200.0);
        yearly.setFrequency("yearly");
        Subscription weekly = new Subscription();
        weekly.setAmount(100.0);
        weekly.setFrequency("weekly");

        String response = reply(controllerWithData(List.of(), List.of(yearly, weekly)),
                "how much are my subscriptions", List.of());

        assertTrue(response.contains("₹533.33 per month"));
    }

    private AIAdvisorController controllerWithNoFinancialData() {
        return controllerWithData(List.of(), List.of());
    }

    private AIAdvisorController controllerWithData(List<Expense> expenseEntries,
                                                   List<Subscription> subscriptionEntries) {
        ExpenseRepository expenses = mock(ExpenseRepository.class);
        BudgetRepository budgets = mock(BudgetRepository.class);
        SubscriptionRepository subscriptions = mock(SubscriptionRepository.class);
        SavingGoalRepository goals = mock(SavingGoalRepository.class);
        AuthService auth = mock(AuthService.class);
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(42L);

        when(auth.requireUser(anyString())).thenReturn(user);
        when(expenses.findByOwnerId(42L)).thenReturn(expenseEntries);
        when(budgets.findByOwnerId(42L)).thenReturn(List.of());
        when(subscriptions.findByOwnerId(42L)).thenReturn(subscriptionEntries);
        when(goals.findByOwnerId(42L)).thenReturn(List.of());

        return new AIAdvisorController(expenses, budgets, subscriptions, goals, auth);
    }

    private String reply(AIAdvisorController controller, String message,
                         List<AIAdvisorController.ChatTurn> history) {
        return controller.askAIAdvisor("******",
                new AIAdvisorController.ChatRequest(message, history)).get("reply").toString();
    }
}
