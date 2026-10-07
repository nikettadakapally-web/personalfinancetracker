package com.anurag.cse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

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
        goal.setOwnerId(authService.requireUser(authorization).getId());
        if (goal.getCurrentAmount() == null) {
            goal.setCurrentAmount(0.0);
        }
        return goalRepository.save(goal);
    }

    @PutMapping("/{id}/add-funds")
    public ResponseEntity<SavingGoal> addFunds(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                @PathVariable Long id, @RequestParam Double amount) {
        Optional<SavingGoal> optionalGoal = goalRepository.findByIdAndOwnerId(id, authService.requireUser(authorization).getId());
        if (optionalGoal.isPresent()) {
            SavingGoal goal = optionalGoal.get();
            double current = (goal.getCurrentAmount() != null) ? goal.getCurrentAmount() : 0.0;
            goal.setCurrentAmount(current + amount);
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
}
