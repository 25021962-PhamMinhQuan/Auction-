package org.example.model.user;

import org.example.model.Entity;

public abstract class User extends Entity {
    protected String username;
    protected String password;
    protected String role;

    public User(String id, String username, String password, String role) {
        super(id);
        this.username = username;
        this.password = password;
        this.role = role;
    }
    protected User cloneWithNewPassword(User user, String newPassword) {
        switch (user.getRole()) {
            case "ADMIN":
                return new Admin(user.getId(), user.getUsername(), newPassword);
            case "SELLER":
                return new Seller(user.getId(), user.getUsername(), newPassword);
            case "BIDDER":
                return new Bidder(user.getId(), user.getUsername(), newPassword);
            default:
                throw new IllegalArgumentException("Unknown role");
        }
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }
}

