package com.braculink.service;

import com.braculink.common.ApiException;
import com.braculink.dao.FriendshipDao;
import com.braculink.dao.UserDao;
import com.braculink.dto.FriendSummaryDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FriendshipService {

    private final FriendshipDao friendshipDao;
    private final UserDao userDao;

    public FriendshipService(FriendshipDao friendshipDao, UserDao userDao) {
        this.friendshipDao = friendshipDao;
        this.userDao = userDao;
    }

    public void sendRequest(Long requesterId, Long addresseeId) {
        if (requesterId.equals(addresseeId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot send a friend request to yourself");
        }
        userDao.findById(addresseeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        if (friendshipDao.existsBetween(requesterId, addresseeId)) {
            throw new ApiException(HttpStatus.CONFLICT, "A friend request already exists between these users");
        }
        friendshipDao.insertRequest(requesterId, addresseeId);
    }

    public void accept(Long requesterId, Long addresseeId) {
        int rows = friendshipDao.updateStatus(requesterId, addresseeId, "ACCEPTED");
        if (rows == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Friend request not found");
        }
    }

    public void decline(Long requesterId, Long addresseeId) {
        int rows = friendshipDao.updateStatus(requesterId, addresseeId, "DECLINED");
        if (rows == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Friend request not found");
        }
    }

    public void unfriend(Long currentUserId, Long otherUserId) {
        int deleted = friendshipDao.delete(currentUserId, otherUserId) + friendshipDao.delete(otherUserId, currentUserId);
        if (deleted == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Friendship not found");
        }
    }

    public List<FriendSummaryDto> getFriends(Long userId) {
        return friendshipDao.findAcceptedFriends(userId);
    }

    public List<FriendSummaryDto> getPendingIncoming(Long userId) {
        return friendshipDao.findPendingIncoming(userId);
    }

    public boolean areFriends(Long a, Long b) {
        return friendshipDao.findAcceptedFriends(a).stream()
                .anyMatch(friend -> friend.getUserId().equals(b));
    }
}
