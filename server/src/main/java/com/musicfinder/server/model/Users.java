package com.musicfinder.server.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data // Tự sinh: Getter, Setter, toString, equals, hashCode
@NoArgsConstructor
@AllArgsConstructor 
@Builder

public class Users {
    private Long userId;
    private String username;
    private String passwordHash;
    private String fullName;
    private String email;
    private String avatarUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
