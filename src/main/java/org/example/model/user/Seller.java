package org.example.model.user;

public class Seller extends User {
    public Seller(String id, String username, String password) {
        super(id, username, password, "SELLER");
    }
}

