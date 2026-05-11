package org.example.service;


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
import java.util.Map;
import java.util.concurrent.*;

public class AuctionService {
    private final AuctionRepository AuctionRepositoryImpl;
    private final BidRepository BidRepositoryImpl;
    private final ItemRepository ItemRepositoryImpl;
    private final AutoBidRepository AutoBidRepositoryImpl;
    private final Map<Integer, BiddingCoordinator> coordinators = new ConcurrentHashMap<>();
    private final Map<Integer, ScheduledExecutorService> schedulers = new ConcurrentHashMap<>();


    public AuctionService(AuctionRepository AuctionRepositoryImpl,
                          BidRepository BidRepositoryImpl,
                          ItemRepository ItemRepositoryImpl,
                          AutoBidRepository AutoBidRepositoryImpl){
        this.ItemRepositoryImpl = ItemRepositoryImpl;
        this.AuctionRepositoryImpl = AuctionRepositoryImpl;
        this.BidRepositoryImpl = BidRepositoryImpl;
        this.AutoBidRepositoryImpl = AutoBidRepositoryImpl;
    }
    public void StartAuction(Auction auction){
        auction.start();
        BiddingCoordinator coordinator = new BiddingCoordinator(auction);
        coordinator.setOnBidPersisted(bid -> {
            AuctionRepositoryImpl.update(auction, Auction.Status.RUNNING.name());
            BidRepositoryImpl.save(bid, auction.getId());
        });
        AuctionRepositoryImpl.save(auction, Auction.Status.OPEN.name());
        coordinators.put(auction.getId(), coordinator);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        schedulers.put(auction.getId(), scheduler);


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
        AutoBidRepositoryImpl.save(autoBid, auction.getId());
    }

    public void FinishAuction(Auction auction){
        auction.finish();
        AuctionRepositoryImpl.update(auction, Auction.Status.FINISHED.name());
        AutoBidRepositoryImpl.deactivateByAuction(auction.getId());
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
        AuctionRepositoryImpl.updateStatus(auction, Auction.Status.CANCELED.name());
        AutoBidRepositoryImpl.deactivateByAuction(auction.getId());
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
        AuctionRepositoryImpl.updateStatus(auction, Auction.Status.PAID.name());
        // không dùng enum vì với đồ vật thì là sold chứ kphai paid
        ItemRepositoryImpl.updateStatus(auction.getItem(),"SOLD");
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

