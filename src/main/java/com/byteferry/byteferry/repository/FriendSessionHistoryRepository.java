package com.byteferry.byteferry.repository;

import com.byteferry.byteferry.model.entity.FriendSessionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FriendSessionHistoryRepository extends JpaRepository<FriendSessionHistory, Long> {

    List<FriendSessionHistory> findByUserIdOrderByClosedAtDesc(Long userId);

    long countBySessionId(String sessionId);
}
