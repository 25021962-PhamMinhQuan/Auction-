package server;

import org.example.model.auction.Auction;
import org.example.model.user.Bidder;
import org.example.model.user.User;
import org.example.service.AuctionService;
import org.example.service.ServiceFactory;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private User currentUser;

    private AuctionService auctionService = ServiceFactory.getInstance().getAuctionService();
    public ClientHandler(Socket socket){
        this.socket=socket;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(),true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        String line;
        try {
            while ((line = in.readLine()) != null) {
                System.out.println("System handle: " + line);
                handleCommand(line);
            }
        } catch (IOException e) {
            System.out.println("Client dissconected: "+socket.getInetAddress());
        }finally {
            AuctionServer.connectClient.remove(this);
            closeConnection();
        }
    }
    private void handleCommand(String rawline) {
        String[] parts = rawline.split("\\|");
        String command = parts[0];

        switch (command){
            case "BID":{
                int auctionId = Integer.parseInt(parts[1]);
                double ammount = Double.parseDouble(parts[2]);
                Auction auction = auctionService.findbyId(auctionId);
                if(auction==null){
                    sendMesage("Auction not found");
                }
                try {
                    auctionService.placeBid(auction,(Bidder) currentUser,ammount);
                    String updateMessage = "UPDATE|" + auctionId + "|"
                            + auction.getCurrentPrice() + "|"
                            + currentUser.getUsername();
                    AuctionServer.broadCast(updateMessage);
                } catch (Exception e) {
                    sendMesage("Lỗi: "+e.getMessage());
                }
            }
        }

    }
    public void sendMesage(String msg){
        System.out.println(msg);
    }
    private void closeConnection(){
        try {
            if(in!=null){
                in.close();
            }
            if(out!=null){
                out.close();
            }if(socket!=null){
                socket.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        }
}
