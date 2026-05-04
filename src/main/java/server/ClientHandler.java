package server;

import org.example.dao.UserDAO;
import org.example.model.auction.Auction;
import org.example.model.user.Bidder;
import org.example.model.user.User;
import org.example.service.AuctionService;
import org.example.service.ServiceFactory;
import org.example.util.AutoBid;

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
    // xac dinh xem hanh dong la chi mo
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
                    // thong bao gia moi cho cac bidder khac
                    AuctionServer.broadCast(updateMessage);
                } catch (Exception e) {
                    sendMesage("Lỗi: "+e.getMessage());
                }
                break;
            }
            case "AUTOBID":{ //AUTOBID|auctionId|maxbid|increment
                int auctionId= Integer.parseInt(parts[1]);
                double maxBid = Double.parseDouble(parts[2]);
                double increment = Double.parseDouble(parts[3]);

                Auction auction = findbyId(auctionId);
                AutoBid autoBid = new AutoBid((Bidder) currentUser,maxBid,increment);
                auctionService.registerAutoBid(auction,autoBid);
                sendMesage("AUTOBID REGISTERED");
                break;
            }
            case "STATUS":{
                int auctionId = Integer.parseInt(parts[1]);
                Auction auction = findbyId(auctionId);
                if (auction != null) {
                    sendMesage("STATUS|" + auction.getStatus()
                            + "|" + auction.getCurrentPrice());
                } else {
                    sendMesage("ERROR|Auction not found");
                }
                break;
            }
            case "LOGIN": { // xu ly auth
                // "LOGIN|username|password"  (xử lý auth)
                String username = parts[1];
                String password = parts[2];

                UserDAO userDAO = new UserDAO();
                User user = userDAO.findByUsername(username); // tìm trong DB

                if (user == null) {
                    sendMesage("ERROR|Không tìm thấy tài khoản");
                } else if (!user.getPassword().equals(password)) {
                    sendMesage("ERROR|Sai mật khẩu");
                } else {
                    currentUser = user; // lưu lại user đang đăng nhập trên kết nối này
                    sendMesage("OK|" + user.getRole() + "|" + user.getUsername());
                    // gửi về ROLE để biết là BIDDER, SELLER hay ADMIN
                }
                break;
            }

            default:
                sendMesage("ERROR|Unknown command: " + command);
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
        private Auction findbyId(int id){      // server nhan bid kieu BID|id|rpice nen nma placebid dung kieu la auction nen ph tim id => auction
        return auctionService.findbyId(id);
        }
}
