package org.example.domain.user;

public class Admin extends User {
    public Admin(String username, String password) { super(username, password, UserRole.ADMIN.name()); }
    public Admin(String id, String username, String password) {
        super(id, username, password, UserRole.ADMIN.name());
    }

    public Admin(String id, String username, String password,
                  String fullName, String email, String phone, String avatarUrl) {
        super(id, username, password, UserRole.ADMIN.name(),
                fullName, email, phone, avatarUrl);
    }

    @Override
    public User cloneWithNewPassword(User user, String newPassword){
        return new Admin(user.getId(), user.getUsername(), newPassword);
    }
}

