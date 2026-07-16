package com.rabbot.auth.repository;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDateTime;

import com.rabbot.auth.model.LoginHistory;
import com.rabbot.auth.Enum.LoginStatus;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
    List<LoginHistory> findTop50ByUserIdOrderByAttemptDateDesc(Long userId);

    long countByIpAddressAndStatusAndAttemptDateAfter(
        String ipAddress,
        LoginStatus status,
        LocalDateTime afterDate
    );

    long countByIpAddressAndEmailAttemptAndAttemptDateAfter(
        String ipAddress,
        String emailAttempt,
        LocalDateTime afterDate
    );
}
