package org.example.model.auction;

import org.example.dao.AuctionDAO;
import org.example.dao.BidDAO;
import org.example.model.item.Item;
import org.example.model.user.Bidder;
import org.example.observer.AuctionObserver;
import org.example.service.AuctionService;
import org.example.service.UserService;
import org.example.util.AutoBid;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.PriorityQueue;
import java.util.function.Consumer;

public class auction {
        private int id;
        // tạo một mảng Status đặc biệt các giai đoạn của quá trình đấu giá
        public enum Status {OPEN, RUNNING, FINISHED, CANCELED}

        private AuctionService auctionService = new AuctionService();

        private Item item;

        // cnay để cập nhận trạng thái của giao dịch
        private Status status;

        // cái list này để lưu lịch sử giao dịch
        private List<BidTransaction> bids;

        private Bidder highestBidder;

        private List<AuctionObserver> observers = new ArrayList<>();

        // tạo ra một hàng đợi ưu tiên cho những người sử dụng lệnh đặt tự động
        private PriorityQueue<AutoBid> autoBids;

        private Consumer<BidTransaction> onBidPersisted;

        public void setOnBidPersisted(Consumer<BidTransaction> logic){
            this.onBidPersisted = logic;
        }

        public auction(Item item) {
            this.item = item;
            this.status = Status.OPEN;
            this.bids = new ArrayList<>();

            // khởi tạo ra mảng với yêu cầu (logic) là sắp xếp theo thứ tự nhỏ dần của giá trần và chx nạp object j đâu
            autoBids = new PriorityQueue<>(
                    (a, b) -> {
                        int logic = Double.compare(b.getMaxBid(), a.getMaxBid());
                        if(logic == 0){
                            return a.getTime().compareTo(b.getTime());
                        }
                        return logic;
                    }
            );
        }
        public double getMinIncrement(){
            return this.item.getCurrentPrice() * 0.05;
        }

        // nếu giao dịch đang mở thì đổi trạng thái nó thành hoạt động
        public void start() {
            if (status == Status.OPEN) {
                status = Status.RUNNING;
            }
        }

        // thêm người theo dõi vào list observers (list mẹ của cái bidder client để thông báo giao dịch ý)
        public void addObserver(AuctionObserver observer) {
            observers.add(observer);
        }


        private void notifyObservers(BidTransaction bid) {
            for (AuctionObserver o : observers) {
                // đoạn này this có nghĩa là nạp cái auction này vào bâyh thì chx hẳn là có tdung nhma khi thiết kế giao diện thì cnay sẽ cung cấp các thông tin như đang đấu giá sản phẩm nào bla bla
                o.update(this, bid, this.getMinIncrement());
            }
        }

        // nếu có ng dùng auto bid thì thêm họ vào danh sách hàng chờ ưu tiên
        public void addAutoBid(AutoBid autoBid) {
            autoBids.add(autoBid);
        }

        // synchronized là để đảm bảo chỉ có 1 người đặt giá 1 lúc ko xảy ra trg hợp 1 lucs 2 ng đặt
        public synchronized void placeBid(Bidder bidder, double amount) {

            // đoạn này là nếu có 1 người tăng giá thủ công thì set cái tigger auto = true để có thể chạy autobid
            placeBidInternal(bidder, amount, true);
        }

        private void placeBidInternal(Bidder bidder, double amount, boolean triggerAuto) {

            if (status != Status.RUNNING) {
                throw new IllegalStateException("Auction not running");
            }

            if (LocalDateTime.now().isAfter(item.getEndTime())) {
                finish();
                throw new IllegalStateException("Auction ended");
            }

            if (amount <= item.getCurrentPrice() || amount < item.getCurrentPrice() + this.getMinIncrement()) {
                throw new IllegalArgumentException("Bid too low");
            }

            item.setCurrentPrice(amount);
            highestBidder = bidder;

            BidTransaction bid = new BidTransaction(bidder, amount);
            // thêm giao dịch vào lịch sử
            bids.add(bid);

            if(onBidPersisted != null){
                onBidPersisted.accept(bid);
            }
            // anti-sniping
            if (item.getEndTime().minusSeconds(30).isBefore(LocalDateTime.now())) {
                item.extendTime(60);
            }

            notifyObservers(bid);

            // đây chính là phần để chạy autobid
            if (triggerAuto) {
                processAutoBids();
            }
        }

        private void processAutoBids() {
            Double minIncreament = this.getMinIncrement();
            if (autoBids.isEmpty()) return;

            AutoBid first=null,second = null;
            List<AutoBid> skipped = new ArrayList<>();

            while(!autoBids.isEmpty()){
                AutoBid candiate = autoBids.poll();
                if(candiate.getMaxBid() > item.getCurrentPrice() && candiate.getIncrement() >= minIncreament){
                    first = candiate;
                    break;
                }
                skipped.add(candiate);
            }
            if(first == null){
                autoBids.addAll(skipped);
                return;
            }

            while(!autoBids.isEmpty()){
                AutoBid candiate = autoBids.poll();
                if(candiate.getMaxBid() > item.getCurrentPrice() && candiate.getIncrement() >= minIncreament){
                    second = candiate;
                    break;
                }
                skipped.add(candiate);
            }
            double priceAfterBid;

            if(second == null){
                priceAfterBid = Math.min(first.getMaxBid(), first.getIncrement() + item.getCurrentPrice());
            }
            else{
                priceAfterBid = Math.min(first.getMaxBid(), second.getMaxBid() + first.getIncrement());
                autoBids.add(second);
            }

            placeBidInternal(first.getBidder(), priceAfterBid, false);
            autoBids.add(first);
            autoBids.addAll(skipped);

        }

        public void finish() {
            if (status == Status.RUNNING) {
                status = Status.FINISHED;
            }
        }

        public Bidder getWinner() {
            if (status == Status.FINISHED) {
                return highestBidder;
            } else {
                return null;
            }
        }

        public double getCurrentPrice() {
            return item.getCurrentPrice();
        }

        public List<BidTransaction> getBids() {
            return bids;
        }

        public void setId(int id) {
            this.id = id;
        }
        public int getId(){
            return this.id;
        }
        public Bidder getHighestBidder() {
            return highestBidder;
        }

        public Item getItem() {
            return item;
        }
}


