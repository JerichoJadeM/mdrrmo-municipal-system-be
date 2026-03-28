package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Notification;
import com.isufst.mdrrmosystem.entity.User;
import com.isufst.mdrrmosystem.repository.NotificationRepository;
import com.isufst.mdrrmosystem.repository.UserRepository;
import com.isufst.mdrrmosystem.request.NotificationCreateRequest;
import com.isufst.mdrrmosystem.response.NotificationResponse;
import com.isufst.mdrrmosystem.util.FindAuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final FindAuthenticatedUser findAuthenticatedUser;

    public NotificationServiceImpl(NotificationRepository notificationRepository, UserRepository userRepository, FindAuthenticatedUser findAuthenticatedUser) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.findAuthenticatedUser = findAuthenticatedUser;
    }

    @Override
    @Transactional
    public NotificationResponse create(NotificationCreateRequest request) {
        User recipient = userRepository.findById(request.recipientUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient user not found"));
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(normalize(request.type()));
        notification.setTitle(request.title().trim());
        notification.setMessage(request.message().trim());
        notification.setReferenceType(request.referenceType());
        notification.setReferenceId(request.referenceId());
        return map(notificationRepository.save(notification));
    }

    @Override
    public List<NotificationResponse> getMyNotifications() {
        User currentUser = findAuthenticatedUser.getAuthenticatedUser();
        return notificationRepository.findByRecipient_IdOrderByCreatedAtDesc(currentUser.getId()).stream().map(this::map).toList();
    }

    @Override @Transactional
    public void markAsRead(Long id) {
        User currentUser = findAuthenticatedUser.getAuthenticatedUser();
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!(notification.getRecipient().getId() == currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot mark another user's notification");
        }
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Override @Transactional
    public void markAllAsRead() {
        User currentUser = findAuthenticatedUser.getAuthenticatedUser();
        notificationRepository.markAllAsReadByUserId(currentUser.getId());
    }

    @Override @Transactional
    public void notifyUser(User recipient, String type, String title, String message, String referenceType, Long referenceId) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(normalize(type));
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);
        notificationRepository.save(notification);
    }

    @Override @Transactional
    public void notifyUsers(Collection<User> recipients, String type, String title, String message, String referenceType, Long referenceId) {
        if (recipients == null || recipients.isEmpty()) return;
        List<Notification> notifications = recipients.stream().distinct().map(user -> {
            Notification n = new Notification();
            n.setRecipient(user);
            n.setType(normalize(type));
            n.setTitle(title);
            n.setMessage(message);
            n.setReferenceType(referenceType);
            n.setReferenceId(referenceId);
            return n;
        }).collect(Collectors.toList());
        notificationRepository.saveAll(notifications);
    }

    @Override @Transactional
    public void notifyAllUsers(String type, String title, String message, String referenceType, Long referenceId) {
        notifyUsers(userRepository.findAll(), type, title, message, referenceType, referenceId);
    }

    @Override @Transactional
    public void notifyAdminsAndManagers(String type, String title, String message, String referenceType, Long referenceId) {
        List<User> users = userRepository.findAll().stream().filter(this::isAdminOrManager).toList();
        notifyUsers(users, type, title, message, referenceType, referenceId);
    }

    @Override
    @Transactional
    public void clearRead() {
        User currentUser = findAuthenticatedUser.getAuthenticatedUser();
        notificationRepository.deleteReadByUserId(currentUser.getId());
    }

    private boolean isAdminOrManager(User user) {
        return user.getAuthorities().stream().anyMatch(authority -> {
            String role = authority.getAuthority();
            return "ROLE_ADMIN".equals(role) || "ROLE_MANAGER".equals(role);
        });
    }

    private String normalize(String value) { return value == null ? "SYSTEM" : value.trim().toUpperCase(); }

    private NotificationResponse map(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getType(), notification.getTitle(), notification.getMessage(),
                notification.getReferenceType(), notification.getReferenceId(), notification.getIsRead(), notification.getCreatedAt());
    }
}
