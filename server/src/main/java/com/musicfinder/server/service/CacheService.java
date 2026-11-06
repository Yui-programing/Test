package com.musicfinder.server.service;

import java.time.LocalDateTime;
import java.util.Optional;

import com.musicfinder.server.util.DataException;
import com.musicfinder.server.model.SearchCache;
import com.musicfinder.server.repository.SearchCacheRepository;
import com.musicfinder.server.util.TextNormalizer;
import com.musicfinder.shared.response.payload.SearchArtistData;
import com.musicfinder.shared.response.payload.SearchSongData;
import com.musicfinder.shared.response.payload.SongData;
import com.musicfinder.shared.util.Json;

public class CacheService {
    private final SearchCacheRepository cacheRepo;

    public CacheService() {
        this(new SearchCacheRepository());
    }

    public CacheService(SearchCacheRepository cacheRepo) {
        this.cacheRepo = cacheRepo;
    }

    public String createKey(String s, String type) {
        return type + ":" + TextNormalizer.normalize(s);
    }

    private String getJson(String s, String type) {
        return readJson(s, type, false);
    }

    private String readJson(String s, String type, boolean evenIfExpired) {
        try {
            String key = createKey(s, type);
            Optional<SearchCache> found = evenIfExpired
                    ? cacheRepo.findByCacheKeyEvenIfExpired(key)
                    : cacheRepo.findByCacheKey(key);
            if (found.isEmpty()) {
                return null;
            }

            SearchCache cache = found.get();
            if (!evenIfExpired && (cache.getExpiresAt() == null
                    || !cache.getExpiresAt().isAfter(LocalDateTime.now()))) {
                return null;
            }
            return cache.getResponseJson();
        } catch (DataException e) {
            System.out.println("Không đọc được cache: " + e.getMessage());
            return null;
        }
    }

    public SearchSongData getSongs(String s) {
        return parseSongs(getJson(s, "SONG"));
    }

    public SearchSongData getSongsEvenIfExpired(String s) {
        return parseSongs(readJson(s, "SONG", true));
    }

    private SearchSongData parseSongs(String json) {
        if (json == null) {
            return null;
        }
        try {
            SearchSongData data = Json.fromJson(json, SearchSongData.class);
            if (data == null || data.songs() == null) {
                return null;
            }
            for (SongData song : data.songs()) {
                if (song == null || (song.songID() == null && song.externalId() == null)) {
                    return null;
                }
            }
            return data;
        } catch (IllegalArgumentException e) {
            System.out.println("Cache bài hát bị lỗi");
            return null;
        }
    }

    public SearchArtistData getArtists(String s) {
        String json = getJson(s, "ARTIST");
        if (json == null) {
            return null;
        }

        try {
            SearchArtistData data = Json.fromJson(json, SearchArtistData.class);
            if (data == null || data.artists() == null) {
                return null;
            }
            for (int i = 0; i < data.artists().size(); i++) {
                if (data.artists().get(i) == null || data.artists().get(i).artistID() == null) {
                    return null;
                }
            }
            return data;
        } catch (IllegalArgumentException e) {
            System.out.println("Cache ca sĩ bị lỗi");
            return null;
        }
    }

    public void saveSongs(String s, SearchSongData data, int seconds) {
        save(s, "SONG", data, seconds);
    }

    public void saveArtists(String s, SearchArtistData data, int seconds) {
        save(s, "ARTIST", data, seconds);
    }

    public <T> T getValue(String s, String type, Class<T> cls) {
        String json = getJson(s, type);
        if (json == null)
            return null;
        try {
            return Json.fromJson(json, cls);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public <T> T getValueEvenIfExpired(String s, String type, Class<T> cls) {
        String json = readJson(s, type, true);
        if (json == null)
            return null;
        try {
            return Json.fromJson(json, cls);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void putValue(String s, String type, Object data, int seconds) {
        save(s, type, data, seconds);
    }

    private void save(String s, String type, Object data, int seconds) {
        if (seconds <= 0) {
            throw new IllegalArgumentException("Thời gian cache phải lớn hơn 0");
        }
        try {
            SearchCache cache = SearchCache.builder()
                    .cacheKey(createKey(s, type))
                    .keyword(s.trim())
                    .normalizedKeyword(TextNormalizer.normalize(s))
                    .searchType(type)
                    .responseJson(Json.toJson(data))
                    .expiresAt(LocalDateTime.now().plusSeconds(seconds))
                    .build();

            cacheRepo.save(cache);
        } catch (DataException | IllegalArgumentException e) {
            System.out.println("Không lưu được cache: " + e.getMessage());
        }
    }
}