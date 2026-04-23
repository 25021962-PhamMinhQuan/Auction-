package org.example.model.user;

public class Bidder extends User {
    public Bidder(String id, String username, String password) {
        super(id, username, password, "BIDDER");
    }
    public Bidder(String username, String password) {
        super(username, password, "BIDDER");
    }
}

