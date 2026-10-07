package com.anurag.cse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByOwnerIdAndType(Long ownerId, String type);
    List<Expense> findByOwnerIdAndCategory(Long ownerId, String category);
    List<Expense> findByOwnerId(Long ownerId);
    List<Expense> findByOwnerIdOrderByDateDesc(Long ownerId);
    java.util.Optional<Expense> findByIdAndOwnerId(Long id, Long ownerId);
    boolean existsByIdAndOwnerId(Long id, Long ownerId);
    void deleteByIdAndOwnerId(Long id, Long ownerId);
}
