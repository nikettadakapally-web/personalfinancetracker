package com.anurag.cse;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/ai")
public class AIAdvisorController {

    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SavingGoalRepository savingGoalRepository;
    private final AuthService authService;

    public AIAdvisorController(ExpenseRepository expenseRepository, BudgetRepository budgetRepository,
                               SubscriptionRepository subscriptionRepository, SavingGoalRepository savingGoalRepository,
                               AuthService authService) {
        this.expenseRepository = expenseRepository;
        this.budgetRepository = budgetRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.savingGoalRepository = savingGoalRepository;
        this.authService = authService;
    }

    @PostMapping("/chat")
    public Map<String, Object> askAIAdvisor(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody ChatRequest request) {
        Long ownerId = authService.requireUser(authorization).getId();
        String message = request == null || request.message() == null ? "" : request.message().trim();
        if (message.isEmpty() || message.length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a message of 1 to 2000 characters.");
        }

        List<Expense> expenses = expenseRepository.findByOwnerId(ownerId);
        List<Subscription> subscriptions = subscriptionRepository.findByOwnerId(ownerId);
        List<Budget> budgets = budgetRepository.findByOwnerId(ownerId);
        List<SavingGoal> goals = savingGoalRepository.findByOwnerId(ownerId);

        List<ChatTurn> history = request == null || request.history() == null ? List.of() : request.history();
        String query = resolveFollowUp(message, history);
        String normalized = query.toLowerCase(Locale.ROOT);
        double income = 0;
        double spending = 0;
        double foodSpending = 0;
        double travelSpending = 0;
        for (Expense expense : expenses) {
            double amount = expense.getAmount() == null ? 0 : expense.getAmount();
            String type = expense.getType() == null ? "expense" : expense.getType();
            String category = expense.getCategory() == null ? "" : expense.getCategory().toLowerCase(Locale.ROOT);
            if ("income".equalsIgnoreCase(type)) {
                income += amount;
            } else {
                spending += amount;
                if (category.contains("food") || category.contains("dining")) foodSpending += amount;
                if (category.contains("travel") || category.contains("commute")) travelSpending += amount;
            }
        }

        String reply = createReply(message, normalized, income, spending, foodSpending, travelSpending,
                expenses, subscriptions, budgets, goals, history);
        return Map.of("reply", reply, "status", "success");
    }

