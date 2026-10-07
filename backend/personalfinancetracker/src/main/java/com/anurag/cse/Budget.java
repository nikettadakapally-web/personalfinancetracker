package com.anurag.cse;

import jakarta.persistence.*;

@Entity
@Table(name = "budgets")
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ownerId;

    private String categoryKey;
    private String categoryName;
    private Double monthlyBudget;
    private String color;
    private String icon;

    public Budget() {}

    public Long getId() { 
        return id; 
    }
    public void setId(Long id) { 
        this.id = id; 
    }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public String getCategoryKey() { 
        return categoryKey; 
    }
    public void setCategoryKey(String categoryKey) { 
        this.categoryKey = categoryKey; 
    }

    public String getCategoryName() { 
        return categoryName; 
    }
    public void setCategoryName(String categoryName) { 
        this.categoryName = categoryName; 
    }

    public Double getMonthlyBudget() { 
        return monthlyBudget; 
    }
    public void setMonthlyBudget(Double monthlyBudget) { 
        this.monthlyBudget = monthlyBudget; 
    }

    public String getColor() { 
        return color; 
    }
    public void setColor(String color) { 
        this.color = color; 
    }

    public String getIcon() { 
        return icon; 
    }
    public void setIcon(String icon) { 
        this.icon = icon; 
    }
}
