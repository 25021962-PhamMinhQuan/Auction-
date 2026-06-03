package org.example.manager;

import org.example.domain.auction.Auction;
import org.example.domain.item.Electronics;
import org.example.domain.user.Bidder;
import org.example.util.AutoBid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AutoBidManager Tests")
public class AutoBidManagerTest {

    private Auction auction;
    private AutoBidManager manager;
    private Bidder bidder1;
    private Bidder bidder2;

    @BeforeEach
    void setUp() {
        LocalDateTime start = LocalDateTime.now().minusMinutes(5);
        LocalDateTime end = LocalDateTime.now().plusHours(1);
        Electronics item = new Electronics("MacBook Pro", "Apple laptop", 10_000_000, start, end, "img.jpg");
        auction = new Auction(item);
        auction.start();
        manager = new AutoBidManager(auction);
        bidder1 = new Bidder("USER_A01", "alice", "pass");
        bidder2 = new Bidder("USER_B02", "bob", "pass");
    }


    @Test
    @DisplayName("processAuto() trả về null khi không có auto bid nào")
    void testProcessAutoNoAutoBids() {
        AutoBidManager.AutoBidResult result = manager.processAuto();
        assertNull(result);
    }



    @Test
    @DisplayName("processAuto() với 1 auto bid hợp lệ -> đặt giá thành công")
    void testProcessAutoSingleBidder() {
        // currentPrice = 10_000_000, minIncrement = 5% = 500_000
        double increment = 600_000; // > minIncrement
        double maxBid = 12_000_000;
        manager.addAutoBid(new AutoBid(bidder1, maxBid, increment));

        AutoBidManager.AutoBidResult result = manager.processAuto();
        assertNotNull(result);
        assertEquals(bidder1, result.getBidder());
        // priceAfterBid = min(maxBid, increment + currentPrice) = min(12M, 10.6M) = 10.6M
        assertEquals(10_000_000 + increment, result.getAmount(), 0.001);
    }

    @Test
    @DisplayName("processAuto() trả về null nếu maxBid <= currentPrice")
    void testProcessAutoMaxBidBelowCurrentPrice() {
        double maxBid = 9_000_000; // thấp hơn currentPrice
        manager.addAutoBid(new AutoBid(bidder1, maxBid, 600_000));

        AutoBidManager.AutoBidResult result = manager.processAuto();
        assertNull(result);
    }

    @Test
    @DisplayName("processAuto() trả về null nếu increment < minIncrement")
    void testProcessAutoIncrementBelowMin() {
        // minIncrement = 5% * 10_000_000 = 500_000
        double increment = 100_000; // nhỏ hơn minIncrement
        manager.addAutoBid(new AutoBid(bidder1, 15_000_000, increment));

        AutoBidManager.AutoBidResult result = manager.processAuto();
        assertNull(result);
    }

    @Test
    @DisplayName("processAuto() với 2 bidder: bidder maxBid cao hơn thắng")
    void testProcessAutoTwoBidders_HigherMaxBidWins() {
        // bidder1 maxBid = 15M (cao hơn) -> sẽ thắng
        // bidder2 maxBid = 12M
        manager.addAutoBid(new AutoBid(bidder1, 15_000_000, 600_000));
        manager.addAutoBid(new AutoBid(bidder2, 12_000_000, 600_000));

        AutoBidManager.AutoBidResult result = manager.processAuto();
        assertNotNull(result);
        assertEquals(bidder1, result.getBidder());
        // priceAfterBid = min(bidder1.maxBid, bidder2.maxBid + bidder1.increment)
        //               = min(15M, 12M + 600K) = min(15M, 12.6M) = 12.6M
        assertEquals(12_000_000 + 600_000, result.getAmount(), 0.001);
    }

    @Test
    @DisplayName("processAuto() với 2 bidder: giá đặt không vượt maxBid của người thắng")
    void testProcessAutoTwoBidders_ResultCappedAtWinnerMaxBid() {
        // bidder1 maxBid = 12M, bidder2 maxBid = 11.8M, increment = 600K
        // priceAfterBid = min(12M, 11.8M + 600K) = min(12M, 12.4M) = 12M -> capped
        manager.addAutoBid(new AutoBid(bidder1, 12_000_000, 600_000));
        manager.addAutoBid(new AutoBid(bidder2, 11_800_000, 600_000));

        AutoBidManager.AutoBidResult result = manager.processAuto();
        assertNotNull(result);
        assertEquals(bidder1, result.getBidder());
        assertEquals(12_000_000, result.getAmount(), 0.001);
    }



    @Test
    @DisplayName("AutoBid có maxBid cao hơn được ưu tiên hơn")
    void testAutoBidPriority_HigherMaxBidFirst() {
        Bidder bidder3 = new Bidder("USER_C03", "charlie", "pass");
        manager.addAutoBid(new AutoBid(bidder2, 11_000_000, 600_000));
        manager.addAutoBid(new AutoBid(bidder1, 15_000_000, 600_000)); // thêm sau nhưng maxBid cao hơn
        manager.addAutoBid(new AutoBid(bidder3, 13_000_000, 600_000));

        AutoBidManager.AutoBidResult result = manager.processAuto();
        assertNotNull(result);
        assertEquals(bidder1, result.getBidder()); // bidder1 maxBid=15M thắng
    }


    @Test
    @DisplayName("AutoBidResult.getBidder() và getAmount() trả về đúng")
    void testAutoBidResultGetters() {
        manager.addAutoBid(new AutoBid(bidder1, 12_000_000, 600_000));

        AutoBidManager.AutoBidResult result = manager.processAuto();
        assertNotNull(result);
        assertNotNull(result.getBidder());
        assertTrue(result.getAmount() > 0);
    }


    @Test
    @DisplayName("addAutoBid() và processAuto() hoạt động đúng sau nhiều lần thêm")
    void testAddMultipleAutoBids() {
        manager.addAutoBid(new AutoBid(bidder1, 20_000_000, 600_000));
        manager.addAutoBid(new AutoBid(bidder2, 15_000_000, 600_000));

        // Gọi processAuto() nhiều lần không được fail
        AutoBidManager.AutoBidResult r1 = manager.processAuto();
        assertNotNull(r1);
        AutoBidManager.AutoBidResult r2 = manager.processAuto();
        assertNotNull(r2); // vẫn còn autobid
    }
}