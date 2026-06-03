package org.example.domain;

import org.example.domain.auction.Auction;
import org.example.domain.auction.BidTransaction;
import org.example.domain.item.Electronics;
import org.example.domain.user.Bidder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Auction Domain Tests")
public class AuctionTest {

    private Electronics item;
    private Auction auction;
    private Bidder bidder1;
    private Bidder bidder2;

    @BeforeEach
    void setUp() {
        LocalDateTime start = LocalDateTime.now().minusMinutes(5);
        LocalDateTime end = LocalDateTime.now().plusHours(1);
        item = new Electronics("iPhone 15", "Brand new", 1_000_000, start, end, "img.jpg");
        auction = new Auction(item);
        auction.start();
        bidder1 = new Bidder("USER_001", "alice", "pass");
        bidder2 = new Bidder("USER_002", "bob", "pass");
    }

    @Test
    @DisplayName("Auction khởi tạo phải có trạng thái OPEN")
    void testInitialStatusIsOpen() {
        Auction newAuction = new Auction(item);
        assertEquals(Auction.Status.OPEN, newAuction.getStatus());
    }

    @Test
    @DisplayName("start() chuyển OPEN -> RUNNING")
    void testStartChangesStatusToRunning() {
        Auction newAuction = new Auction(item);
        newAuction.start();
        assertEquals(Auction.Status.RUNNING, newAuction.getStatus());
    }

    @Test
    @DisplayName("start() không làm gì nếu đã RUNNING")
    void testStartOnRunningAuctionHasNoEffect() {
        auction.start(); // gọi lần 2
        assertEquals(Auction.Status.RUNNING, auction.getStatus());
    }

    @Test
    @DisplayName("finish() chuyển RUNNING -> FINISHED")
    void testFinishChangesStatusToFinished() {
        auction.finish();
        assertEquals(Auction.Status.FINISHED, auction.getStatus());
    }

    @Test
    @DisplayName("finish() không làm gì nếu không phải RUNNING")
    void testFinishOnOpenAuctionHasNoEffect() {
        Auction newAuction = new Auction(item);
        newAuction.finish();
        assertEquals(Auction.Status.OPEN, newAuction.getStatus());
    }

    @Test
    @DisplayName("cancel() từ OPEN chuyển sang CANCELED")
    void testCancelFromOpen() {
        Auction newAuction = new Auction(item);
        newAuction.cancel();
        assertEquals(Auction.Status.CANCELED, newAuction.getStatus());
    }

    @Test
    @DisplayName("cancel() từ RUNNING chuyển sang CANCELED")
    void testCancelFromRunning() {
        auction.cancel();
        assertEquals(Auction.Status.CANCELED, auction.getStatus());
    }

    @Test
    @DisplayName("markPaid() từ FINISHED chuyển sang PAID")
    void testMarkPaid() {
        auction.finish();
        auction.markPaid();
        assertEquals(Auction.Status.PAID, auction.getStatus());
    }

    @Test
    @DisplayName("markPaid() không làm gì nếu không phải FINISHED")
    void testMarkPaidOnRunningHasNoEffect() {
        auction.markPaid();
        assertEquals(Auction.Status.RUNNING, auction.getStatus());
    }


    @Test
    @DisplayName("validateBid() ném lỗi nếu bidder là null")
    void testValidateBidNullBidder() {
        assertThrows(IllegalArgumentException.class,
                () -> auction.validateBid(null, 1_200_000, BidTransaction.BidType.MANUAL));
    }

    @Test
    @DisplayName("validateBid() ném lỗi nếu auction đã CANCELED")
    void testValidateBidOnCanceledAuction() {
        auction.cancel();
        assertThrows(IllegalStateException.class,
                () -> auction.validateBid(bidder1, 1_200_000, BidTransaction.BidType.MANUAL));
    }

    @Test
    @DisplayName("validateBid() ném lỗi nếu auction chưa RUNNING (trạng thái OPEN)")
    void testValidateBidOnOpenAuction() {
        Auction newAuction = new Auction(item);
        assertThrows(IllegalStateException.class,
                () -> newAuction.validateBid(bidder1, 1_200_000, BidTransaction.BidType.MANUAL));
    }

    @Test
    @DisplayName("validateBid() ném lỗi nếu auction đã PAID")
    void testValidateBidOnPaidAuction() {
        auction.finish();
        auction.markPaid();
        assertThrows(IllegalStateException.class,
                () -> auction.validateBid(bidder1, 1_200_000, BidTransaction.BidType.MANUAL));
    }

    @Test
    @DisplayName("validateBid() ném lỗi nếu giá đặt quá thấp (thấp hơn currentPrice)")
    void testValidateBidAmountTooLow() {
        assertThrows(IllegalArgumentException.class,
                () -> auction.validateBid(bidder1, 500_000, BidTransaction.BidType.MANUAL));
    }

    @Test
    @DisplayName("validateBid() ném lỗi nếu người đang giữ giá cao nhất đặt thủ công lại")
    void testValidateBidSameBidderManual() {
        double minBid = item.getCurrentPrice() + auction.getMinIncrement() + 1;
        auction.recordBid(bidder1, minBid, BidTransaction.BidType.MANUAL);
        double nextBid = auction.getCurrentPrice() + auction.getMinIncrement() + 1;
        assertThrows(IllegalArgumentException.class,
                () -> auction.validateBid(bidder1, nextBid, BidTransaction.BidType.MANUAL));
    }

