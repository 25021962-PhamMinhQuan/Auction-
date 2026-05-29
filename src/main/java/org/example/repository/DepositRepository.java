package org.example.repository;

import org.example.domain.user.DepositRequest;
import java.util.List;

public interface DepositRepository {
    void save(DepositRequest request);
    List<DepositRequest> findAll();
    List<DepositRequest> findByUserId(String userId);
    List<DepositRequest> findPending();
    void approve(int requestId);
    void reject(int requestId);
}
