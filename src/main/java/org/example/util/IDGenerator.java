package org.example.util;

import java.util.UUID;

public class IDGenerator {
    public static String generatorUID(){
        return "USER_" + UUID.randomUUID().toString();
    }

    public static String generateItemID(){
        return "ITEM_" + UUID.randomUUID().toString();
    }

    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    public static String generateShortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

}
