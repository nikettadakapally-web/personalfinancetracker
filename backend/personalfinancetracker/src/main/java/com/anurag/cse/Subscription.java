package com.anurag.cse;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @jakarta.persistence.Column(nullable = false)
    private Long ownerId;

    private String name;
    private Double amount;
    private String frequency;
    private String category;
    private LocalDate nextDueDate;
    private String usageRate;
    private Boolean active = true;

    public Subscription() {
    }

    public Subscription(String name, Double amount, String frequency, String category, LocalDate nextDueDate, String usageRate, Boolean active) {
        this.name = name;
        this.amount = amount;
        this.frequency = frequency;
        this.category = category;
        this.nextDueDate = nextDueDate;
        this.usageRate = usageRate;
        this.active = active;
    }

    public Long getId() { 
        return id; 
    }
    public void setId(Long id) { 
        this.id = id; 
    }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public String getName() { 
        return name; 
    }
    public void setName(String name) { 
        this.name = name; 
    }

    public Double getAmount() { 
        return amount; 
    }
    public void setAmount(Double amount) { 
        this.amount = amount; 
    }

    public String getFrequency() { 
        return frequency; 
    }
    public void setFrequency(String frequency) { 
        this.frequency = frequency; 
    }

    public String getCategory() { 
        return category; 
    }
    public void setCategory(String category) { 
        this.category = category; 
    }

    public LocalDate getNextDueDate() { 
        return nextDueDate; 
    }
    public void setNextDueDate(LocalDate nextDueDate) { 
        this.nextDueDate = nextDueDate; 
    }

    public String getUsageRate() { 
        return usageRate; 
    }
    public void setUsageRate(String usageRate) { 
        this.usageRate = usageRate; 
    }

    public Boolean getActive() { 
        return active; 
    }
    public void setActive(Boolean active) { 
        this.active = active; 
    }
}
