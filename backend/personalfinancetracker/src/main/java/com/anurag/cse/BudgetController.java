package com.anurag.cse;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
        validate(budget);
        Long ownerId = authService.requireUser(authorization).getId();
        Optional<Budget> existingOpt = budgetRepository.findByOwnerIdAndCategoryKey(ownerId, budget.getCategoryKey());
        if (existingOpt.isPresent()) {
            Budget existing = existingOpt.get();
            existing.setMonthlyBudget(budget.getMonthlyBudget());
            return budgetRepository.save(existing);
        } else {
            budget.setOwnerId(ownerId);
            budget.setId(null);
            return budgetRepository.save(budget);
        }
    }

    private void validate(Budget budget) {
        if (budget == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A budget is required.");
        }
        ApiInputValidation.requireText(budget.getCategoryKey(), "Category key", 80);
        ApiInputValidation.requireText(budget.getCategoryName(), "Category name", 80);
        ApiInputValidation.requirePositiveAmount(budget.getMonthlyBudget(), "Monthly budget");
        ApiInputValidation.requireOptionalText(budget.getColor(), "Color", 30);
        ApiInputValidation.requireOptionalText(budget.getIcon(), "Icon", 30);
    }
}
