package com.musicfinder.shared.response.payload;

import java.time.LocalDate;

public record HistoryData(Long historyId, String keyword, String searchType, Integer resultCount,
        LocalDate searchedAt) {

}
