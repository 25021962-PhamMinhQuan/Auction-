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

    // processAuto() - KHÔNG CÓ AUTO BID


    @Test
    @DisplayName("processAuto() trả về null khi không có auto bid nào")
    void testProcessAutoNoAutoBids() {
        AutoBidManager.AutoBidResult result = manager.processAuto();
        assertNull(result);
    }

    // ============================================================
    // processAuto() - MỘT AUTO BID
    // ============================================================

    @Test
    @DisplayName("processAuto() với 1 auto bid hợp lệ -> đặt giá thành công")
    void testProcessAutoSingleBidder() {
        double increment = 600_000;
        double maxBid = 12_000_000;
        manager.addAutoBid(new AutoBid(bidder1, maxBid, increment));

        AutoBidManager.AutoBidResult result = manager.processAuto();
        assertNotNull(result);
        assertEquals(bidder1, result.getBidder());
        assertEquals(10_000_000 + increment, result.getAmount(), 0.001);
    }

    @Test
    @DisplayName("processAuto() trả về null nếu maxBid <= currentPrice")
    void testProcessAutoMaxBidBelowCurrentPrice() {
        double maxBid = 9_000_000;
        manager.addAutoBid(new AutoBid(bidder1, maxBid, 600_000));

        AutoBidManager.AutoBidResult result = manager.processAuto();
        assertNull(result);
    }

    @Test
    @DisplayName("processAuto() tự động dùng minIncrement khi increment đăng ký quá thấp")
    void testProcessAutoIncrementBelowMin_AutoRaised() {
        // minIncrement = 5% * 10_000_000 = 500_000
        // increment đăng ký = 100_000 < minIncrement → effectiveIncrement = 500_000
        // nextPrice = 10_500_000 < maxBid = 12_000_000 → hợp lệ
        double increment = 100_000;
        manager.addAutoBid(new AutoBid(bidder1, 12_000_000, increment));

        AutoBidManager.AutoBidResult result = manager.processAuto();
        assertNotNull(result, "Vẫn bid được, chỉ dùng minIncrement thay thế");
        assertEquals(bidder1, result.getBidder());
        // effectiveIncrement = max(100_000, 500_000) = 500_000
        assertEquals(10_000_000 + 500_000, result.getAmount(), 0.001);
    }

    // processAuto() - HAI AUTO BID

    @Test
    @DisplayName("processAuto() với 2 bidder: bidder maxBid cao hơn được chọn trước")
    void testProcessAutoTwoBidders_HigherMaxBidWins() {
        // processAuto() trả về 1 bước từ currentPrice=10M
        // bidder1 maxBid=15M > bidder2 maxBid=12M → bidder1 được chọn
        // bước 1: bidder1 @ 10M + 600K = 10.6M
        manager.addAutoBid(new AutoBid(bidder1, 15_000_000, 600_000));
        manager.addAutoBid(new AutoBid(bidder2, 12_000_000, 600_000));

        AutoBidManager.AutoBidResult result = manager.processAuto();
        assertNotNull(result);
        assertEquals(bidder1, result.getBidder());
        assertEquals(10_000_000 + 600_000, result.getAmount(), 0.001);
    }

    @Test
    @DisplayName("processAuto() ping-pong dừng khi bidder2 không thể outbid nữa")
    void testProcessAutoTwoBidders_ResultCappedAtWinnerMaxBid() {
        // currentPrice PHẢI được cập nhật thủ công sau mỗi bước
        // vì unit test không đi qua placeBidInternal()
        // bidder1 maxBid=12M, bidder2 maxBid=11.8M, increment=600K
        manager.addAutoBid(new AutoBid(bidder1, 12_000_000, 600_000));
        manager.addAutoBid(new AutoBid(bidder2, 11_800_000, 600_000));

        // bước 1: bidder1 @ 10.6M
        AutoBidManager.AutoBidResult r1 = manager.processAuto();
        assertNotNull(r1);
        assertEquals(bidder1, r1.getBidder());
        assertEquals(10_600_000, r1.getAmount(), 0.001);
        auction.getItem().setCurrentPrice(r1.getAmount()); // update thủ công

        // bước 2: bidder2 @ 11.2M
        AutoBidManager.AutoBidResult r2 = manager.processAuto();
        assertNotNull(r2);
        assertEquals(bidder2, r2.getBidder());
        assertEquals(11_200_000, r2.getAmount(), 0.001);
        auction.getItem().setCurrentPrice(r2.getAmount());

        // bước 3: bidder1 @ 11.8M
        AutoBidManager.AutoBidResult r3 = manager.processAuto();
        assertNotNull(r3);
        assertEquals(bidder1, r3.getBidder());
        assertEquals(11_800_000, r3.getAmount(), 0.001);
        auction.getItem().setCurrentPrice(r3.getAmount());

        // bước 4: bidder2 muốn bid 11.8M + 600K = 12.4M > maxBid 11.8M → null
        AutoBidManager.AutoBidResult r4 = manager.processAuto();
        assertNull(r4, "bidder2 không thể outbid nữa → dừng");
    }

    // PRIORITY QUEUE - THỨ TỰ ƯU TIÊN

    @Test
    @DisplayName("AutoBid có maxBid cao hơn được ưu tiên hơn")
    void testAutoBidPriority_HigherMaxBidFirst() {
        Bidder bidder3 = new Bidder("USER_C03", "charlie", "pass");
        manager.addAutoBid(new AutoBid(bidder2, 11_000_000, 600_000));
        manager.addAutoBid(new AutoBid(bidder1, 15_000_000, 600_000));
        manager.addAutoBid(new AutoBid(bidder3, 13_000_000, 600_000));

        AutoBidManager.AutoBidResult result = manager.processAuto();
        assertNotNull(result);
        assertEquals(bidder1, result.getBidder());
    }

    // AutoBidResult GETTERS


    @Test
    @DisplayName("AutoBidResult.getBidder() và getAmount() trả về đúng")
    void testAutoBidResultGetters() {
        manager.addAutoBid(new AutoBid(bidder1, 12_000_000, 600_000));

        AutoBidManager.AutoBidResult result = manager.processAuto();
        assertNotNull(result);
        assertNotNull(result.getBidder());
        assertTrue(result.getAmount() > 0);
    }

    // FIX: SINGLE BIDDER KHÔNG TỰ BID VỚI CHÍNH MÌNH


    @Test
    @DisplayName("processAuto() dừng ngay khi bidder duy nhất đã là người dẫn đầu")
    void testProcessAutoStopsWhenSingleLeader() {
        manager.addAutoBid(new AutoBid(bidder1, 20_000_000, 600_000));

        // Lần 1: bidder1 chưa bid lần nào → được phép bid
        AutoBidManager.AutoBidResult r1 = manager.processAuto();
        assertNotNull(r1);
        assertEquals(bidder1, r1.getBidder());

        // Lần 2: bidder1 vừa là lastAutoBidder, không có ai khác → dừng
        AutoBidManager.AutoBidResult r2 = manager.processAuto();
        assertNull(r2, "Bidder duy nhất không được tự outbid chính mình");
    }

    @Test
    @DisplayName("processAuto() sau resetLastBidder() cho phép bidder duy nhất bid lại")
    void testProcessAutoAfterReset_SingleBidderCanBidAgain() {
        manager.addAutoBid(new AutoBid(bidder1, 20_000_000, 600_000));

        AutoBidManager.AutoBidResult r1 = manager.processAuto();
        assertNotNull(r1);

        // Sau reset (ví dụ: có bid tay mới) → bidder1 được phép bid lại
        manager.resetLastBidder();
        AutoBidManager.AutoBidResult r2 = manager.processAuto();
        assertNotNull(r2, "Sau resetLastBidder(), bidder duy nhất được phép bid lại");
        assertEquals(bidder1, r2.getBidder());
    }

    // THREAD SAFETY - addAutoBid

    @Test
    @DisplayName("addAutoBid() với 2 bidder: processAuto() nhiều lần vẫn đúng")
    void testAddMultipleAutoBids() {
        manager.addAutoBid(new AutoBid(bidder1, 20_000_000, 600_000));
        manager.addAutoBid(new AutoBid(bidder2, 15_000_000, 600_000));

        // bidder1 (maxBid cao hơn) bid trước bidder2
        AutoBidManager.AutoBidResult r1 = manager.processAuto();
        assertNotNull(r1);
        assertEquals(bidder1, r1.getBidder());

        // Lần 2: ping sang bidder2
        AutoBidManager.AutoBidResult r2 = manager.processAuto();
        assertNotNull(r2, "Với 2 bidder, ping-pong vẫn tiếp tục");
        assertEquals(bidder2, r2.getBidder());
    }
}