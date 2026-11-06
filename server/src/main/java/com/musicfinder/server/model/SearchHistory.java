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
public class SearchHistory {
    private Long historyId;
    private Long userId;
    private String keyword;
    private String normalizedKeyword;
    private String searchType;
    private Integer resultCount;
    private LocalDateTime searchedAt;
}