    @Test
    @DisplayName("validateBid() cho phép bidder2 đặt giá hợp lệ")
    void testValidateBidValidAmount() {
        double validBid = item.getCurrentPrice() + auction.getMinIncrement() + 1;
        assertDoesNotThrow(() -> auction.validateBid(bidder1, validBid, BidTransaction.BidType.MANUAL));
    }

    @Test
    @DisplayName("recordBid() cập nhật giá hiện tại và highest bidder")
    void testRecordBidUpdatesState() {
        double bidAmount = item.getCurrentPrice() + auction.getMinIncrement() + 1;
        auction.recordBid(bidder1, bidAmount, BidTransaction.BidType.MANUAL);

        assertEquals(bidAmount, auction.getCurrentPrice());
        assertEquals(bidder1, auction.getHighestBidder());
    }

    @Test
    @DisplayName("recordBid() thêm vào danh sách bid history")
    void testRecordBidAddsToBidHistory() {
        double bidAmount = item.getCurrentPrice() + auction.getMinIncrement() + 1;
        auction.recordBid(bidder1, bidAmount, BidTransaction.BidType.MANUAL);

        assertEquals(1, auction.getBids().size());
        assertEquals(bidAmount, auction.getBids().get(0).getAmount());
    }

    @Test
    @DisplayName("nhiều lần recordBid() tích lũy đúng lịch sử và cập nhật highest bidder")
    void testMultipleBidsHistory() {
        double bid1 = item.getCurrentPrice() + auction.getMinIncrement() + 1;
        auction.recordBid(bidder1, bid1, BidTransaction.BidType.MANUAL);

        double bid2 = auction.getCurrentPrice() + auction.getMinIncrement() + 1;
        auction.recordBid(bidder2, bid2, BidTransaction.BidType.MANUAL);

        assertEquals(2, auction.getBids().size());
        assertEquals(bidder2, auction.getHighestBidder());
    }

    @Test
    @DisplayName("recordBid() trả về BidTransaction với thông tin đúng")
    void testRecordBidReturnsBidTransaction() {
        double bidAmount = item.getCurrentPrice() + auction.getMinIncrement() + 1;
        BidTransaction tx = auction.recordBid(bidder1, bidAmount, BidTransaction.BidType.MANUAL);

        assertNotNull(tx);
        assertEquals(bidder1, tx.getBidder());
        assertEquals(bidAmount, tx.getAmount());
        assertEquals(BidTransaction.BidType.MANUAL, tx.getType());
        assertNotNull(tx.getTime());
    }

    @Test
    @DisplayName("getWinner() trả về null khi chưa FINISHED")
    void testGetWinnerNotFinished() {
        assertNull(auction.getWinner());
    }

    @Test
    @DisplayName("getWinner() trả về highest bidder sau khi FINISHED")
    void testGetWinnerAfterFinish() {
        double bidAmount = item.getCurrentPrice() + auction.getMinIncrement() + 1;
        auction.recordBid(bidder1, bidAmount, BidTransaction.BidType.MANUAL);
        auction.finish();

        assertEquals(bidder1, auction.getWinner());
    }

    @Test
    @DisplayName("getWinner() trả về null khi FINISHED nhưng không có bid nào")
    void testGetWinnerNoBids() {
        auction.finish();
        assertNull(auction.getWinner());
    }

    @Test
    @DisplayName("getMinIncrement() = 5% giá hiện tại")
    void testGetMinIncrement() {
        double expected = item.getCurrentPrice() * 0.05;
        assertEquals(expected, auction.getMinIncrement(), 0.001);
    }


    @Test
    @DisplayName("AntiSniping() gia hạn thời gian khi trong 30s cuối")
    void testAntiSnipingExtendsTime() {
        item.setEndTime(LocalDateTime.now().plusSeconds(10));
        LocalDateTime before = item.getEndTime();
        auction.AntiSniping();
        assertTrue(item.getEndTime().isAfter(before),
                "Thời gian kết thúc phải được gia hạn khi trong 30s cuối");
    }

    @Test
    @DisplayName("AntiSniping() không gia hạn khi còn nhiều thời gian")
    void testAntiSnipingNoExtensionWhenTimeRemaining() {
        item.setEndTime(LocalDateTime.now().plusHours(2));
        LocalDateTime before = item.getEndTime();
        auction.AntiSniping();
        assertEquals(before, item.getEndTime());
    }


    @Test
    @DisplayName("setId() và getId() hoạt động đúng")
    void testSetAndGetId() {
        auction.setId(42);
        assertEquals(42, auction.getId());
    }

    @Test
    @DisplayName("getItem() trả về item đúng")
    void testGetItem() {
        assertEquals(item, auction.getItem());
    }

    @Test
    @DisplayName("getBids() trả về unmodifiable list")
    void testGetBidsUnmodifiable() {
        assertThrows(UnsupportedOperationException.class,
                () -> auction.getBids().add(new BidTransaction(bidder1, 100, BidTransaction.BidType.MANUAL)));
    }
}