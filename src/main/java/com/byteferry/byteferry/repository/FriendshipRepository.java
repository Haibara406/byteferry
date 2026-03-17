package com.byteferry.byteferry.repository;

import com.byteferry.byteferry.model.entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    List<Friendship> findByUserIdAndStatus(Long userId, Friendship.Status status);

    List<Friendship> findByFriendIdAndStatus(Long friendId, Friendship.Status status);

    Optional<Friendship> findByUserIdAndFriendId(Long userId, Long friendId);

    boolean existsByUserIdAndFriendIdAndStatus(Long userId, Long friendId, Friendship.Status status);

    void deleteByUserIdAndFriendId(Long userId, Long friendId);
}
