package com.anurag.cse;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetRepository budgetRepository;
    private final AuthService authService;

    public BudgetController(BudgetRepository budgetRepository, AuthService authService) {
        this.budgetRepository = budgetRepository;
        this.authService = authService;
    }

    @GetMapping
    public List<Budget> getAllBudgets(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return budgetRepository.findByOwnerId(authService.requireUser(authorization).getId());
    }

    @PostMapping
    public Budget saveOrUpdateBudget(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @RequestBody Budget budget) {
        Long ownerId = authService.requireUser(authorization).getId();
        Optional<Budget> existingOpt = budgetRepository.findByOwnerIdAndCategoryKey(ownerId, budget.getCategoryKey());
        if (existingOpt.isPresent()) {
            Budget existing = existingOpt.get();
            existing.setMonthlyBudget(budget.getMonthlyBudget());
            return budgetRepository.save(existing);
        } else {
            budget.setOwnerId(ownerId);
            return budgetRepository.save(budget);
        }
    }
}
