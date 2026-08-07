package com.braculink.service;

import com.braculink.common.ApiException;
import com.braculink.dao.UserDao;
import com.braculink.dto.LiveStatusDto;
import com.braculink.dto.PublicProfileDto;
import com.braculink.dto.RoutineRowDto;
import com.braculink.dto.UpdateProfileRequest;
import com.braculink.dto.UserProfileDto;
import com.braculink.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserDao userDao;
    private final FriendshipService friendshipService;
    private final RoutineService routineService;

    public UserService(UserDao userDao, FriendshipService friendshipService, RoutineService routineService) {
        this.userDao = userDao;
        this.friendshipService = friendshipService;
        this.routineService = routineService;
    }

    public UserProfileDto getMe(Long userId) {
        return toProfileDto(requireUser(userId));
    }

    public UserProfileDto updateMe(Long userId, UpdateProfileRequest request) {
        userDao.updateProfile(userId, request.getPhoneNumber(), request.isPhonePublic(), request.getFbProfileUrl());
        return getMe(userId);
    }

    public PublicProfileDto getPublicProfile(Long userId) {
        User user = requireUser(userId);
        PublicProfileDto dto = new PublicProfileDto();
        dto.setFullName(user.getFullName());
        dto.setStudentId(user.getStudentId());
        dto.setFbProfileUrl(user.getFbProfileUrl());
        dto.setPhoneNumber(user.isPhonePublic() ? user.getPhoneNumber() : null);
        return dto;
    }

    public List<RoutineRowDto> getRoutineIfFriends(Long currentUserId, Long targetUserId) {
        requireFriends(currentUserId, targetUserId);
        return routineService.buildWeeklyRoutineForCurrentSemester(targetUserId);
    }

    public LiveStatusDto getStatusIfFriends(Long currentUserId, Long targetUserId) {
        requireFriends(currentUserId, targetUserId);
        return routineService.getLiveStatus(targetUserId);
    }

    private void requireFriends(Long currentUserId, Long targetUserId) {
        if (!friendshipService.areFriends(currentUserId, targetUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not friends with this user");
        }
    }

    private User requireUser(Long userId) {
        return userDao.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private UserProfileDto toProfileDto(User user) {
        UserProfileDto dto = new UserProfileDto();
        dto.setId(user.getId());
        dto.setStudentId(user.getStudentId());
        dto.setFullName(user.getFullName());
        dto.setBracuEmail(user.getBracuEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setPhonePublic(user.isPhonePublic());
        dto.setFbProfileUrl(user.getFbProfileUrl());
        dto.setEmailVerified(user.isEmailVerified());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}
