package com.anurag.cse;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "payment_requests")
public class PaymentRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long requesterId;

    @Column(nullable = false)
    private String recipientName;

    @Column(nullable = false)
    private String recipientEmail;

    @Column(nullable = false)
    private double amount;

    @Column(length = 500)
    private String note;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected PaymentRequest() {}

    public PaymentRequest(Long requesterId, String recipientName, String recipientEmail, double amount, String note) {
        this.requesterId = requesterId;
        this.recipientName = recipientName;
        this.recipientEmail = recipientEmail;
        this.amount = amount;
        this.note = note;
    }

    public Long getId() { return id; }
    public Long getRequesterId() { return requesterId; }
    public String getRecipientName() { return recipientName; }
    public String getRecipientEmail() { return recipientEmail; }
    public double getAmount() { return amount; }
    public String getNote() { return note; }
    public Instant getCreatedAt() { return createdAt; }
}