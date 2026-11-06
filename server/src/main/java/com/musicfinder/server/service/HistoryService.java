package com.musicfinder.server.service;

import java.util.ArrayList;
import java.util.List;

import com.musicfinder.server.model.SearchHistory;
import com.musicfinder.server.repository.SearchHistoryRepository;
import com.musicfinder.server.util.TextNormalizer;
import com.musicfinder.shared.response.payload.HistoryData;

// Lịch sử tìm kiếm của từng user. Mọi hàm đều nhận userId nên user này không thể xem/xóa lịch sử của user khác
public class HistoryService {
    private static final int MAX_KEYWORD_LENGTH = 255;
    private static final int HISTORY_LIMIT = 10;

    private final SearchHistoryRepository historyRepo = new SearchHistoryRepository();

    // GET_HISTORY: lịch sử của user, mới nhất ở trên cùng (repository đã ORDER BY
    // searched_at DESC)
    public List<HistoryData> getHistory(Long userId) {
        List<SearchHistory> histories = historyRepo.findByUserId(userId, HISTORY_LIMIT);

        List<HistoryData> result = new ArrayList<>();
        for (SearchHistory h : histories) {
            result.add(new HistoryData(
                    h.getHistoryId(),
                    h.getKeyword(),
                    h.getSearchType(),
                    h.getResultCount(),
                    h.getSearchedAt() != null ? h.getSearchedAt().toLocalDate() : null)); // HistoryData chỉ lưu ngày
        }
        return result;
    }

    // REMOVE_ROW_HISTORY: xóa 1 dòng. Trả false nếu không có dòng đó hoặc dòng đó
    // không thuộc user này
    public boolean removeRow(Long userId, Long historyId) {
        if (historyId == null || historyId <= 0) {
            throw new IllegalArgumentException("ID lịch sử không hợp lệ");
        }
        return historyRepo.deleteById(historyId, userId);
    }

    // CLEAR_HISTORY: xóa toàn bộ lịch sử của user (không có gì để xóa cũng không
    // sao)
    public void clearHistory(Long userId) {
        historyRepo.deleteByUserId(userId);
    }

    // Ghi 1 lượt tìm kiếm. SearchService sẽ gọi hàm này sau mỗi lần SEARCH_SONG /
    // SEARCH_ARTIST
    public void recordSearch(Long userId, String keyword, String searchType, int resultCount) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("ID người dùng không hợp lệ");
        }

        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("Từ khóa không được để trống");
        }

        if (!"SONG".equals(searchType) && !"ARTIST".equals(searchType)) {
            throw new IllegalArgumentException("Loại tìm kiếm không hợp lệ");
        }

        if (resultCount < 0) {
            throw new IllegalArgumentException("Số kết quả không được âm");
        }

        String cleanKeyword = keyword.trim();

        if (cleanKeyword.length() > MAX_KEYWORD_LENGTH) {
            cleanKeyword = cleanKeyword.substring(0, MAX_KEYWORD_LENGTH);
        }

        SearchHistory history = SearchHistory.builder()
                .userId(userId)
                .keyword(cleanKeyword)
                .normalizedKeyword(
                        TextNormalizer.normalize(cleanKeyword))
                .searchType(searchType)
                .resultCount(resultCount)
                .build();

        historyRepo.save(history);
    }
}