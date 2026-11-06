package com.musicfinder.server.repository;

import com.musicfinder.server.config.DatabaseConfig;
import com.musicfinder.server.util.DataException;
import com.musicfinder.server.model.SearchCache;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
public class SearchCacheRepository {

    private SearchCache mapResultSetToSearchCache(ResultSet rs) throws SQLException {
        Timestamp createdAtTs = rs.getTimestamp("created_at");
        Timestamp expiresAtTs = rs.getTimestamp("expires_at");
        return SearchCache.builder()
                .cacheId(rs.getLong("cache_id"))
                .cacheKey(rs.getString("cache_key"))
                .keyword(rs.getString("keyword"))
                .normalizedKeyword(rs.getString("normalized_keyword"))
                .searchType(rs.getString("search_type"))
                .responseJson(rs.getString("response_json"))
                .createdAt(createdAtTs != null ? createdAtTs.toLocalDateTime() : null)
                .expiresAt(expiresAtTs != null ? expiresAtTs.toLocalDateTime() : null)
                .build();
    }

    public Optional<SearchCache> findByCacheKey(String cacheKey) {
        String sql = "SELECT * FROM SEARCH_CACHE WHERE cache_key = ? AND expires_at > CURRENT_TIMESTAMP";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cacheKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSearchCache(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            log.error("Lỗi truy vấn search cache theo key [{}]: {}", cacheKey, e.getMessage(), e);
            throw new DataException("Lỗi truy vấn search cache theo key " + cacheKey, e);
        }
    }

    public Optional<SearchCache> findByCacheKeyEvenIfExpired(String cacheKey) {
        String sql = "SELECT * FROM SEARCH_CACHE WHERE cache_key = ?";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cacheKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSearchCache(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            log.error("Lỗi truy vấn search cache (kể cả hết hạn) theo key [{}]: {}", cacheKey, e.getMessage(), e);
            throw new DataException("Lỗi truy vấn search cache theo key " + cacheKey, e);
        }
    }

    public SearchCache save(SearchCache cache) {
        String sql = "INSERT INTO SEARCH_CACHE (cache_key, keyword, normalized_keyword, search_type, response_json, expires_at) "
                + "VALUES (?, ?, ?, ?, ?::jsonb, ?) "
                + "ON CONFLICT (cache_key) DO UPDATE SET "
                + "response_json = EXCLUDED.response_json, "
                + "expires_at = EXCLUDED.expires_at";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, cache.getCacheKey());
            ps.setString(2, cache.getKeyword());
            ps.setString(3, cache.getNormalizedKeyword());
            ps.setString(4, cache.getSearchType());
            ps.setString(5, cache.getResponseJson());

            LocalDateTime expiresAt = cache.getExpiresAt() != null
                    ? cache.getExpiresAt()
                    : LocalDateTime.now().plusMinutes(3);
            ps.setTimestamp(6, Timestamp.valueOf(expiresAt));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next())
                    cache.setCacheId(rs.getLong(1));
            }
            return cache;
        } catch (SQLException e) {
            log.error("Lỗi lưu search cache [{}]: {}", cache.getCacheKey(), e.getMessage(), e);
            throw new DataException("Lỗi lưu search cache key " + cache.getCacheKey(), e);
        }
    }

    public int deleteExpiredOlderThanDays(int days) {
        String sql = "DELETE FROM SEARCH_CACHE WHERE expires_at < CURRENT_TIMESTAMP - make_interval(days => ?)";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, days);
            return ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Lỗi xóa search cache hết hạn quá {} ngày: {}", days, e.getMessage(), e);
            throw new DataException("Lỗi xóa search cache hết hạn", e);
        }
    }
}
