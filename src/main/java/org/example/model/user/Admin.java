package org.example.model.user;

public class Admin extends User {
    public Admin(String username, String password) { super(username, password, UserRole.ADMIN.name()); }
    public Admin(String id, String username, String password) {
        super(id, username, password, UserRole.ADMIN.name());
    }

    @Override
    public User cloneWithNewPassword(User user, String newPassword){
        return new Admin(user.getId(), user.getUsername(), newPassword);
    }
}

