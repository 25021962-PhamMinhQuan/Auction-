package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.example.domain.auction.Auction;
import org.example.coordinator.BiddingCoordinator;
import org.example.domain.user.Bidder;
import org.example.domain.user.User;
import org.example.observer.AuctionObserver;
import org.example.repository.AuctionRepository;
import org.example.repository.AutoBidRepository;
import org.example.repository.BidRepository;
import org.example.repository.ItemRepository;
import org.example.util.AutoBid;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class AuctionService {
    private final AuctionRepository auctionDAO;
    private final BidRepository bidDAO;
    private final ItemRepository itemDao;
    private final AutoBidRepository autoBidDao;
    private final Map<Integer, BiddingCoordinator> coordinators = new ConcurrentHashMap<>();
    private final Map<Integer, ScheduledExecutorService> schedulers = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(AuctionService.class);

    public AuctionService(AuctionRepository auctionDAO,
                          BidRepository bidDAO,
                          ItemRepository itemDao,
                          AutoBidRepository autoBidDAO) {
        this.itemDao = itemDao;
        this.auctionDAO = auctionDAO;
        this.bidDAO = bidDAO;
        this.autoBidDao = autoBidDAO;
    }
    public List<Auction> getAuctionsByStatus(String status) {
        return auctionDAO.findByStatus(status);
    }

    public void StartAuction(Auction auction){

        // Nếu item chưa có thời gian, tự động gán: bắt đầu = now, kết thúc = now + 30 phút
        if (auction.getItem().getStartTime() == null) {
            auction.getItem().setStartTime(LocalDateTime.now());
        }
        if (auction.getItem().getEndTime() == null) {
            auction.getItem().setEndTime(auction.getItem().getStartTime().plusMinutes(30));
        }

        BiddingCoordinator coordinator = new BiddingCoordinator(auction);
        coordinator.setOnBidPersisted(bid -> {
            auctionDAO.update(auction, Auction.Status.RUNNING.name());
            bidDAO.save(bid, auction.getId());
        });
        auctionDAO.save(auction, Auction.Status.RUNNING.name());
        coordinators.put(auction.getId(), coordinator);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        schedulers.put(auction.getId(), scheduler);


        scheduler.scheduleAtFixedRate(() -> {
            try {
                LocalDateTime now = LocalDateTime.now();

                // Chưa đến startTime → chưa bắt đầu, bỏ qua
                if (auction.getStatus() == Auction.Status.OPEN) {
                    if (now.isBefore(auction.getItem().getStartTime())) {
                        return; // chờ đến giờ
                    }
                    // Đến giờ rồi → chuyển sang RUNNING
                    auction.start();
                    auctionDAO.update(auction, Auction.Status.RUNNING.name());
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
        auction.finish();
        auctionDAO.update(auction, Auction.Status.FINISHED.name());
        autoBidDao.deactivateByAuction(auction.getId());
        coordinators.remove(auction.getId());
    }

    public void cancelAuction(Auction auction, User requester){

        if(requester.getRole().equals(User.UserRole.BIDDER.name())){
            throw new IllegalStateException("Bidder have no right to cancel this auction");
        }

        if(requester.getRole().equals(User.UserRole.SELLER.name()) && auction.getStatus() != Auction.Status.OPEN){
            throw  new IllegalStateException("Auction can't be cancelled after started ");
        }

        auction.cancel();
        auctionDAO.updateStatus(auction, Auction.Status.CANCELED.name());
        autoBidDao.deactivateByAuction(auction.getId());
        coordinators.remove(auction.getId());
    }

    public void markPaid(Auction auction, User requester){
        if(auction.getHighestBidder() == null){
            throw new IllegalStateException("No bidder won this auction");
        }

        boolean isWinner = auction.getWinner().getId().equals(requester.getId());
        boolean isAdmin = requester.getRole().equals(User.UserRole.ADMIN.name());
        boolean isSeller = requester.getRole().equals(User.UserRole.SELLER.name());

        if(!isAdmin && !isWinner){
            throw new IllegalStateException("Unable to paid");
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
        if (coord==null){
            return null;
        }
        return coord.getAuction();
    }

    public BiddingCoordinator getCoordinator(int auctionId) {
        return coordinators.get(auctionId);
    }
}

