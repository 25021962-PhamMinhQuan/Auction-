package org.example.factory;

import org.example.domain.item.Item;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ItemFactory Tests")
public class ItemFactoryTest {

    private final LocalDateTime START = LocalDateTime.now().plusMinutes(5);
    private final LocalDateTime END = START.plusHours(1);

    // ============================================================
    // CREATE ITEM - HAPPY PATH
    // ============================================================

    @Test
    @DisplayName("Tạo item ELECTRONICS thành công")
    void testCreateElectronics() {
        Item item = ItemFactory.createItem("ELECTRONICS", "Laptop", "Gaming laptop", 5_000_000, START, END, "img.jpg");
        assertNotNull(item);
        assertEquals("ELECTRONICS", item.getType());
        assertEquals("Laptop", item.getName());
        assertEquals(5_000_000, item.getStartPrice());
    }

    @Test
    @DisplayName("Tạo item ART thành công")
    void testCreateArt() {
        Item item = ItemFactory.createItem("ART", "Mona Lisa", "Famous painting", 10_000_000, START, END, "art.jpg");
        assertNotNull(item);
        assertEquals("ART", item.getType());
    }

    @Test
    @DisplayName("Tạo item FASHIONS thành công")
    void testCreateFashions() {
        Item item = ItemFactory.createItem("FASHIONS", "Gucci Bag", "Authentic", 2_000_000, START, END, "bag.jpg");
        assertNotNull(item);
        assertEquals("FASHIONS", item.getType());
    }

    @Test
    @DisplayName("Tạo item VEHICLES thành công")
    void testCreateVehicles() {
        Item item = ItemFactory.createItem("VEHICLES", "BMW M3", "Sport car", 50_000_000, START, END, "car.jpg");
        assertNotNull(item);
        assertEquals("VEHICLES", item.getType());
    }

    @Test
    @DisplayName("Tạo item ESTATE thành công")
    void testCreateEstate() {
        Item item = ItemFactory.createItem("ESTATE", "Villa HN", "Luxury villa", 100_000_000, START, END, "villa.jpg");
        assertNotNull(item);
        assertEquals("ESTATE", item.getType());
    }

    @Test
    @DisplayName("Tạo item OTHERS thành công")
    void testCreateOthers() {
        Item item = ItemFactory.createItem("OTHERS", "Rare coin", "Collector item", 500_000, START, END, "coin.jpg");
        assertNotNull(item);
        assertEquals("OTHERS", item.getType());
    }

    @Test
    @DisplayName("Tên type chữ thường vẫn được nhận (case-insensitive)")
    void testCreateItemCaseInsensitive() {
        Item item = ItemFactory.createItem("electronics", "Phone", "Smartphone", 1_000_000, START, END, "phone.jpg");
        assertNotNull(item);
        assertEquals("ELECTRONICS", item.getType());
    }

    @Test
    @DisplayName("Item được tạo có ID không null và bắt đầu bằng ITEM_")
    void testItemHasValidId() {
        Item item = ItemFactory.createItem("ART", "Painting", "Oil on canvas", 1_000_000, START, END, "p.jpg");
        assertNotNull(item.getId());
        assertTrue(item.getId().startsWith("ITEM_"));
    }

    @Test
    @DisplayName("currentPrice khởi đầu bằng startPrice")
    void testCurrentPriceEqualsStartPrice() {
        Item item = ItemFactory.createItem("ELECTRONICS", "TV", "Smart TV", 3_000_000, START, END, "tv.jpg");
        assertEquals(item.getStartPrice(), item.getCurrentPrice());
    }

    // ============================================================
    // VALIDATE - EXCEPTION CASES
    // ============================================================

    @Test
    @DisplayName("Ném lỗi nếu tên item rỗng")
    void testCreateItemEmptyName() {
        assertThrows(IllegalArgumentException.class,
                () -> ItemFactory.createItem("ELECTRONICS", "", "desc", 1_000_000, START, END, "img.jpg"));
    }

    @Test
    @DisplayName("Ném lỗi nếu tên item là null")
    void testCreateItemNullName() {
        assertThrows(IllegalArgumentException.class,
                () -> ItemFactory.createItem("ELECTRONICS", null, "desc", 1_000_000, START, END, "img.jpg"));
    }

    @Test
    @DisplayName("Ném lỗi nếu giá <= 0")
    void testCreateItemZeroPrice() {
        assertThrows(IllegalArgumentException.class,
                () -> ItemFactory.createItem("ELECTRONICS", "Phone", "desc", 0, START, END, "img.jpg"));
    }

    @Test
    @DisplayName("Ném lỗi nếu giá âm")
    void testCreateItemNegativePrice() {
        assertThrows(IllegalArgumentException.class,
                () -> ItemFactory.createItem("ELECTRONICS", "Phone", "desc", -500_000, START, END, "img.jpg"));
    }

    @Test
    @DisplayName("Ném lỗi nếu end trước start")
    void testCreateItemEndBeforeStart() {
        LocalDateTime badEnd = START.minusHours(1);
        assertThrows(IllegalArgumentException.class,
                () -> ItemFactory.createItem("ELECTRONICS", "Phone", "desc", 1_000_000, START, badEnd, "img.jpg"));
    }

    @Test
    @DisplayName("Ném lỗi nếu type không tồn tại")
    void testCreateItemUnknownType() {
        assertThrows(IllegalArgumentException.class,
                () -> ItemFactory.createItem("UNKNOWN_TYPE", "Phone", "desc", 1_000_000, START, END, "img.jpg"));
    }

    // ============================================================
    // CREATE FROM DAO (with existing ID)
    // ============================================================

    @Test
    @DisplayName("createItemFromDAO() tạo item với ID đã có sẵn")
    void testCreateItemFromDAO() {
        String existingId = "ITEM_abc-123";
        Item item = ItemFactory.createItemFromDAO("ELECTRONICS", existingId, "Laptop", "desc", 2_000_000, START, END, "img.jpg");
        assertNotNull(item);
        assertEquals(existingId, item.getId());
        assertEquals("ELECTRONICS", item.getType());
    }

    @Test
    @DisplayName("createItemFromDAO() ném lỗi nếu type không hợp lệ")
    void testCreateItemFromDAOUnknownType() {
        assertThrows(IllegalArgumentException.class,
                () -> ItemFactory.createItemFromDAO("INVALID", "id-1", "Name", "desc", 1_000_000, START, END, "img.jpg"));
    }

    // ============================================================
    // AVAILABLE TYPES
    // ============================================================

    @Test
    @DisplayName("getAvailableTypes() trả về đủ 6 loại item")
    void testGetAvailableTypes() {
        var types = ItemFactory.getAvailableTypes();
        assertEquals(6, types.size());
        assertTrue(types.contains("ELECTRONICS"));
        assertTrue(types.contains("ART"));
        assertTrue(types.contains("FASHIONS"));
        assertTrue(types.contains("VEHICLES"));
        assertTrue(types.contains("ESTATE"));
        assertTrue(types.contains("OTHERS"));
    }
}