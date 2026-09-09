package com.formation.notification.repository;

import com.formation.notification.model.Notification;
import com.formation.notification.model.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderBySentDateDesc(Long userId);

    List<Notification> findByStatus(NotificationStatus status);
}
