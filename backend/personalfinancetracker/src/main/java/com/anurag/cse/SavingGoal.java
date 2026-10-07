package com.anurag.cse;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "savings_goals")
public class SavingGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @jakarta.persistence.Column(nullable = false)
    private Long ownerId;

    private String title;
    private Double targetAmount;
    private Double currentAmount;
    private LocalDate deadline;
    private String category;

    public SavingGoal() {}

    public SavingGoal(String title, Double targetAmount, Double currentAmount, LocalDate deadline, String category) {
        this.title = title;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
        this.deadline = deadline;
        this.category = category;
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

    public Double getTargetAmount() { 
        return targetAmount; 
    }
    public void setTargetAmount(Double targetAmount) { 
        this.targetAmount = targetAmount; 
    }

    public Double getCurrentAmount() { 
        return currentAmount; 
    }
    public void setCurrentAmount(Double currentAmount) { 
        this.currentAmount = currentAmount; 
    }

    public LocalDate getDeadline() { 
        return deadline; 
    }
    public void setDeadline(LocalDate deadline) { 
        this.deadline = deadline; 
    }

    public String getCategory() { 
        return category; 
    }
    public void setCategory(String category) { 
        this.category = category; 
    }
}
