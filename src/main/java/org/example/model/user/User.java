package org.example.model.user;

import org.example.model.Entity;
import org.example.util.IDGenerator;

public abstract class User extends Entity {

    public enum UserRole {
        ADMIN,
        SELLER,
        BIDDER
    }

    protected String username;
    protected String password;
    protected String role;

    public User(String username, String password, String role) {
        super(IDGenerator.generatorUID());
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public User(String id, String username, String password, String role) {
        super(id);
        this.username = username;
        this.password = password;
        this.role = role;
    }


    public abstract User cloneWithNewPassword(User user, String newPassword);


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

