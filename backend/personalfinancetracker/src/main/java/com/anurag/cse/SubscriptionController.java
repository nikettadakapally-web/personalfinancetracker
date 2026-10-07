package com.anurag.cse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionRepository subscriptionRepository;
    private final AuthService authService;

    public SubscriptionController(SubscriptionRepository subscriptionRepository, AuthService authService) {
        this.subscriptionRepository = subscriptionRepository;
        this.authService = authService;
    }

    @GetMapping
    public List<Subscription> getAllSubscriptions(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return subscriptionRepository.findByOwnerId(authService.requireUser(authorization).getId());
    }

    @PostMapping
    public Subscription createSubscription(@RequestHeader(value = "Authorization", required = false) String authorization,
                                           @RequestBody Subscription subscription) {
        subscription.setOwnerId(authService.requireUser(authorization).getId());
        return subscriptionRepository.save(subscription);
    }

    @PutMapping("/{id}/active")
    public ResponseEntity<Subscription> updateActive(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id, @RequestParam boolean active) {
        Long ownerId = authService.requireUser(authorization).getId();
        Optional<Subscription> optional = subscriptionRepository.findByIdAndOwnerId(id, ownerId);
        if (optional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Subscription subscription = optional.get();
        subscription.setActive(active);
        return ResponseEntity.ok(subscriptionRepository.save(subscription));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubscription(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                    @PathVariable Long id) {
        Long ownerId = authService.requireUser(authorization).getId();
        if (!subscriptionRepository.existsByIdAndOwnerId(id, ownerId)) {
            return ResponseEntity.notFound().build();
        }
        subscriptionRepository.deleteByIdAndOwnerId(id, ownerId);
        return ResponseEntity.noContent().build();
    }
}
