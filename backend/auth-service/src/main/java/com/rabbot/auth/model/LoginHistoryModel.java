package com.rabbot.auth.model;

import jakarta.persistence.*;
import lombok.Data;

import org.springframework.context.annotation.Lazy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

import com.rabbot.auth.Enum.LoginStatus;

@Data
@Entity
@Table(name = "login_history")
@EntityListeners(AuditingEntityListener.class)
public class LoginHistoryModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email_attempt", nullable = false)
    private String emailAttempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @Lazy
    private User user;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "device_type", length = 50)
    private String deviceType;

    @Column(name = "browser", length = 50)
    private String browser;

    @Column(name = "operating_system", length = 50)
    private String operatingSystem;

    @Column(name = "location", length = 100)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoginStatus status;

    @CreatedDate
    @Column(name = "attempt_date", nullable = false, updatable = false)
    private LocalDateTime attemptDate;
}