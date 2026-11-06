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

public class Songs {
    private Long songId;
    private String externalId;
    private String title;
    private String normalizedTitle;
    private String composer;
    private int durationSec;
    private String audioUrl;
    private String videoUrl;
    private String thumbnailUrl;
    private Long searchCount;
    private String previewUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
