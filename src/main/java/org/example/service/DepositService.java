package org.example.service;

import org.example.domain.user.DepositRequest;
import org.example.repository.DepositRepository;

import java.util.List;

public class DepositService {

    private final DepositRepository repo;

    public DepositService(DepositRepository repo) {
        this.repo = repo;
    }

    /**
     * User gửi yêu cầu nạp tiền.
     * @return thông báo kết quả
     */
    public String requestDeposit(String userId, String username, double amount, String note) {
        if (amount <= 0) return "Số tiền nạp phải lớn hơn 0";
        if (amount > 50_000_000_000.0) return "Số tiền nạp tối đa là 50 tỷ VND";
        DepositRequest req = new DepositRequest(userId, username, amount, note);
        repo.save(req);
        return "Yêu cầu nạp tiền đã được gửi đến Admin!";
    }

    public List<DepositRequest> getAllRequests()           { return repo.findAll(); }
    public List<DepositRequest> getPendingRequests()       { return repo.findPending(); }
    public List<DepositRequest> getRequestsByUser(String userId) { return repo.findByUserId(userId); }

    /** Admin duyệt yêu cầu — balance được cộng trong DepositDAO.approve() */
    public void approve(int requestId) { repo.approve(requestId); }

    /** Admin từ chối yêu cầu */
    public void reject(int requestId)  { repo.reject(requestId); }
}