    private String createReply(String originalMessage, String message, double income, double spending,
                               double foodSpending, double travelSpending, List<Expense> expenses,
                               List<Subscription> subscriptions, List<Budget> budgets, List<SavingGoal> goals,
                               List<ChatTurn> history) {
        if (isGreeting(message)) {
            return choose(history,
                    "Hey, good to see you 🙂 What's on your mind?",
                    "Hey! How's your day going so far?",
                    "Hi there 🙂 Want to chat, swap a joke, or look at your finances?",
                    "Well hello! What are we chatting about today?",
                    "Hey hey 👋 How can I make this moment a little more useful or fun?");
        }
        if (message.contains("tax")) {
            if (message.contains("trouble") || message.contains("confus")
                    || message.contains("hard") || message.contains("difficult")
                    || message.contains("help")) {
                return "I can help you work through it, but tax rules depend on where you file. "
                        + "Which country or region and tax year are you dealing with, and is the difficult part "
                        + "filing, deductions, or calculating what you owe?";
            }
            return "Tax rules depend on your country or region and the tax year, so I won't guess at rates or "
                    + "deadlines. A safe starting point is to gather income statements, receipts for potentially "
                    + "eligible deductions, and your previous return; then verify filing requirements and dates "
                    + "with your tax authority. Which country or region and tax year do you mean?";
        }
        if (message.contains("invest") || message.contains("stock") || message.contains("shares")
                || message.contains("crypto")) {
            return "I can't promise returns or tell you what to buy. Before investing, check that essential bills "
                    + "and high-interest debt are manageable, keep an emergency reserve that fits your situation, "
                    + "and consider your time horizon, risk tolerance, fees, and diversification. What country, "
                    + "time horizon, and type of investment are you asking about?";
        }
        if (message.contains("debt") || message.contains("loan") || message.contains("credit card")) {
            return "Start by listing each balance, interest rate, minimum payment, and due date. Keep required "
                    + "payments current where possible; if you can pay extra, directing it to the highest-interest "
                    + "debt usually reduces interest fastest, while paying the smallest balance first can help "
                    + "motivation. Check fees and local protections before changing or consolidating a loan.";
        }
        if (message.contains("how are you") || message.contains("how's it going")
                || message.contains("how is your day") || message.contains("how's your day")) {
            return choose(history,
                    "Doing great in my own chatbot way 🙂 How are you doing?",
                    "Pretty good—always nice to have a chat! How's your day treating you?",
                    "I'm here and ready to chat 🙂 What's been the best part of your day?",
                    "Can't complain: no bills, no sleep schedule 😄 How are you?");
        }
        if (message.contains("thank")) {
            return choose(history, "Anytime 🙂 What else is on your mind?",
                    "You're very welcome! Want to keep chatting?",
                    "Glad I could help 🙂 Got another question for me?",
                    "Of course! I'm here if you want to talk more.");
        }
        if (message.contains("joke") || message.contains("make me laugh") || message.contains("something funny")) {
            return choose(history,
                    "Why did the scarecrow win an award? Because he was outstanding in his field 😄",
                    "I told my suitcase there would be no vacation this year. Now I'm dealing with emotional baggage. 🧳",
                    "Why don't skeletons fight each other? They don't have the guts. 💀",
                    "I asked my dog what's two minus two. He said nothing. 🐶",
                    "Why did the bicycle fall over? It was two-tired. 🚲",
                    "I used to hate facial hair, but then it grew on me. 😄",
                    "Why was the math book sad? It had too many problems. 📚",
                    "What do you call a bear with no teeth? A gummy bear. 🐻",
                    "Why can't you trust stairs? They're always up to something. 😄",
                    "I tried to catch fog yesterday. Mist. 🌫️",
                    "Why did the coffee file a police report? It got mugged. ☕",
                    "What did one wall say to the other? I'll meet you at the corner. 🧱",
                    "Why did the computer go to the doctor? It had a virus. 💻",
                    "What do you call fake spaghetti? An impasta. 🍝",
                    "Why did the tomato blush? It saw the salad dressing. 🍅",
                    "I wondered why the ball was getting bigger. Then it hit me. 😄");
        }
        if (message.contains("sad") || message.contains("upset") || message.contains("rough day")
                || message.contains("stressed") || message.contains("anxious")) {
            return choose(history,
                    "I'm sorry it's feeling heavy right now. Want to tell me what happened, or would a distraction help more?",
                    "That sounds like a lot. I'm here to listen—no need to solve everything in one go.",
                    "Oof, I'm sorry. Do you want to vent, talk it through, or switch to something lighter?",
                    "Thanks for telling me. What would feel helpful right now: a little space to talk, or a small distraction?");
        }
        if (message.contains("bored")) {
            return choose(history,
                    "Let's fix that. Want a joke, a quick would-you-rather, or a random fun fact?",
                    "Boredom check! Pick a lane: silly question, tiny trivia challenge, or joke?",
                    "I can help with that 😄 Want to play a quick guessing game or hear a joke?");
        }
        if (message.contains("favorite") || message.contains("favourite")) {
            return choose(history,
                    "I don't have personal favorites, but I love hearing yours. What's your current favorite?",
                    "No personal tastes on my end, but I'm curious about yours—what's your favorite lately?",
                    "I can't experience favorites like a person can, but I can definitely talk recommendations. What are you into?");
        }
        if (message.contains("who are you") || message.contains("what are you")) {
            return choose(history,
                    "I'm FinBot, your in-app chat companion. I can talk casually and help explain the finance data you've entered.",
                    "I'm FinBot 🙂 Think of me as a friendly chat companion with access to your recorded finance summaries.",
                    "I'm FinBot. I can keep you company, tell a joke, or help make sense of the finance details you add here.");
        }
        if (message.contains("goodbye") || message.matches(".*\\b(bye|see you|talk later)\\b.*")) {
            return choose(history, "Catch you later! 👋", "See you next time—take care 🙂",
                    "Talk soon! I'll be right here when you want another chat.",
                    "Bye for now! Hope the rest of your day goes well 👋");
        }

        boolean hasFinancialData = !expenses.isEmpty() || !subscriptions.isEmpty() || !budgets.isEmpty() || !goals.isEmpty();
        if (!hasFinancialData && asksForPersonalNumbers(message)) {
            return choose(history,
                    "I don't have financial entries to analyze yet, so I won't guess at your numbers. Add some whenever you're ready—or we can chat about something else.",
                    "I can't see any saved finance details yet, and I'd rather not make up numbers. We can still talk about anything else 🙂",
                    "No finance data has been added for me to check yet. When you add some, I'll use that; for now, what's else on your mind?");
        }

        if (message.contains("summary") || message.contains("overview") || message.contains("how am i doing")
                || message.contains("financial health") || message.contains("balance")) {
            if (expenses.isEmpty()) {
                return choose(history, "There aren't any transactions to summarize yet. Add some when you're ready and I'll put together an overview.",
                        "I need a few recorded transactions before I can give you a real summary—no made-up numbers from me 🙂",
                        "Nothing has been logged yet, so there's no honest summary to show. Want to add an entry or chat about something else?");
            }
            if (income <= 0) {
                return choose(history,
                        "Your recorded expenses total " + money(spending) + ". Log some income too and I can work out your net balance and savings rate.",
                        "I can see " + money(spending) + " in expenses so far, but no income yet. Add income when you have it and I'll complete the picture.",
                        "From the entries here, expenses are " + money(spending) + ". Once income is recorded, I can calculate the full summary.");
            }
            double net = income - spending;
            double savingsRate = net / income * 100;
            String figures = String.format(Locale.ROOT,
                    "income %s, expenses %s, net from those entries %s, and a savings rate of %.1f%%",
                    money(income), money(spending), money(net), savingsRate);
            return "Across all transactions currently recorded (not necessarily a single month), " + figures
                    + ". This is not a live bank balance; missing or unrecorded transactions are not included.";
        }
        String personalTotal = personalTotalReply(message, expenses);
        if (personalTotal != null) {
            return personalTotal;
        }
        if (message.contains("food") || message.contains("dining") || message.contains("restaurant")) {
            return expenses.stream().anyMatch(expense -> {
                String category = expense.getCategory() == null ? "" : expense.getCategory().toLowerCase(Locale.ROOT);
                return category.contains("food") || category.contains("dining");
            }) ? choose(history,
                    "You've logged " + money(foodSpending) + " in food and dining. Is there a particular part you'd like to change?",
                    "Your recorded food spend is " + money(foodSpending) + ". Want to look at ways to adjust it, or were you just curious?",
                    "I see " + money(foodSpending) + " in food and dining entries. We can dig into that together if you'd like.")
                    : choose(history,
                    "No food or dining transactions are recorded yet. Add a few and I can help spot patterns.",
                    "I don't see any food spend logged so far, so I can't give you a personal total yet.",
                    "Looks like the food category is still empty. Want a general food-budget idea while it is?");
        }
        if (message.contains("subscription") || message.contains("recurring")) {
            if (subscriptions.isEmpty()) {
                return choose(history, "No subscriptions have been added yet. Once you add them, we can review cost and usage.",
                        "I don't see any recurring subscriptions logged. Add them whenever you're ready and we can audit them together.",
                        "Your subscriptions list is empty for now—nothing to review yet 🙂");
            }
            List<Subscription> activeSubscriptions = subscriptions.stream()
                    .filter(subscription -> !Boolean.FALSE.equals(subscription.getActive())).toList();
            long activeCount = activeSubscriptions.size();
            long lowUsageCount = activeSubscriptions.stream()
                    .filter(subscription -> subscription.getUsageRate() != null
                            && subscription.getUsageRate().toLowerCase(Locale.ROOT).matches(".*(low|unused).*"))
                    .count();
            double monthlyCost = activeSubscriptions.stream()
                    .mapToDouble(subscription -> {
                        Double amount = monthlySubscriptionAmount(subscription);
                        return amount == null ? 0 : amount;
                    }).sum();
            long unknownCostCount = activeSubscriptions.stream()
                    .filter(subscription -> monthlySubscriptionAmount(subscription) == null).count();
            String counts = String.format(Locale.ROOT,
                    "%d active subscription%s; known costs average %s per month. %d %s marked low-usage",
                    activeCount, activeCount == 1 ? "" : "s", money(monthlyCost), lowUsageCount,
                    lowUsageCount == 1 ? "is" : "are");
            if (unknownCostCount > 0) {
                counts += String.format(Locale.ROOT,
                        ". I couldn't calculate a monthly amount for %d subscription%s because its amount or "
                                + "billing frequency is missing or unsupported",
                        unknownCostCount, unknownCostCount == 1 ? "" : "s");
            }
            return counts + ".";
        }
        if (message.contains("50/30/20") || message.contains("budget breakdown") || message.contains("budget rule")) {
            if (income <= 0) {
                return choose(history,
                        "The 50/30/20 guideline divides income among needs, wants, and savings. Add an income entry and I can compare your own figures with it.",
                        "In short, 50/30/20 is a budgeting guideline: needs, wants, and savings. I need recorded income before I can personalize the comparison.",
                        "I can explain the 50/30/20 rule now, but to calculate your targets I need an income amount logged first.");
            }
            double needs = expenses.stream().filter(expense -> !"income".equalsIgnoreCase(expense.getType()))
                    .filter(expense -> isNeed(expense.getCategory())).mapToDouble(expense -> expense.getAmount() == null ? 0 : expense.getAmount()).sum();
            double wants = Math.max(0, spending - needs);
            String figures = String.format(Locale.ROOT,
                    "with income %s, targets of %s for needs, %s for wants, and %s for savings; recorded spending is %s for needs and %s for other categories",
                    money(income), money(income * 0.5), money(income * 0.3), money(income * 0.2),
                    money(needs), money(wants));
            return choose(history, "Using the 50/30/20 guideline, " + figures + ". It's an estimate, not a strict rule.",
                    "A rough 50/30/20 comparison from your entries: " + figures + ".",
                    "Here's one way to view it: " + figures + ". Treat the split as a flexible guide.");
        }
        if (message.contains("goal") || message.contains("emergency fund") || message.contains("saving for")) {
            if (goals.isEmpty()) {
                return choose(history,
                        "No savings goals are on your list yet. Add one with the Add Goal button and I'll help you keep an eye on progress.",
                        "I don't see a goal recorded yet. What are you saving for? You can add it whenever you like.",
                        "Your goals list is empty for now. Once you add a target, we can talk through how it's going.");
            }
            String goalSummary = goals.stream().map(goal -> {
                double target = goal.getTargetAmount() == null ? 0 : goal.getTargetAmount();
                double current = goal.getCurrentAmount() == null ? 0 : goal.getCurrentAmount();
                return goal.getTitle() + ": " + money(current) + " of " + money(target);
            }).reduce((first, second) -> first + "; " + second).orElse("");
            return choose(history, "Here's the progress you've logged: " + goalSummary + ". Which one should we focus on?",
                    "Your current goals are " + goalSummary + ". Want to pick one to discuss?",
                    "I can see these goal figures: " + goalSummary + ". How are you feeling about them?");
        }
        if (message.contains("travel") || message.contains("commute") || message.contains("fuel")) {
            if (expenses.stream().noneMatch(expense -> {
                String category = expense.getCategory() == null ? "" : expense.getCategory().toLowerCase(Locale.ROOT);
                return category.contains("travel") || category.contains("commute");
            })) {
                return choose(history, "No travel or commute expenses are recorded yet. Add some and we can look for patterns.",
                        "I don't see any commute entries yet, so there's no personal travel total to report.",
                        "Your travel category is empty for now. We can revisit it once you've logged a few trips.");
            }
            return choose(history,
                    "You've logged " + money(travelSpending) + " in travel and commute spending. Want to compare options or just check the total?",
                    "Your recorded travel spend is " + money(travelSpending) + ". Anything specific you're planning?",
                    "I see " + money(travelSpending) + " in commute and travel entries. Is there a trip or cost you're thinking about?");
        }
        if (message.contains("save") || message.contains("saving") || message.contains("spend less")
                || message.contains("money tip")) {
            return choose(history,
                    "One low-pressure start: pick a flexible spending category and simply notice it for a week. No need to change everything at once. What feels easiest to look at?",
                    "A small, repeatable habit often beats a dramatic cut. You could review recurring charges or set aside a manageable amount after payday—what sounds realistic to you?",
                    "We can make this practical rather than restrictive. Is there one expense you'd like to understand better, or are you more interested in building a savings routine?");
        }
        if (message.contains("help") || message.contains("what can you do")) {
            return choose(history,
                    "We can chat about everyday stuff, swap jokes, or talk through the finance data you've added—spending, subscriptions, budgets, or goals. What are you in the mood for?",
                    "I'm up for casual chat, jokes, or a look at your recorded money details. No need to keep it strictly financial 🙂 What would you like?",
                    "Ask me for a joke, tell me about your day, or ask about the figures you've logged. Where should we start?");
        }
        if (!originalMessage.isBlank()) {
            return choose(history,
                    "I might need a little more context to follow—what part should we start with? We can keep it casual too.",
                    "Tell me a bit more; I want to make sure I understand what you mean 🙂",
                    "I'm listening. Can you say a little more about that?",
                    "I may be missing the context here. What happened next?",
                    "Interesting—what got you thinking about that?");
        }
        return choose(history, "What's on your mind?", "Where should we take the conversation?",
                "Want to tell me a bit more?", "I'm listening 🙂");
    }

