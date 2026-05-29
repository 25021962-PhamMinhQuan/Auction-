package org.example.domain.user;

import java.time.LocalDateTime;

public class DepositRequest {

    public enum Status { PENDING, APPROVED, REJECTED }

    private int id;
    private String userId;
    private String username;
    private double amount;
    private String note;
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    public DepositRequest(int id, String userId, String username,
                          double amount, String note, Status status,
                          LocalDateTime createdAt, LocalDateTime resolvedAt) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.amount = amount;
        this.note = note;
        this.status = status;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
    }

    // Constructor for new request (no id yet)
    public DepositRequest(String userId, String username, double amount, String note) {
        this.userId = userId;
        this.username = username;
        this.amount = amount;
        this.note = note;
        this.status = Status.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public int getId()                   { return id; }
    public String getUserId()            { return userId; }
    public String getUsername()          { return username; }
    public double getAmount()            { return amount; }
    public String getNote()              { return note; }
    public Status getStatus()            { return status; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }

    public void setStatus(Status status)               { this.status = status; }
    public void setResolvedAt(LocalDateTime resolvedAt){ this.resolvedAt = resolvedAt; }
}
