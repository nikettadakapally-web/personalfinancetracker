package com.anurag.cse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseRepository expenseRepository;
    private final AuthService authService;

    public ExpenseController(ExpenseRepository expenseRepository, AuthService authService) {
        this.expenseRepository = expenseRepository;
        this.authService = authService;
    }

    @GetMapping
    public List<Expense> getAllExpenses(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return expenseRepository.findByOwnerIdOrderByDateDesc(authService.requireUser(authorization).getId());
    }

    @PostMapping
    public Expense createExpense(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @RequestBody Expense expense) {
        expense.setOwnerId(authService.requireUser(authorization).getId());
        if (expense.getDate() == null) expense.setDate(java.time.LocalDate.now());
        return expenseRepository.save(expense);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Expense> updateExpense(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                  @PathVariable Long id, @RequestBody Expense updated) {
        Long ownerId = authService.requireUser(authorization).getId();
        Optional<Expense> optionalExpense = expenseRepository.findByIdAndOwnerId(id, ownerId);
        if (optionalExpense.isPresent()) {
            Expense expense = optionalExpense.get();
            expense.setTitle(updated.getTitle());
            expense.setAmount(updated.getAmount());
            expense.setCategory(updated.getCategory());
            expense.setType(updated.getType());
            expense.setPaymentMode(updated.getPaymentMode());
            expense.setDate(updated.getDate());
            expense.setNotes(updated.getNotes());
            expense.setDiscretionary(updated.getDiscretionary());
            return ResponseEntity.ok(expenseRepository.save(expense));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@RequestHeader(value = "Authorization", required = false) String authorization,
                                               @PathVariable Long id) {
        Long ownerId = authService.requireUser(authorization).getId();
        if (!expenseRepository.existsByIdAndOwnerId(id, ownerId)) {
            return ResponseEntity.notFound().build();
        }
        expenseRepository.deleteByIdAndOwnerId(id, ownerId);
        return ResponseEntity.noContent().build();
    }
}