    private boolean isNeed(String category) {
        if (category == null) return false;
        String normalized = category.toLowerCase(Locale.ROOT);
        return normalized.contains("bill") || normalized.contains("health")
                || normalized.contains("food") || normalized.contains("travel");
    }

    private boolean asksForPersonalNumbers(String message) {
        return message.contains("my ") || message.contains("how much") || message.contains("analy")
                || message.contains("spend") || message.contains("income") || message.contains("balance")
                || message.contains("budget") || message.contains("subscription") || message.contains("goal")
                || message.contains("summary") || message.contains("overview") || message.contains("financial health");
    }

    private boolean isGreeting(String message) {
        return message.matches("(?i)^(hi|hello|hey|good morning|good afternoon|good evening)[!.?, ]*$");
    }

    private String choose(List<ChatTurn> history, String... options) {
        List<String> allReplies = history == null ? List.of() : history.stream()
                .filter(turn -> turn != null && "assistant".equals(turn.role()) && turn.content() != null)
                .map(ChatTurn::content)
                .toList();
        List<String> recentReplies = allReplies.stream()
                .skip(Math.max(0, allReplies.size() - 8))
                .toList();
        List<String> unseenOptions = java.util.Arrays.stream(options)
                .filter(option -> !recentReplies.contains(option))
                .toList();
        List<String> availableOptions = unseenOptions.isEmpty() ? List.of(options) : unseenOptions;
        return availableOptions.get(ThreadLocalRandom.current().nextInt(availableOptions.size()));
    }

