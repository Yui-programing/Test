package com.musicfinder.server.repository;

import com.musicfinder.server.config.DatabaseConfig;
import com.musicfinder.server.util.DataException;
import com.musicfinder.server.model.Songs;
import lombok.extern.slf4j.Slf4j;
import com.musicfinder.server.util.TextNormalizer;
import com.musicfinder.shared.response.payload.SongData;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Arrays;

@Slf4j
public class SongRepository {
    // Hàm chuyển đổi resultSet sang đối tượng Song
    private Songs mapResultSetToSong(ResultSet rs) throws SQLException {
        Timestamp createdAtTs = rs.getTimestamp("created_at");
        Timestamp updatedAtTs = rs.getTimestamp("updated_at");

        return Songs.builder()
                .songId(rs.getLong("song_id"))
                .externalId(rs.getString("external_id"))
                .title(rs.getString("title"))
                .normalizedTitle(rs.getString("normalized_title"))
                .composer(rs.getString("composer"))
                .durationSec(rs.getInt("duration_seconds"))
                .audioUrl(rs.getString("audio_url"))
                .videoUrl(rs.getString("video_url"))
                .thumbnailUrl(rs.getString("thumbnail_url"))
                .searchCount(rs.getLong("search_count"))
                .createdAt(createdAtTs != null ? createdAtTs.toLocalDateTime() : null)
                .updatedAt(updatedAtTs != null ? updatedAtTs.toLocalDateTime() : null)
                .previewUrl(null)
                .build();
    }

