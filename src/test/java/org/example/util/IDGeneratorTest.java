package org.example.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class IDGeneratorTest {

    @Test
    void testGeneratorUID() {
        String id = IDGenerator.generatorUID();
        assertNotNull(id);
        assertTrue(id.startsWith("USER_"));
    }

    @Test
    void testGenerateItemID() {
        String id = IDGenerator.generateItemID();
        assertNotNull(id);
        assertTrue(id.startsWith("ITEM_"));
    }

    @Test
    void testGenerateUUID() {
        String id = IDGenerator.generateUUID();
        assertNotNull(id);
        assertEquals(36, id.length());
    }

    @Test
    void testGenerateShortId() {
        String id = IDGenerator.generateShortId();
        assertNotNull(id);
        assertEquals(8, id.length());
        assertEquals(id, id.toUpperCase());
    }
}