    private String resolveFollowUp(String message, List<ChatTurn> history) {
        if (history == null || history.isEmpty()) {
            return message;
        }
        String previousUserMessage = history.stream()
                .filter(turn -> turn != null && "user".equals(turn.role()) && turn.content() != null)
                .reduce((first, second) -> second)
                .map(ChatTurn::content)
                .orElse("");
        String normalized = message.toLowerCase(Locale.ROOT);
        boolean asksFollowUp = normalized.matches(
                "(?s).*(tell me more|what about it|why|and then|more details|another|one more|again).*");
        boolean vagueFinanceFollowUp = normalized.matches(
                "(?s).*(\\bit\\b|\\bthat\\b|\\bthis\\b|\\btrouble\\b|\\bconfus\\w*\\b|\\bhelp\\b|\\bhard\\b|\\bdifficult\\b).*")
                && previousUserMessage.toLowerCase(Locale.ROOT).matches(
                        "(?s).*(tax|money|finance|budget|income|spend|expense|invest|stock|loan|debt|subscription|saving).*");
        return asksFollowUp || vagueFinanceFollowUp ? previousUserMessage + " " + message : message;
    }

    private Double monthlySubscriptionAmount(Subscription subscription) {
        if (subscription.getAmount() == null || subscription.getFrequency() == null) {
            return null;
        }
        double amount = subscription.getAmount();
        return switch (subscription.getFrequency().trim().toLowerCase(Locale.ROOT)) {
            case "monthly", "month" -> amount;
            case "yearly", "annual", "annually", "year" -> amount / 12;
            case "quarterly", "quarter" -> amount / 3;
            case "weekly", "week" -> amount * 52 / 12;
            case "daily", "day" -> amount * 365 / 12;
            default -> null;
        };
    }

