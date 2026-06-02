package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.example.domain.auction.Auction;
import org.example.coordinator.BiddingCoordinator;
import org.example.domain.item.Item;
import org.example.domain.user.Bidder;
import org.example.domain.user.User;
import org.example.observer.AuctionObserver;
import org.example.repository.AuctionRepository;
import org.example.repository.AutoBidRepository;
import org.example.repository.BidRepository;
import org.example.repository.ItemRepository;
import org.example.repository.UserRepository;
import org.example.util.AutoBid;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class AuctionService {
    private final AuctionRepository auctionDAO;
    private final BidRepository bidDAO;
    private final ItemRepository itemDao;
    private final AutoBidRepository autoBidDao;
    private final UserRepository userDao;
    private final Map<Integer, BiddingCoordinator> coordinators = new ConcurrentHashMap<>();
    private final Map<Integer, ScheduledExecutorService> schedulers = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(AuctionService.class);
    private static final ZoneId AUCTION_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public AuctionService(AuctionRepository auctionDAO,
                          BidRepository bidDAO,
                          ItemRepository itemDao,
                          AutoBidRepository autoBidDAO,
                          UserRepository userDAO) {
        this.itemDao = itemDao;
        this.auctionDAO = auctionDAO;
        this.bidDAO = bidDAO;
        this.autoBidDao = autoBidDAO;
        this.userDao = userDAO;
        restoreSchedulers();
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(AUCTION_ZONE);
    }

    private void restoreSchedulers() {
        try {
            List<Auction> openAuctions    = auctionDAO.findByStatus("OPEN");
            List<Auction> runningAuctions = auctionDAO.findByStatus("RUNNING");
            for (Auction auction : openAuctions) {
                if (!schedulers.containsKey(auction.getId())) {
                    registerScheduler(auction);
                    logger.info("[RESTORE] Scheduler registered for OPEN auction id={}", auction.getId());
                }
            }
            for (Auction auction : runningAuctions) {
                if (!schedulers.containsKey(auction.getId())) {
                    BiddingCoordinator coordinator = new BiddingCoordinator(auction);
                    coordinator.setAuctionRepository(auctionDAO);
                    coordinator.setOnBidPersisted(bid -> {
                        auctionDAO.update(auction, Auction.Status.RUNNING.name());
                        bidDAO.save(bid, auction.getId());
                    });
                    coordinators.put(auction.getId(), coordinator);
                    registerScheduler(auction);
                    logger.info("[RESTORE] Scheduler registered for RUNNING auction id={}", auction.getId());
                }
            }
        } catch (Exception e) {
            logger.error("Failed to restore schedulers on startup", e);
        }
    }

    private void registerScheduler(Auction auction) {
        if (schedulers.containsKey(auction.getId())) {
            return;
        }
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        schedulers.put(auction.getId(), scheduler);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                LocalDateTime now = now();
                if (auction.getStatus() == Auction.Status.OPEN) {
                    if (now.isBefore(auction.getItem().getStartTime())) return;
                    auction.start();
                    auctionDAO.updateStatus(auction, Auction.Status.RUNNING.name());
                    // Tạo coordinator nếu chưa có (trường hợp restore từ DB sau restart)
                    coordinators.computeIfAbsent(auction.getId(), id -> {
                        BiddingCoordinator coord = new BiddingCoordinator(auction);
                        coord.setAuctionRepository(auctionDAO);
                        coord.setOnBidPersisted(bid -> {
                            auctionDAO.update(auction, Auction.Status.RUNNING.name());
                            bidDAO.save(bid, auction.getId());
                        });
                        return coord;
                    });
                    // Broadcast cho tất cả client biết auction đã mở → UI tự reload
                    org.example.server.AuctionServer.broadCast("NEW_AUCTION|"
                            + auction.getId() + "|" + auction.getItem().getName()
                            + "|" + auction.getCurrentPrice()
                            + "|" + auction.getItem().getEndTime()
                            + "|" + auction.getItem().getStartTime()
                            + "|" + auction.getStatus().name());
                    return;
                }
                if (auction.getStatus() != Auction.Status.RUNNING) {
                    cleanup(auction.getId());
                    return;
                }
                if (now.isAfter(auction.getItem().getEndTime())) {
                    FinishAuction(auction);
                }
            } catch (Exception e) {
                logger.error("Error in restored scheduler for auction {}", auction.getId(), e);
                cleanup(auction.getId());
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
    public List<Auction> getAuctionsByStatus(String status) {
        return auctionDAO.findByStatus(status);
    }

    public void StartAuction(Auction auction){

        // Nếu item chưa có thời gian, tự động gán: bắt đầu = now, kết thúc = now + 30 phút
        if (auction.getItem().getStartTime() == null) {
            auction.getItem().setStartTime(now());
        }
        if (auction.getItem().getEndTime() == null) {
            auction.getItem().setEndTime(auction.getItem().getStartTime().plusMinutes(30));
        }

        BiddingCoordinator coordinator = new BiddingCoordinator(auction);
        coordinator.setAuctionRepository(auctionDAO);
        coordinator.setOnBidPersisted(bid -> {
            auctionDAO.update(auction, Auction.Status.RUNNING.name());
            bidDAO.save(bid, auction.getId());
        });
        auctionDAO.save(auction, Auction.Status.OPEN.name());
        coordinators.put(auction.getId(), coordinator);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        schedulers.put(auction.getId(), scheduler);


        scheduler.scheduleAtFixedRate(() -> {
            try {
                LocalDateTime now = now();

                // Chưa đến startTime → chưa bắt đầu, bỏ qua
                if (auction.getStatus() == Auction.Status.OPEN) {
                    if (now.isBefore(auction.getItem().getStartTime())) {
                        return; // chờ đến giờ
                    }
                    // Đến giờ rồi → chuyển sang RUNNING
                    auction.start();
                    auctionDAO.updateStatus(auction, Auction.Status.RUNNING.name());
                    // Broadcast cho tất cả client biết auction đã mở → UI tự reload
                    org.example.server.AuctionServer.broadCast("NEW_AUCTION|"
                            + auction.getId() + "|" + auction.getItem().getName()
                            + "|" + auction.getCurrentPrice()
                            + "|" + auction.getItem().getEndTime()
                            + "|" + auction.getItem().getStartTime()
                            + "|" + auction.getStatus().name());
                    return;
                }

                if(auction.getStatus() != Auction.Status.RUNNING){
                    cleanup(auction.getId());
                    return;
                }

                LocalDateTime endTime = auction.getItem().getEndTime();
                if(now.isAfter(endTime)){
                    FinishAuction(auction);
                }
            } catch (Exception e) {
                logger.error("Error in auction scheduler for auction {}", auction.getId(), e);
                cleanup(auction.getId());
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void activateApprovedItem(Item item) {
        if (item == null) {
            throw new IllegalArgumentException("Item not found");
        }
        if (!"APPROVED".equalsIgnoreCase(item.getStatus())) {
            throw new IllegalStateException("Item must be approved before auction is activated");
        }

        Auction auction = auctionDAO.findByItemId(item.getId());
        if (auction == null) {
            StartAuction(new Auction(item));
            return;
        }

        if (auction.getStatus() == Auction.Status.OPEN || auction.getStatus() == Auction.Status.RUNNING) {
            registerScheduler(auction);
            if (auction.getStatus() == Auction.Status.RUNNING && !coordinators.containsKey(auction.getId())) {
                BiddingCoordinator coordinator = new BiddingCoordinator(auction);
                coordinator.setAuctionRepository(auctionDAO);
                coordinator.setOnBidPersisted(bid -> {
                    auctionDAO.update(auction, Auction.Status.RUNNING.name());
                    bidDAO.save(bid, auction.getId());
                });
                coordinators.put(auction.getId(), coordinator);
            }
        }
    }

    //searcj
    public List<Auction> searchByName(String keyword) {
        return auctionDAO.findByName(keyword);
    }

    public void cleanup(int auctionId) {
        ScheduledExecutorService scheduler = schedulers.remove(auctionId);
        if (scheduler != null) scheduler.shutdownNow();
    }

    public void placeBid(Auction auction, Bidder bidder,double amount){
        if (bidder == null) {
            throw new IllegalArgumentException("Bidder null");
        }

        BiddingCoordinator coordinator = coordinators.get(auction.getId());
        if (coordinator == null) {
            throw new IllegalStateException("Auction not started properly");
        }

        coordinator.placeBid(bidder, amount);

    }

    public void registerAutoBid(Auction auction, AutoBid autoBid) {
        BiddingCoordinator coordinator = coordinators.get(auction.getId());
        if (coordinator == null) {
            throw new IllegalStateException("Auction not started yet");
        }

        coordinator.registerAutoBid(autoBid);
        autoBidDao.save(autoBid, auction.getId());
    }

    public void FinishAuction(Auction auction){
        if (auction.getStatus() == Auction.Status.FINISHED || auction.getStatus() == Auction.Status.PAID) {
            return;
        }
        auction.finish();
        auctionDAO.update(auction, Auction.Status.FINISHED.name());
        autoBidDao.deactivateByAuction(auction.getId());
        coordinators.remove(auction.getId());
        String winner = auction.getHighestBidder() != null
                ? auction.getHighestBidder().getUsername()
                : "";
        org.example.server.AuctionServer.broadCast("FINISHED|"
                + auction.getId() + "|"
                + winner + "|"
                + auction.getCurrentPrice());
    }

    public void cancelAuction(Auction auction, User requester){

        if(requester.getRole().equals(User.UserRole.BIDDER.name())){
            throw new IllegalStateException("Bidder have no right to cancel this auction");
        }

        if (requester.getRole().equals(User.UserRole.SELLER.name())) {
            String sellerId = itemDao.findSellerIdByItemId(auction.getItem().getId());
            if (sellerId == null || !sellerId.equals(requester.getId())) {
                throw new IllegalStateException("You can only cancel your own auction");
            }
        }

        if(requester.getRole().equals(User.UserRole.SELLER.name()) && auction.getStatus() != Auction.Status.OPEN){
            throw  new IllegalStateException("Auction can't be cancelled after started ");
        }

        auction.cancel();
        auctionDAO.updateStatus(auction, Auction.Status.CANCELED.name());
        itemDao.updateStatus(auction.getItem(), "CANCELED");
        autoBidDao.deactivateByAuction(auction.getId());
        coordinators.remove(auction.getId());
        cleanup(auction.getId());
    }

    public synchronized void markPaid(Auction auction, User requester){
        if (auction.getStatus() == Auction.Status.RUNNING && now().isAfter(auction.getItem().getEndTime())) {
            FinishAuction(auction);
            Auction refreshed = auctionDAO.findById(auction.getId());
            if (refreshed != null) auction = refreshed;
        }
        if (auction.getStatus() != Auction.Status.FINISHED) {
            throw new IllegalStateException("Auction must be finished before closing winner");
        }
        if(auction.getHighestBidder() == null){
            throw new IllegalStateException("No bidder won this auction");
        }

        boolean isAdmin = requester.getRole().equals(User.UserRole.ADMIN.name());
        boolean isSeller = requester.getRole().equals(User.UserRole.SELLER.name());

        if(!isAdmin && !isSeller){
            throw new IllegalStateException("Only seller or admin can close winner");
        }

        User winner = userDao.findById(auction.getHighestBidder().getId());
        if (winner == null) {
            throw new IllegalStateException("Winner account not found");
        }
        double finalPrice = auction.getCurrentPrice();
        if (winner.getBalance() < finalPrice) {
            throw new IllegalStateException("Winner balance is not enough");
        }
        double newBalance = winner.getBalance() - finalPrice;
        userDao.updateBalance(winner.getId(), newBalance);
        if (requester.getId().equals(winner.getId())) {
            requester.setBalance(newBalance);
        }

        auction.markPaid();
        auctionDAO.updateStatus(auction, Auction.Status.PAID.name());
        // không dùng enum vì với đồ vật thì là sold chứ kphai paid
        itemDao.updateStatus(auction.getItem(),"SOLD");
        coordinators.remove(auction.getId());
    }
    public void addObserverToAuction(Auction auction, AuctionObserver observer) {
        BiddingCoordinator coordinator = coordinators.get(auction.getId());
        if (coordinator != null) {
            coordinator.getNotifier().addObserver(observer);
        }
    }
    public Auction findbyId(int id){
        BiddingCoordinator coord = coordinators.get(id);
        if (coord != null){
            return coord.getAuction();
        }
        return auctionDAO.findById(id);
    }

    public List<String[]> getBidHistory(int auctionId) {
        return bidDAO.getBidHistory(auctionId);
    }

    public List<Auction> searchByType(String type) {
        return auctionDAO.findByType(type);
    }

    public BiddingCoordinator getCoordinator(int auctionId) {
        return coordinators.get(auctionId);
    }
    public List<Auction> findAllAuctions() {
        return auctionDAO.findAll();
    }

    public void stopAuction(int auctionId, User requester) {
        Auction auction = findbyId(auctionId);
        if (auction == null) {
            throw new IllegalArgumentException("Auction not found");
        }
        cancelAuction(auction, requester);
        cleanup(auctionId);
    }

    public void deleteAuction(int auctionId) {
        cleanup(auctionId);
        coordinators.remove(auctionId);
        autoBidDao.deactivateByAuction(auctionId);
        auctionDAO.delete(auctionId);
    }

    public long countRunningAuctions() {
        return auctionDAO.findByStatus(Auction.Status.RUNNING.name()).size();
    }

    public long countOpenAuctions() {
        return auctionDAO.findByStatus(Auction.Status.OPEN.name()).size();
    }

    public Auction findByItemId(String itemId) {
        return auctionDAO.findByItemId(itemId);
    }

    public List<Auction> findAuctionsBySeller(String sellerId) {
        return auctionDAO.findBySellerId(sellerId);
    }

    public List<Auction> findWonAuctions(String bidderId) {
        return auctionDAO.findWonByBidderId(bidderId);
    }

    public void updateScheduledAuction(Auction auction, User requester) {
        if (!requester.getRole().equals(User.UserRole.SELLER.name())) {
            throw new IllegalStateException("Only seller can edit scheduled auction");
        }
        String sellerId = itemDao.findSellerIdByItemId(auction.getItem().getId());
        if (sellerId == null || !sellerId.equals(requester.getId())) {
            throw new IllegalStateException("You can only edit your own auction");
        }
        if (auction.getStatus() != Auction.Status.OPEN) {
            throw new IllegalStateException("Only scheduled auction can be edited");
        }
        itemDao.update(auction.getItem());
        auctionDAO.updateScheduleAndPrice(auction);
    }
}
