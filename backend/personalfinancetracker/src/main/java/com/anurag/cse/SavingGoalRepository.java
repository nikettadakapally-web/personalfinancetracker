package com.anurag.cse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SavingGoalRepository extends JpaRepository<SavingGoal, Long> {
	java.util.List<SavingGoal> findByOwnerId(Long ownerId);
	java.util.Optional<SavingGoal> findByIdAndOwnerId(Long id, Long ownerId);
	boolean existsByIdAndOwnerId(Long id, Long ownerId);
	void deleteByIdAndOwnerId(Long id, Long ownerId);
}