    private String personalTotalReply(String message, List<Expense> expenses) {
        boolean incomeQuery = message.contains("income") || message.contains("earnings") || message.contains("salary");
        boolean spendingQuery = message.contains("spend") || message.contains("spent")
                || message.contains("spending") || message.contains("expense") || message.contains("outgoing");
        boolean asksForAmount = message.contains("how much") || message.contains("total")
                || message.contains("amount") || message.contains("my income") || message.contains("my earnings")
                || message.contains("my spending") || message.contains("my expenses") || message.equals("income");
        boolean asksForSpecificCategory = spendingQuery
                && message.matches("(?s).*\\b(on|in|for)\\s+(?!this\\b|the\\s+period\\b)[a-z].*");
        if (!asksForAmount || (!incomeQuery && !spendingQuery) || (incomeQuery && spendingQuery)
                || asksForSpecificCategory
                || message.contains("food") || message.contains("dining") || message.contains("restaurant")
                || message.contains("travel") || message.contains("commute") || message.contains("fuel")
                || message.contains("subscription")) {
            return null;
        }

        LocalDate today = LocalDate.now();
        LocalDate periodStart = null;
        String periodLabel = "across all recorded entries (not necessarily a single month)";
        if (message.contains("today")) {
            periodStart = today;
            periodLabel = "today";
        } else if (message.contains("this week")) {
            periodStart = today.minusDays(today.getDayOfWeek().getValue() - 1L);
            periodLabel = "this week to date";
        } else if (message.contains("this month")) {
            periodStart = today.withDayOfMonth(1);
            periodLabel = "this month to date";
        } else if (message.contains("this year")) {
            periodStart = today.withDayOfYear(1);
            periodLabel = "this year to date";
        }

        final LocalDate start = periodStart;
        List<Expense> included = expenses.stream()
                .filter(expense -> start == null || (expense.getDate() != null
                        && !expense.getDate().isBefore(start) && !expense.getDate().isAfter(today)))
                .filter(expense -> incomeQuery
                        ? "income".equalsIgnoreCase(expense.getType())
                        : !"income".equalsIgnoreCase(expense.getType()))
                .toList();
        double total = included.stream()
                .mapToDouble(expense -> expense.getAmount() == null ? 0 : expense.getAmount())
                .sum();
        String label = incomeQuery ? "recorded income" : "recorded spending";
        String response = String.format(Locale.ROOT, "Your %s %s is %s.",
                label, periodLabel, money(total));
        if (start != null) {
            long undatedCount = expenses.stream().filter(expense -> expense.getDate() == null).count();
            if (undatedCount > 0) {
                response += String.format(Locale.ROOT,
                        " %d transaction%s without a date %s excluded from this period total.",
                        undatedCount, undatedCount == 1 ? "" : "s", undatedCount == 1 ? "was" : "were");
            }
        }
        return response + " It includes only entries saved in this app, not unrecorded transactions or live bank data.";
    }

    private String money(double amount) {
        NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("en", "IN"));
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);
        return "₹" + formatter.format(amount);
    }

    public record ChatRequest(String message, List<ChatTurn> history) {}
    public record ChatTurn(String role, String content) {}
}
