package com.anurag.cse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/goals")
public class SavingGoalController {

    private final SavingGoalRepository goalRepository;
    private final AuthService authService;

    public SavingGoalController(SavingGoalRepository goalRepository, AuthService authService) {
        this.goalRepository = goalRepository;
        this.authService = authService;
    }

    @GetMapping
    public List<SavingGoal> getAllGoals(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return goalRepository.findByOwnerId(authService.requireUser(authorization).getId());
    }

    @PostMapping
    public SavingGoal createGoal(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @RequestBody SavingGoal goal) {
        validate(goal);
        goal.setOwnerId(authService.requireUser(authorization).getId());
        goal.setId(null);
        if (goal.getCurrentAmount() == null) {
            goal.setCurrentAmount(0.0);
        }
        return goalRepository.save(goal);
    }

    @PutMapping("/{id}/add-funds")
    public ResponseEntity<SavingGoal> addFunds(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                @PathVariable Long id, @RequestParam Double amount) {
        ApiInputValidation.requirePositiveAmount(amount, "Amount");
        Optional<SavingGoal> optionalGoal = goalRepository.findByIdAndOwnerId(id,
                authService.requireUser(authorization).getId());
        if (optionalGoal.isPresent()) {
            SavingGoal goal = optionalGoal.get();
            double current = (goal.getCurrentAmount() != null) ? goal.getCurrentAmount() : 0.0;
            double updatedAmount = current + amount;
            if (!Double.isFinite(updatedAmount)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The resulting goal amount is too large.");
            }
            goal.setCurrentAmount(updatedAmount);
            return ResponseEntity.ok(goalRepository.save(goal));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(@RequestHeader(value = "Authorization", required = false) String authorization,
                                            @PathVariable Long id) {
        Long ownerId = authService.requireUser(authorization).getId();
        if (!goalRepository.existsByIdAndOwnerId(id, ownerId)) {
            return ResponseEntity.notFound().build();
        }
        goalRepository.deleteByIdAndOwnerId(id, ownerId);
        return ResponseEntity.noContent().build();
    }

    private void validate(SavingGoal goal) {
        if (goal == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A savings goal is required.");
        }
        ApiInputValidation.requireText(goal.getTitle(), "Title", 120);
        ApiInputValidation.requirePositiveAmount(goal.getTargetAmount(), "Target amount");
        if (goal.getCurrentAmount() == null) {
            goal.setCurrentAmount(0.0);
        }
        ApiInputValidation.requireNonNegativeAmount(goal.getCurrentAmount(), "Current amount");
        ApiInputValidation.requireOptionalText(goal.getCategory(), "Category", 80);
    }
}
