package com.byteferry.byteferry.service;

import com.byteferry.byteferry.model.entity.Friendship;
import com.byteferry.byteferry.model.entity.User;
import com.byteferry.byteferry.repository.FriendshipRepository;
import com.byteferry.byteferry.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    public Friendship sendRequest(Long senderId, String targetUsername) {
        User target = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new RuntimeException("User '" + targetUsername + "' does not exist"));
        if (target.getId().equals(senderId)) {
            throw new RuntimeException("You cannot add yourself as a friend");
        }
        if (friendshipRepository.findByUserIdAndFriendId(senderId, target.getId()).isPresent()) {
            throw new RuntimeException("Friend request already sent or you are already friends");
        }
        if (friendshipRepository.findByUserIdAndFriendId(target.getId(), senderId).isPresent()) {
            throw new RuntimeException("This user has already sent you a friend request");
        }
        Friendship f = Friendship.builder()
                .userId(senderId)
                .friendId(target.getId())
                .status(Friendship.Status.PENDING)
                .build();
        return friendshipRepository.save(f);
    }

    public Friendship acceptRequest(Long receiverId, Long friendshipId) {
        Friendship f = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        if (!f.getFriendId().equals(receiverId)) {
            throw new RuntimeException("Access denied");
        }
        if (f.getStatus() != Friendship.Status.PENDING) {
            throw new RuntimeException("Request is not pending");
        }
        f.setStatus(Friendship.Status.ACCEPTED);
        f.setAcceptedAt(LocalDateTime.now());
        friendshipRepository.save(f);

        Friendship reverse = Friendship.builder()
                .userId(receiverId)
                .friendId(f.getUserId())
                .status(Friendship.Status.ACCEPTED)
                .acceptedAt(LocalDateTime.now())
                .build();
        friendshipRepository.save(reverse);
        return f;
    }

    public void rejectRequest(Long receiverId, Long friendshipId) {
        Friendship f = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        if (!f.getFriendId().equals(receiverId)) {
            throw new RuntimeException("Access denied");
        }
        friendshipRepository.delete(f);
    }

    public List<Friendship> getFriends(Long userId) {
        return friendshipRepository.findByUserIdAndStatus(userId, Friendship.Status.ACCEPTED);
    }

    public List<Friendship> getPendingRequests(Long userId) {
        return friendshipRepository.findByFriendIdAndStatus(userId, Friendship.Status.PENDING);
    }

    public List<Friendship> getSentRequests(Long userId) {
        return friendshipRepository.findByUserIdAndStatus(userId, Friendship.Status.PENDING);
    }

    @Transactional
    public void removeFriend(Long userId, Long friendId) {
        friendshipRepository.deleteByUserIdAndFriendId(userId, friendId);
        friendshipRepository.deleteByUserIdAndFriendId(friendId, userId);
    }

    @Transactional
    public void blockFriend(Long blockerId, Long targetId) {
        Friendship existing = friendshipRepository.findByUserIdAndFriendId(blockerId, targetId).orElse(null);
        if (existing != null) {
            existing.setStatus(Friendship.Status.BLOCKED);
            friendshipRepository.save(existing);
        } else {
            friendshipRepository.save(Friendship.builder()
                    .userId(blockerId).friendId(targetId).status(Friendship.Status.BLOCKED).build());
        }
        friendshipRepository.findByUserIdAndFriendId(targetId, blockerId)
                .ifPresent(friendshipRepository::delete);
    }

    public boolean areFriends(Long userId, Long friendId) {
        return friendshipRepository.existsByUserIdAndFriendIdAndStatus(userId, friendId, Friendship.Status.ACCEPTED);
    }
}
