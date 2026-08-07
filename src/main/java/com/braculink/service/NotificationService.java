package com.braculink.service;

import com.braculink.common.ApiException;
import com.braculink.dao.NotificationDao;
import com.braculink.dto.NotificationDto;
import com.braculink.model.Notification;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * The single entry point for raising an in-app notification.
 *
 * <p>Every feature that needs to tell a user something — a friend request, a swap proposal, a
 * confirmation — calls {@link #notify} rather than touching {@link NotificationDao} directly, so the
 * JSON-payload encoding lives in exactly one place.
 */
@Service
public class NotificationService {

    private final NotificationDao notificationDao;
    private final ObjectMapper objectMapper;

    public NotificationService(NotificationDao notificationDao, ObjectMapper objectMapper) {
        this.notificationDao = notificationDao;
        this.objectMapper = objectMapper;
    }

    public void notify(Long userId, String type, Map<String, Object> payload) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setPayload(writeJson(payload));
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationDao.insert(notification);
    }

    public List<NotificationDto> getMyNotifications(Long userId) {
        return notificationDao.findByUser(userId).stream().map(this::toDto).toList();
    }

    public void markRead(Long id, Long userId) {
        if (notificationDao.markRead(id, userId) == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Notification not found");
        }
    }

    private NotificationDto toDto(Notification notification) {
        NotificationDto dto = new NotificationDto();
        dto.setId(notification.getId());
        dto.setType(notification.getType());
        dto.setPayload(readJson(notification.getPayload()));
        dto.setRead(notification.isRead());
        dto.setCreatedAt(notification.getCreatedAt());
        return dto;
    }

    private String writeJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize notification payload", e);
        }
    }

    private Map<String, Object> readJson(String payload) {
        if (payload == null) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse notification payload: " + payload, e);
        }
    }
}
