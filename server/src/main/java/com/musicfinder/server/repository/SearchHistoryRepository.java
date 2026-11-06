package com.musicfinder.server.repository;

import com.musicfinder.server.config.DatabaseConfig;
import com.musicfinder.server.util.DataException;
import com.musicfinder.server.model.SearchHistory;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SearchHistoryRepository {

    private SearchHistory mapResultSetToSearchHistory(ResultSet rs) throws SQLException {
        Timestamp searchedAtTs = rs.getTimestamp("searched_at");

        return SearchHistory.builder()
                .historyId(rs.getLong("history_id"))
                .userId(rs.getLong("user_id"))
                .keyword(rs.getString("keyword"))
                .normalizedKeyword(rs.getString("normalized_keyword"))
                .searchType(rs.getString("search_type"))
                .resultCount(rs.getInt("result_count"))
                .searchedAt(searchedAtTs != null ? searchedAtTs.toLocalDateTime() : null)
                .build();
    }

    public List<SearchHistory> findByUserId(Long userId) {
        List<SearchHistory> list = new ArrayList<>();
        String sql = "SELECT * FROM SEARCH_HISTORY WHERE user_id = ? ORDER BY searched_at DESC";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToSearchHistory(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            log.error("Lỗi truy vấn lịch sử tìm kiếm theo user_id [{}]: {}", userId, e.getMessage(), e);
            throw new DataException("Lỗi truy vấn lịch sử tìm kiếm theo user_id " + userId, e);
        }
    }

    public SearchHistory save(SearchHistory history) {
        String sql = "INSERT INTO SEARCH_HISTORY (user_id, keyword, normalized_keyword, search_type, result_count) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, history.getUserId());
            ps.setString(2, history.getKeyword());
            ps.setString(3, history.getNormalizedKeyword());
            ps.setString(4, history.getSearchType());
            if (history.getResultCount() != null) {
                ps.setInt(5, history.getResultCount());
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next())
                        history.setHistoryId(rs.getLong(1));
                }
            }
            return history;
        } catch (SQLException e) {
            log.error("Lỗi lưu lịch sử tìm kiếm user [{}], keyword [{}]: {}", history.getUserId(), history.getKeyword(),
                    e.getMessage(), e);
            throw new DataException("Lỗi lưu lịch sử tìm kiếm", e);
        }
    }

    public boolean deleteByUserId(Long userId) {
        String sql = "DELETE FROM SEARCH_HISTORY WHERE user_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Lỗi xóa lịch sử tìm kiếm theo user_id [{}]: {}", userId, e.getMessage(), e);
            throw new DataException("Lỗi xóa lịch sử tìm kiếm theo user_id " + userId, e);
        }
    }

    public List<SearchHistory> findTopKeywords(String searchType, int limit) {
        List<SearchHistory> list = new ArrayList<>();
        String sql = "SELECT keyword, normalized_keyword, search_type, COUNT(*) AS search_count "
                + "FROM SEARCH_HISTORY WHERE search_type = ? "
                + "GROUP BY keyword, normalized_keyword, search_type "
                + "ORDER BY search_count DESC LIMIT ?";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, searchType);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(SearchHistory.builder()
                            .keyword(rs.getString("keyword"))
                            .normalizedKeyword(rs.getString("normalized_keyword"))
                            .searchType(rs.getString("search_type"))
                            .resultCount((int) rs.getLong("search_count"))
                            .build());
                }
            }
            return list;
        } catch (SQLException e) {
            log.error("Lỗi lấy top keywords loại [{}]: {}", searchType, e.getMessage(), e);
            throw new DataException("Lỗi lấy top keywords loại " + searchType, e);
        }
    }

    public boolean deleteById(Long historyId, Long userId) {
        String sql = "DELETE FROM SEARCH_HISTORY WHERE history_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, historyId);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0; // false = không có dòng đó hoặc không thuộc user này
        } catch (SQLException e) {
            log.error("Lỗi xóa lịch sử [{}] của user [{}]: {}", historyId, userId, e.getMessage(), e);
            throw new DataException("Lỗi xóa lịch sử ID " + historyId, e);
        }
    }

    public List<SearchHistory> findByUserId(Long userId, int limit) {
        List<SearchHistory> list = new ArrayList<>();
        String sql = "SELECT * FROM SEARCH_HISTORY WHERE user_id = ? ORDER BY searched_at DESC LIMIT ?";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToSearchHistory(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            log.error("Lỗi truy vấn lịch sử tìm kiếm theo user_id [{}]: {}", userId, e.getMessage(), e);
            throw new DataException("Lỗi truy vấn lịch sử tìm kiếm theo user_id " + userId, e);
        }
    }
}