    // Tim kiem nhac
    public List<Songs> searchSongs(String nameSong, int limit) {
        List<Songs> songs = new ArrayList<>();
        String sql = "SELECT * FROM SONGS WHERE normalized_title LIKE ? LIMIT ?";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            String normalizedName = TextNormalizer.normalize(nameSong);
            ps.setString(1, "%" + normalizedName + "%");
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    songs.add(mapResultSetToSong(rs));
                }
            }
            return songs;
        } catch (SQLException e) {
            log.error("Lỗi truy vấn CSDL khi tìm bài hát theo tên [{}]: {}", nameSong, e.getMessage(), e);
            throw new DataException("Lỗi truy vấn CSDL khi tìm bài hát theo tên " + nameSong, e);
        }
    }

    // Tim theo id song
    public Optional<Songs> findByIdSong(Long songId) {
        String sql = "SELECT * FROM SONGS WHERE song_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, songId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return Optional.of(mapResultSetToSong(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            log.error("Lỗi truy vấn CSDL khi tìm bài hát theo ID [{}]: {}", songId, e.getMessage(), e);
            throw new DataException("Lỗi truy vấn CSDL khi tìm bài hát ID " + songId, e);
        }
    }

    // Tìm theo external_id (kiểm tra trước khi sync từ API ngoài)
    public Optional<Songs> findByExternalId(String externalId) {
        String sql = "SELECT * FROM SONGS WHERE external_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, externalId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSong(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            log.error("Lỗi truy vấn CSDL khi tìm bài hát theo external_id [{}]: {}", externalId, e.getMessage(), e);
            throw new DataException("Lỗi truy vấn CSDL khi tìm bài hát theo external_id " + externalId, e);
        }
    }

    // Tăng lượt tìm kiếm(search_count)
    public void updateSearchCount(Long songId) {
        String sql = "UPDATE SONGS SET search_count = search_count + 1 WHERE song_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, songId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Lỗi cập nhật lượt tìm kiếm bài hát [{}]: {}", songId, e.getMessage(), e);
            throw new DataException("Lỗi cập nhật lượt tìm kiếm bài hát ID " + songId, e);
        }
    }

    // Them bai hat moi
    public Songs save(Songs song) {
        String sql = "INSERT INTO SONGS (external_id, title, normalized_title, composer, duration_seconds, "
                + "audio_url, video_url, thumbnail_url, search_count) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, song.getExternalId());
            ps.setString(2, song.getTitle());
            ps.setString(3, song.getNormalizedTitle());
            ps.setString(4, song.getComposer());
            ps.setInt(5, song.getDurationSec());
            ps.setString(6, song.getAudioUrl());
            ps.setString(7, song.getVideoUrl());
            ps.setString(8, song.getThumbnailUrl());
            ps.setLong(9, song.getSearchCount() != null ? song.getSearchCount() : 0L);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        song.setSongId(rs.getLong(1));
                    }
                }
            }
            return song;
        } catch (SQLException e) {
            log.error("Lỗi lưu bài hát [{}]: {}", song.getTitle(), e.getMessage(), e);
            throw new DataException("Lỗi lưu bài hát " + song.getTitle(), e);
        }
    }

    public void linkArtist(Long songId, Long artistId) {
        String sql = "INSERT INTO SONG_ARTISTS (song_id, artist_id) SELECT ?, ? "
                + "WHERE NOT EXISTS (SELECT 1 FROM SONG_ARTISTS WHERE song_id = ? AND artist_id = ?)";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, songId);
            ps.setLong(2, artistId);
            ps.setLong(3, songId);
            ps.setLong(4, artistId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataException("Không lưu được liên kết bài hát và ca sĩ", e);
        }
    }

    public boolean updateAudioUrl(Long songId, String audioUrl) {
        String sql = "UPDATE SONGS SET audio_url = ?, updated_at = CURRENT_TIMESTAMP WHERE song_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, audioUrl);
            ps.setLong(2, songId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataException("Không cập nhật được link audio bài hát ID " + songId, e);
        }
    }

    // Cập nhật thông tin bài hát
    public boolean update(Songs song) {
        String sql = "UPDATE SONGS SET external_id = ?, title = ?, normalized_title = ?, composer = ?, "
                + "duration_seconds = ?, audio_url = ?, video_url = ?, thumbnail_url = ?, "
                + "updated_at = CURRENT_TIMESTAMP " // <-- Tự động cập nhật thời gian sửa đổi
                + "WHERE song_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, song.getExternalId());
            ps.setString(2, song.getTitle());
            ps.setString(3, song.getNormalizedTitle());
            ps.setString(4, song.getComposer());
            ps.setInt(5, song.getDurationSec());
            ps.setString(6, song.getAudioUrl());
            ps.setString(7, song.getVideoUrl());
            ps.setString(8, song.getThumbnailUrl());
            ps.setLong(9, song.getSongId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Lỗi cập nhật bài hát [{}]: {}", song.getSongId(), e.getMessage(), e);
            throw new DataException("Lỗi cập nhật bài hát ID " + song.getSongId(), e);
        }
    }

    // Lay danh sach bai hat theo id artist
    public List<Songs> findByArtistId(Long artistId) {
        List<Songs> songs = new ArrayList<>();
        String sql = "SELECT s.* FROM SONGS s "
                + "JOIN SONG_ARTISTS sa ON s.song_id = sa.song_id "
                + "WHERE sa.artist_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, artistId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    songs.add(mapResultSetToSong(rs));
                }
            }
            return songs;
        } catch (SQLException e) {
            log.error("Lỗi truy vấn CSDL khi tìm bài hát theo artist_id [{}]: {}", artistId, e.getMessage(), e);
            throw new DataException("Lỗi truy vấn CSDL khi tìm bài hát theo artist_id " + artistId, e);
        }
    }

    public List<Songs> findByPlaylistId(Long playlistId) {
        List<Songs> songs = new ArrayList<>();
        String sql = "SELECT s.* FROM SONGS s "
                + "JOIN PLAYLIST_SONGS ps ON s.song_id = ps.song_id "
                + "WHERE ps.playlist_id = ? ORDER BY ps.position ASC";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, playlistId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    songs.add(mapResultSetToSong(rs));
                }
            }
            return songs;
        } catch (SQLException e) {
            log.error("Lỗi truy vấn CSDL khi tìm bài hát theo playlist_id [{}]: {}", playlistId, e.getMessage(), e);
            throw new DataException("Lỗi truy vấn CSDL khi tìm bài hát theo playlist_id " + playlistId, e);
        }
    }

    public List<SongData> findSongDataByPlaylistId(Long playlistId) {
        List<SongData> list = new ArrayList<>();
        String sql = "SELECT s.*, STRING_AGG(a.name, ', ' ORDER BY a.name) AS artist_names "
                + "FROM SONGS s "
                + "JOIN PLAYLIST_SONGS ps ON s.song_id = ps.song_id "
                + "LEFT JOIN SONG_ARTISTS sa ON s.song_id = sa.song_id "
                + "LEFT JOIN ARTISTS a ON sa.artist_id = a.artist_id "
                + "WHERE ps.playlist_id = ? "
                + "GROUP BY s.song_id, ps.position "
                + "ORDER BY ps.position ASC";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, playlistId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String artistsStr = rs.getString("artist_names");
                    List<String> artistNames = (artistsStr != null && !artistsStr.isBlank())
                            ? Arrays.asList(artistsStr.split(", "))
                            : List.of();
                    list.add(new SongData(
                            rs.getLong("song_id"),
                            rs.getString("title"),
                            rs.getString("composer"),
                            artistNames,
                            rs.getInt("duration_seconds"),
                            rs.getString("audio_url"),
                            rs.getString("video_url"),
                            rs.getString("thumbnail_url"),
                            rs.getLong("search_count"),
                            rs.getString("preview_url"),
                            rs.getString("external_id")));
                }
            }
            return list;
        } catch (SQLException e) {
            log.error("Lỗi khi lấy bài hát kèm ca sĩ theo playlist [{}]: {}", playlistId, e.getMessage(), e);
            throw new DataException("Lỗi truy vấn danh sách bài hát trong playlist", e);
        }
    }

    // Thống kê: Lấy top bài hát được tìm kiếm nhiều nhất
    public List<Songs> findTopSongs(int limit) {
        List<Songs> songs = new ArrayList<>();
        String sql = "SELECT * FROM SONGS ORDER BY search_count DESC, created_at DESC LIMIT ?";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    songs.add(mapResultSetToSong(rs));
                }
            }
            return songs;
        } catch (SQLException e) {
            log.error("Lỗi truy vấn top bài hát thịnh hành: {}", e.getMessage(), e);
            throw new DataException("Lỗi truy vấn top bài hát thịnh hành", e);
        }
    }
}
