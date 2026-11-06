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
public class SearchCache {
    private Long cacheId;
    private String cacheKey;
    private String keyword;
    private String normalizedKeyword;
    private String searchType;
    private String responseJson;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
