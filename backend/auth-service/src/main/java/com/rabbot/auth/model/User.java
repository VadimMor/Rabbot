package com.rabbot.auth.model;

import jakarta.persistence.*;
import java.util.List;
import lombok.Data;

import com.rabbot.auth.Enum.RoleUser;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleUser role = RoleUser.ROLE_USER;

    // Связь: Один пользователь может владеть несколькими Workspaces
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Workspace> workspaces;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LoginHistoryModel> loginHistories;
}