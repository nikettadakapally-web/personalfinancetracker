package com.anurag.cse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.server.ResponseStatusException;

class WriteControllerValidationTests {

    @Test
    void rejectsInvalidExpenseAmountsAndTypesBeforeSaving() {
        ExpenseRepository expenses = mock(ExpenseRepository.class);
        AuthService auth = mock(AuthService.class);
        ExpenseController controller = new ExpenseController(expenses, auth);

        Expense negativeAmount = validExpense();
        negativeAmount.setAmount(-1.0);
        assertBadRequest(() -> controller.createExpense(null, negativeAmount));

        Expense unsupportedType = validExpense();
        unsupportedType.setType("transfer");
        assertBadRequest(() -> controller.updateExpense(null, 1L, unsupportedType));

        verifyNoInteractions(expenses, auth);
    }

    @Test
    void rejectsInvalidGoalAmountsAndNegativeFundAdditions() {
        SavingGoalRepository goals = mock(SavingGoalRepository.class);
        AuthService auth = mock(AuthService.class);
        SavingGoalController controller = new SavingGoalController(goals, auth);

        SavingGoal invalidGoal = new SavingGoal("Emergency fund", 1000.0, -1.0, null, "savings");
        assertBadRequest(() -> controller.createGoal(null, invalidGoal));
        assertBadRequest(() -> controller.addFunds(null, 1L, Double.NaN));

        verifyNoInteractions(goals, auth);
    }

    @Test
    void rejectsNonPositiveBudgetsAndUnsupportedSubscriptionFrequencies() {
        BudgetRepository budgets = mock(BudgetRepository.class);
        AuthService auth = mock(AuthService.class);
        BudgetController budgetController = new BudgetController(budgets, auth);
        Budget invalidBudget = new Budget();
        invalidBudget.setCategoryKey("food");
        invalidBudget.setCategoryName("Food");
        invalidBudget.setMonthlyBudget(0.0);
        assertBadRequest(() -> budgetController.saveOrUpdateBudget(null, invalidBudget));

        SubscriptionRepository subscriptions = mock(SubscriptionRepository.class);
        SubscriptionController subscriptionController = new SubscriptionController(subscriptions, auth);
        Subscription invalidSubscription = new Subscription("Music", 10.0, "whenever",
                "entertainment", null, null, true);
        assertBadRequest(() -> subscriptionController.createSubscription(null, invalidSubscription));

        verifyNoInteractions(budgets, subscriptions, auth);
    }

    @Test
    void rejectsOverlongPaymentRecipientBeforeSendingOrSaving() {
        AuthService auth = mock(AuthService.class);
        AppUser user = mock(AppUser.class);
        when(auth.requireUser(null)).thenReturn(user);
        PaymentRequestRepository requests = mock(PaymentRequestRepository.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<org.springframework.mail.javamail.JavaMailSender> mailProvider = mock(ObjectProvider.class);
        PaymentRequestController controller = new PaymentRequestController(auth, requests, mailProvider);
        PaymentRequestController.PaymentRequestInput input = new PaymentRequestController.PaymentRequestInput(
                "R".repeat(101), "recipient@example.com", 10.0, null);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> controller.create(null, input));

        assertEquals(400, error.getStatusCode().value());
        verifyNoInteractions(requests, mailProvider);
    }

    @Test
    void sendsFriendlyPaymentRequestEmailWithCurrencyAndCustomNote() {
        AuthService auth = mock(AuthService.class);
        AppUser user = mock(AppUser.class);
        when(auth.requireUser("Bearer test-token")).thenReturn(user);
        when(user.getId()).thenReturn(42L);
        when(user.getName()).thenReturn("Finance Dashboard User");
        when(user.getEmail()).thenReturn("sender@example.com");

        PaymentRequestRepository requests = mock(PaymentRequestRepository.class);
        when(requests.save(any(PaymentRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        JavaMailSender mailSender = mock(JavaMailSender.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> mailProvider = mock(ObjectProvider.class);
        when(mailProvider.getIfAvailable()).thenReturn(mailSender);

        PaymentRequestController controller = new PaymentRequestController(auth, requests, mailProvider);
        ReflectionTestUtils.setField(controller, "senderEmail", "noreply@example.com");
        controller.create("Bearer test-token", new PaymentRequestController.PaymentRequestInput(
                "Friend", "friend@example.com", 250.0, "The coffee was lovely—thanks!"));

        org.mockito.ArgumentCaptor<SimpleMailMessage> message =
                org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(message.capture());
        assertTrue(message.getValue().getSubject().contains("friendly money nudge"));
        assertTrue(message.getValue().getText().contains("₹250.00"));
        assertTrue(message.getValue().getText().contains("The coffee was lovely—thanks!"));
        assertTrue(message.getValue().getText().contains("not a payment receipt"));
    }

    @Test
    void rejectsBcryptPasswordsThatExceedItsUtf8ByteLimit() {
        AuthService auth = mock(AuthService.class);
        AuthController controller = new AuthController(auth);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () ->
                controller.register(new AuthController.AuthRequest("Finance Dashboard User",
                        "person@example.com", "é".repeat(37))));

        assertEquals(400, error.getStatusCode().value());
        verifyNoInteractions(auth);
    }

    @Test
    void createRoutesDiscardClientSuppliedIdsAndSetTheAuthenticatedOwner() {
        AuthService auth = mock(AuthService.class);
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(42L);
        when(auth.requireUser("Bearer test-token")).thenReturn(user);

        Expense expense = validExpense();
        expense.setId(7L);
        new ExpenseController(mock(ExpenseRepository.class), auth)
                .createExpense("Bearer test-token", expense);
        assertNull(expense.getId());
        assertEquals(42L, expense.getOwnerId());

        SavingGoal goal = new SavingGoal("Emergency fund", 1000.0, 0.0, null, "savings");
        goal.setId(8L);
        new SavingGoalController(mock(SavingGoalRepository.class), auth)
                .createGoal("Bearer test-token", goal);
        assertNull(goal.getId());
        assertEquals(42L, goal.getOwnerId());

        Budget budget = new Budget();
        budget.setId(9L);
        budget.setCategoryKey("food");
        budget.setCategoryName("Food");
        budget.setMonthlyBudget(300.0);
        BudgetRepository budgets = mock(BudgetRepository.class);
        when(budgets.findByOwnerIdAndCategoryKey(42L, "food")).thenReturn(Optional.empty());
        new BudgetController(budgets, auth).saveOrUpdateBudget("Bearer test-token", budget);
        assertNull(budget.getId());
        assertEquals(42L, budget.getOwnerId());

        Subscription subscription = new Subscription("Music", 10.0, "monthly",
                "entertainment", null, null, true);
        subscription.setId(10L);
        new SubscriptionController(mock(SubscriptionRepository.class), auth)
                .createSubscription("Bearer test-token", subscription);
        assertNull(subscription.getId());
        assertEquals(42L, subscription.getOwnerId());
    }

    private Expense validExpense() {
        Expense expense = new Expense();
        expense.setTitle("Groceries");
        expense.setAmount(25.0);
        expense.setCategory("Food");
        expense.setType("expense");
        return expense;
    }

    private void assertBadRequest(Executable action) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, action::execute);
        assertEquals(400, error.getStatusCode().value());
    }

    @FunctionalInterface
    private interface Executable {
        void execute();
    }
}
