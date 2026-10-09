package com.anurag.cse;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ownerId;

    private String title;
    private Double amount;
    private String category;
    private String type;
    private String paymentMode;
    private LocalDate date;
    private String notes;
    private Boolean discretionary;

    public Expense() {}

    public Expense(String title, Double amount, String category, String type, String paymentMode, LocalDate date, String notes) {
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.type = type;
        this.paymentMode = paymentMode;
        this.date = date;
        this.notes = notes;
    }

    public Long getId() { 
        return id; 
    }
    public void setId(Long id) { 
        this.id = id; 
    }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public String getTitle() { 
        return title; 
    }
    public void setTitle(String title) { 
        this.title = title; 
    }

    public Double getAmount() { 
        return amount; 
    }
    public void setAmount(Double amount) { 
        this.amount = amount; 
    }

    public String getCategory() { 
        return category; 
    }
    public void setCategory(String category) { 
        this.category = category; 
    }

    public String getType() { 
        return type; 
    }
    public void setType(String type) { 
        this.type = type; 
    }

    public String getPaymentMode() { 
        return paymentMode; 
    }
    public void setPaymentMode(String paymentMode) { 
        this.paymentMode = paymentMode; 
    }

    public LocalDate getDate() { 
        return date; 
    }
    public void setDate(LocalDate date) { 
        this.date = date; 
    }

    public String getNotes() { 
        return notes; 
    }
    public void setNotes(String notes) { 
        this.notes = notes; 
    }

    public Boolean getDiscretionary() {
        return discretionary;
    }
    public void setDiscretionary(Boolean discretionary) {
        this.discretionary = discretionary;
    }
}
