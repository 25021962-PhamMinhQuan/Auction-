package org.example.domain.user;

public class Seller extends User {
    public Seller(String id, String username, String password) {
        super(id, username, password, UserRole.SELLER.name());
    }
    public Seller(String username, String password) {
        super(username, password, UserRole.SELLER.name());
    }

    @Override
    public User cloneWithNewPassword(User user, String newPassword){
        return new Seller(user.getId(), user.getUsername(), newPassword);
    }
}

