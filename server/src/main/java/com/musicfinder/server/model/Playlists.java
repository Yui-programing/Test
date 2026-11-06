package com.musicfinder.server.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Playlists {
    private Long playlistId;
    private Long userId;
    private String name;
    private String description;
    private String type;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
