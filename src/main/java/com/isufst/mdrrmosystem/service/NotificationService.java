package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.User;
import com.isufst.mdrrmosystem.request.NotificationCreateRequest;
import com.isufst.mdrrmosystem.response.NotificationResponse;

import java.util.Collection;
import java.util.List;

public interface NotificationService {
    NotificationResponse create(NotificationCreateRequest request);
    List<NotificationResponse> getMyNotifications();
    void markAsRead(Long id);
    void markAllAsRead();
    void clearRead();
    void notifyUser(User recipient, String type, String title, String message, String referenceType, Long referenceId);
    void notifyUsers(Collection<User> recipients, String type, String title, String message, String referenceType, Long referenceId);
    void notifyAllUsers(String type, String title, String message, String referenceType, Long referenceId);
    void notifyAdminsAndManagers(String type, String title, String message, String referenceType, Long referenceId);
}
