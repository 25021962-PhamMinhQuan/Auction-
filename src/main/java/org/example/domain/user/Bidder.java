package org.example.domain.user;

public class Bidder extends User {
    public Bidder(String id, String username, String password) {
        super(id, username, password, UserRole.BIDDER.name());
    }
    public Bidder(String username, String password) {
        super(username, password, UserRole.BIDDER.name());
    }

    @Override
    public User cloneWithNewPassword(User user, String newPassword){
        return new Bidder(user.getId(), user.getUsername(), newPassword);
    }
}

