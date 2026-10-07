package com.anurag.cse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
	java.util.List<Subscription> findByOwnerId(Long ownerId);
	java.util.Optional<Subscription> findByIdAndOwnerId(Long id, Long ownerId);
	boolean existsByIdAndOwnerId(Long id, Long ownerId);
	void deleteByIdAndOwnerId(Long id, Long ownerId);
}
