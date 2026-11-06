package com.musicfinder.server.service;

import java.util.ArrayList;
import java.util.List;

import com.musicfinder.server.model.Artists;
import com.musicfinder.server.model.Songs;
import com.musicfinder.server.repository.ArtistRepository;
import com.musicfinder.server.repository.SongRepository;
import com.musicfinder.server.util.DtoMapper;
import com.musicfinder.shared.response.payload.ArtistData;
import com.musicfinder.shared.response.payload.SearchArtistData;
import com.musicfinder.shared.response.payload.SearchSongData;
import com.musicfinder.shared.response.payload.SongData;

// Thống kê: top bài hát / top ca sĩ được tìm kiếm nhiều nhất (theo cột search_count)
public class StatisticsService {
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;
    private final SongRepository songRepo = new SongRepository();
    private final ArtistRepository artistRepo = new ArtistRepository();

    // GET_TOP_SONGS
    public SearchSongData getTopSongs(int limit) {
        int safeLimit = normalizeLimit(limit);

        // Lấy top bài hát từ DB
        List<Songs> songs = songRepo.findTopSongs(safeLimit);

        // Đổi từng bài sang SongData để gửi cho Client
        List<SongData> result = new ArrayList<>();
        for (Songs song : songs) {
            List<String> artistNames = getArtistNames(song.getSongId());
            result.add(DtoMapper.toSongData(song, artistNames));
        }
        return new SearchSongData(result);
    }

    // GET_TOP_ARTISTS
    public SearchArtistData getTopArtists(int limit) {
        int safeLimit = normalizeLimit(limit);

        List<Artists> artists = artistRepo.findTopArtists(safeLimit);

        List<ArtistData> result = new ArrayList<>();
        for (Artists artist : artists) {
            result.add(DtoMapper.toArtistData(artist));
        }
        return new SearchArtistData(result);
    }

    private static int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        if (limit > MAX_LIMIT) {
            return MAX_LIMIT;
        }
        return limit;
    }

    // Lấy tên các ca sĩ của 1 bài hát (JOIN SONG_ARTISTS)
    private List<String> getArtistNames(Long songId) {
        List<String> names = new ArrayList<>();
        for (Artists artist : artistRepo.findAllArtistByASongId(songId)) {
            names.add(artist.getArtistName());
        }
        return names;
    }
}