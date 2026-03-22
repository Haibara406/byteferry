package com.byteferry.byteferry.repository;

import com.byteferry.byteferry.model.entity.FriendSessionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FriendSessionHistoryRepository extends JpaRepository<FriendSessionHistory, Long> {

    List<FriendSessionHistory> findByUserIdOrderByClosedAtDesc(Long userId);

    long countBySessionId(String sessionId);

    @Modifying
    @Query("DELETE FROM FriendSessionHistory h WHERE h.id IN :ids")
    void deleteByIdIn(@Param("ids") List<Long> ids);
}
