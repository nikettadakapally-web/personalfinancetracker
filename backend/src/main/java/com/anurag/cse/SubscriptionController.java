package com.anurag.cse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
        validate(subscription);
        subscription.setFrequency(subscription.getFrequency().trim().toLowerCase(Locale.ROOT));
        subscription.setOwnerId(authService.requireUser(authorization).getId());
        subscription.setId(null);
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

    private void validate(Subscription subscription) {
        if (subscription == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A subscription is required.");
        }
        ApiInputValidation.requireText(subscription.getName(), "Name", 120);
        ApiInputValidation.requirePositiveAmount(subscription.getAmount(), "Amount");
        ApiInputValidation.requireText(subscription.getFrequency(), "Frequency", 20);
        if (!Set.of("daily", "day", "weekly", "week", "monthly", "month",
                "quarterly", "quarter", "yearly", "annual", "annually", "year")
                .contains(subscription.getFrequency().trim().toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Frequency must be daily, weekly, monthly, quarterly, or yearly.");
        }
        ApiInputValidation.requireOptionalText(subscription.getCategory(), "Category", 80);
        ApiInputValidation.requireOptionalText(subscription.getUsageRate(), "Usage rate", 80);
    }
}
