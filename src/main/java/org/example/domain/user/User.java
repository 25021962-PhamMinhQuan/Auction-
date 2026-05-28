package org.example.domain.user;

import org.example.domain.Entity;
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
    protected String fullName;
    protected String email;
    protected String phone;
    protected String avatarUrl;

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

    public User(String id, String username, String password, String role,
                String fullName, String email, String phone, String avatarUrl) {
        super(id);
        this.username  = username;
        this.password  = password;
        this.role      = role;
        this.fullName  = fullName;
        this.email     = email;
        this.phone     = phone;
        this.avatarUrl = avatarUrl;
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

    public String getFullName()  { return fullName; }
    public String getEmail()     { return email; }
    public String getPhone()     { return phone; }
    public String getAvatarUrl() { return avatarUrl; }

    public void setFullName(String fullName)   { this.fullName = fullName; }
    public void setEmail(String email)         { this.email = email; }
    public void setPhone(String phone)         { this.phone = phone; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}

