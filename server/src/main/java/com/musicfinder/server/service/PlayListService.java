package com.musicfinder.server.service;

import java.util.List;

import com.musicfinder.server.model.Playlists;
import com.musicfinder.server.model.Songs;
import com.musicfinder.server.repository.ArtistRepository;
import com.musicfinder.server.repository.PlaylistRepository;
import com.musicfinder.server.repository.SongRepository;
import com.musicfinder.server.util.DtoMapper;
import com.musicfinder.shared.request.payload.Add_RemoveToPlaylistPayload;
import com.musicfinder.shared.request.payload.Create_UpdateToPlaylistPayload;
import com.musicfinder.shared.request.payload.Remove_OpenPlaylistPayload;
import com.musicfinder.shared.response.payload.GetFavoritesData;
import com.musicfinder.shared.response.payload.GetPlaylistSongsData;
import com.musicfinder.shared.response.payload.PlayListData;
import com.musicfinder.shared.response.payload.SongData;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PlayListService {
    private final PlaylistRepository playlistRepo = new PlaylistRepository();
    private final SongRepository songRepo = new SongRepository();
    private final ArtistRepository artistRepo = new ArtistRepository();
    private final SearchService searchService;

    public PlayListService(SearchService searchService) {
        this.searchService = searchService;
    }

    // =================PLAYLIST NORMAL=======================

    public PlayListData createPlaylist(Long userId, Create_UpdateToPlaylistPayload payload) {
        validateUser(userId);
        if (payload == null || payload.name() == null || payload.name().isBlank())
            throw new IllegalArgumentException("Tên playlist không được để trống!");

        Playlists playlist = Playlists.builder()
                .userId(userId)
                .name(payload.name().trim())
                .description(payload.description() != null ? payload.description().trim() : "")
                .type("NORMAL")
                .build();

        return DtoMapper.toPlayListData(playlistRepo.save(playlist));
    }

    public List<PlayListData> getPlaylists(Long userId) {
        validateUser(userId);
        return playlistRepo.findByUserIdNormal(userId)
                .stream()
                .map(DtoMapper::toPlayListData)
                .toList();
    }

    public boolean editPlaylist(Long userId, Create_UpdateToPlaylistPayload payload) {
        validateUser(userId);
        if (payload == null || payload.playlistId() == null || payload.playlistId() <= 0) {
            throw new IllegalArgumentException("ID playlist không hợp lệ");
        }
        if (payload == null || payload.name() == null || payload.name().isBlank())
            throw new IllegalArgumentException("Tên playlist không được để trống!");

        Playlists playlist = Playlists.builder()
                .playlistId(payload.playlistId())
                .userId(userId)
                .name(payload.name().trim())
                .description(payload.description() != null ? payload.description().trim() : "")
                .build();
        return playlistRepo.update(playlist);
    }

    public boolean deletePlaylist(Long userId, Remove_OpenPlaylistPayload payload) {
        validateUser(userId);
        if (payload == null || payload.playListID() == null || payload.playListID() <= 0) {
            throw new IllegalArgumentException("ID playlist không hợp lệ");
        }
        return playlistRepo.deleteById(payload.playListID(), userId);
    }

    public GetPlaylistSongsData getPlaylistSongs(Long userId, Remove_OpenPlaylistPayload payload) {
        validateUser(userId);
        if (payload == null || payload.playListID() == null || payload.playListID() <= 0) {
            throw new IllegalArgumentException("ID playlist không hợp lệ");
        }
        if (playlistRepo.findById(payload.playListID(), userId).isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy playlist");
        }
        List<SongData> songs = songRepo.findSongDataByPlaylistId(payload.playListID());
        return new GetPlaylistSongsData(songs);
    }

    public boolean addSongToPlaylist(Long userId, Add_RemoveToPlaylistPayload payload) {
        if (payload == null)
            throw new IllegalArgumentException("Dữ liệu yêu cầu không hợp lệ");
        validateUser(userId);
        checkOwnership(userId, payload.playlistId()); // kiểm tra chủ sở hữu TRƯỚC khi import
        return playlistRepo.addSongToPlaylist(payload.playlistId(), resolveSongIdToAdd(payload));
    }

    public boolean removeSongFromPlaylist(Long userId, Add_RemoveToPlaylistPayload payload) {
        validateUser(userId);
        checkOwnership(userId, payload.playlistId());
        return playlistRepo.removeSongFromPlaylist(payload.playlistId(), resolveSongIdToAdd(payload));
    }

    // ===========================BÀI HÁT YUE THÍCH===========================
    public GetFavoritesData getFavorites(Long userId) {
        validateUser(userId);
        Long favPlaylistId = playlistRepo.getOrCreateFavoritePlaylist(userId);
        // Gọi 1 query duy nhất:
        List<SongData> songs = songRepo.findSongDataByPlaylistId(favPlaylistId);
        return new GetFavoritesData(songs);
    }

    public boolean addFavorite(Long userId, Add_RemoveToPlaylistPayload payload) {
        validateUser(userId);
        Long songId = resolveSongIdToAdd(payload);
        Long favPlaylistId = playlistRepo.getOrCreateFavoritePlaylist(userId);
        return playlistRepo.addSongToPlaylist(favPlaylistId, songId);
    }

    public boolean removeFavorite(Long userId, Add_RemoveToPlaylistPayload payload) {
        validateUser(userId);
        Long favPlaylistId = playlistRepo.getOrCreateFavoritePlaylist(userId);
        return playlistRepo.removeSongFromPlaylist(favPlaylistId, resolveSongIdToAdd(payload));
    }

    private void validateUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Người dùng chưa đăng nhập");
        }
    }

    private void checkOwnership(Long userId, Long playlistId) {
        if (playlistId == null || playlistId <= 0) {
            throw new IllegalArgumentException("ID playlist không hợp lệ");
        }
        if (playlistRepo.findById(playlistId, userId).isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy playlist hoặc bạn không phải chủ sở hữu");
        }
    }

    private Long resolveSongIdToAdd(Add_RemoveToPlaylistPayload payload) {
        if (payload == null || payload.song() == null)
            throw new IllegalArgumentException("Thông tin bài hát không hợp lệ");
        String externalId = payload.song().externalId();
        if (externalId != null && !externalId.isBlank())
            return searchService.ensureSong(externalId);
        return extractSongId(payload);
    }

    public static Long extractSongId(Add_RemoveToPlaylistPayload payload) {
        if (payload == null || payload.song() == null || payload.song().songID() == null) {
            throw new IllegalArgumentException("Thông tin bài hát không hợp lệ");
        }
        return payload.song().songID();
    }
}
