package org.example.service;


import org.example.model.auction.Auction;
import org.example.model.auction.BiddingCoordinator;
import org.example.model.user.Bidder;
import org.example.model.user.User;
import org.example.observer.AuctionObserver;
import org.example.repository.AuctionRepository;
import org.example.repository.AutoBidRepository;
import org.example.repository.BidRepository;
import org.example.repository.ItemRepository;
import org.example.util.AutoBid;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.*;

public class AuctionService {
    private final AuctionRepository auctionDAO;
    private final BidRepository bidDAO;
    private final ItemRepository itemDao;
    private final AutoBidRepository autoBidDao;
    private final Map<Integer, BiddingCoordinator> coordinators = new ConcurrentHashMap<>();

    public AuctionService(AuctionRepository auctionDAO,
                          BidRepository bidDAO,
                          ItemRepository itemDao,
                          AutoBidRepository autoBidDAO){
        this.itemDao = itemDao;
        this.auctionDAO = auctionDAO;
        this.bidDAO = bidDAO;
        this.autoBidDao = autoBidDAO;
    }
    public void StartAuction(Auction auction){
        auction.start();
        BiddingCoordinator coordinator = new BiddingCoordinator(auction);
        coordinator.setOnBidPersisted(bid -> {
            auctionDAO.update(auction, Auction.Status.RUNNING.name());
            bidDAO.save(bid, auction.getId());
        });
        auctionDAO.save(auction, Auction.Status.OPEN.name());
        coordinators.put(auction.getId(), coordinator);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            if(auction.getStatus() != Auction.Status.RUNNING){
                scheduler.shutdown();
                return;
            }

            LocalDateTime endTime = auction.getItem().getEndTime();

            if(LocalDateTime.now().isAfter(endTime)){
                FinishAuction(auction);
                scheduler.shutdown();
            }
                }, 0, 1,TimeUnit.SECONDS);
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

