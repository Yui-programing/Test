package com.musicfinder.server.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import com.musicfinder.server.api.YtDlpApi;
import com.musicfinder.server.model.Artists;
import com.musicfinder.server.model.Songs;
import com.musicfinder.server.network.AudioStreamServer;
import com.musicfinder.server.repository.ArtistRepository;
import com.musicfinder.server.repository.SongRepository;
import com.musicfinder.server.util.DtoMapper;
import com.musicfinder.server.util.FuzzyMatcher;
import com.musicfinder.server.util.TextNormalizer;
import com.musicfinder.shared.response.payload.SearchSongData;
import com.musicfinder.shared.response.payload.SongData;

public class SearchService {
    private final YtDlpApi apiService;
    private final CacheService cache;
    private final SongRepository songRepo;
    private final ArtistRepository artistRepo;
    private final HistoryService historyService;

    private static final List<String> BAD_WORDS = List.of("karaoke", "reaction", "mix");
    private static final Pattern BAD_TITLE = Pattern.compile(
            "\\b(" + String.join("|", BAD_WORDS) + ")\\b", Pattern.CASE_INSENSITIVE);
    private static final int MIN_SECONDS = 60;
    private static final int MAX_SECONDS = 600;
    private static final int LIST_CACHE_SECONDS = 3600; // 1 giờ

    private final Map<String, Object> locks = new ConcurrentHashMap<>();

    public SearchService() {
        this(new YtDlpApi());
    }

    public SearchService(YtDlpApi apiService) {
        this(apiService, new CacheService());
    }

    public SearchService(YtDlpApi apiService, CacheService cache) {
        this(apiService, cache, new SongRepository(), new ArtistRepository(), new HistoryService());
    }

    public SearchService(YtDlpApi apiService, CacheService cache, SongRepository songRepo,
            ArtistRepository artistRepo, HistoryService historyService) {
        this.apiService = apiService;
        this.cache = cache;
        this.songRepo = songRepo;
        this.artistRepo = artistRepo;
        this.historyService = historyService;
    }

    public SearchSongData searchSongs(String keyword, Long userId) {
        if (keyword == null || keyword.isBlank() || keyword.trim().length() > 255)
            throw new IllegalArgumentException("Từ khóa không hợp lệ");
        keyword = keyword.trim();
        if (TextNormalizer.normalize(keyword).isEmpty())
            throw new IllegalArgumentException("Từ khóa không hợp lệ");

        SearchSongData data = cache.getSongs(keyword);
        if (data == null)
            data = fetchWithLock(keyword);

        if (userId != null && userId > 0)
            historyService.recordSearch(userId, keyword, "SONG", data.songs().size());
        return data;
    }

    private SearchSongData fetchWithLock(String keyword) {
        Object lock = locks.computeIfAbsent(TextNormalizer.normalize(keyword), k -> new Object());
        synchronized (lock) {
            SearchSongData cached = cache.getSongs(keyword);
            if (cached != null)
                return cached;

            try {
                List<YtDlpApi.RemoteSong> found = apiService.searchSongs(keyword);
                SearchSongData data = buildResult(keyword, found);
                cache.saveSongs(keyword, data, LIST_CACHE_SECONDS);
                return data;
            } catch (RuntimeException e) {
                SearchSongData old = cache.getSongsEvenIfExpired(keyword);
                if (old != null)
                    return old;
                throw e;
            }
        }
    }

    private SearchSongData buildResult(String keyword, List<YtDlpApi.RemoteSong> found) {
        if (found == null)
            throw new IllegalStateException("Không lấy được metadata từ yt-dlp");

        List<SongData> songs = new ArrayList<>();
        for (YtDlpApi.RemoteSong remote : found) {
            if (!isRealSong(remote))
                continue;
            songs.add(toResultSong(remote));
        }

        List<SongData> suggestions = new ArrayList<>();
        if (songs.isEmpty()) {
            FuzzyMatcher fuzzy = new FuzzyMatcher();
            List<Songs> known = songRepo.searchSongs("", 200);
            suggestions = toSongData(fuzzy.suggestSongs(keyword, known));
        }
        return new SearchSongData(songs, suggestions);
    }

    private boolean isRealSong(YtDlpApi.RemoteSong remote) {
        int seconds = remote.durationSeconds();
        if (seconds > 0 && (seconds < MIN_SECONDS || seconds > MAX_SECONDS))
            return false;
        return !BAD_TITLE.matcher(remote.title()).find();
    }

    private SongData remoteToSongData(YtDlpApi.RemoteSong remote) {
        return new SongData(null, remote.title(), null, List.of(remote.artist().name()),
                remote.durationSeconds(), null, remote.storeUrl(), remote.thumbnailUrl(), 0L,
                remote.previewUrl(), remote.externalId());
    }

    public SongData toResultSong(YtDlpApi.RemoteSong remote) {
        cache.putValue(remote.externalId(), "SONG_META", remote, LIST_CACHE_SECONDS);
        return remoteToSongData(remote);
    }

    public synchronized Artists importArtist(YtDlpApi.RemoteArtist remote) {
        Optional<Artists> found = artistRepo.findByExternalId(remote.externalId());
        if (found.isPresent())
            return found.get();
        Artists artist = new Artists();
        artist.setExternalId(remote.externalId());
        artist.setArtistName(remote.name());
        artist.setNormalizedName(TextNormalizer.normalize(remote.name()));
        artist.setSearchCount(0L);
        artist = artistRepo.save(artist);
        if (artist.getArtistId() == null)
            throw new IllegalStateException("DB chưa cấp ID ca sĩ/kênh");
        return artist;
    }

    public synchronized SongData importSong(YtDlpApi.RemoteSong remote) {
        Artists artist = importArtist(remote.artist());
        Optional<Songs> found = songRepo.findByExternalId(remote.externalId());
        Songs song;
        if (found.isPresent()) {
            song = found.get();
        } else {
            song = new Songs();
            song.setExternalId(remote.externalId());
            song.setTitle(remote.title());
            song.setNormalizedTitle(TextNormalizer.normalize(remote.title()));
            song.setDurationSec(remote.durationSeconds());
            song.setVideoUrl(remote.storeUrl());
            song.setThumbnailUrl(remote.thumbnailUrl());
            song.setSearchCount(0L);
            song = songRepo.save(song);
        }
        if (song.getSongId() == null)
            throw new IllegalStateException("DB chưa cấp ID bài hát");
        songRepo.linkArtist(song.getSongId(), artist.getArtistId());
        return toSongData(song);
    }

    public Long ensureSong(String externalId) {
        if (externalId == null || !externalId.matches("youtube:video:[A-Za-z0-9_-]{11}"))
            throw new IllegalArgumentException("Mã bài hát không hợp lệ");

        Optional<Songs> found = songRepo.findByExternalId(externalId);
        if (found.isPresent())
            return found.get().getSongId();

        YtDlpApi.RemoteSong meta = cache.getValue(externalId, "SONG_META", YtDlpApi.RemoteSong.class);
        if (meta == null || !externalId.equals(meta.externalId()))
            throw new IllegalArgumentException("Kết quả đã hết hạn, vui lòng tìm lại");

        return importSong(meta).songID(); // importSong đã synchronized và kiểm tra trùng lại
    }

    public SongData getSong(Long id) {
        if (id == null || id <= 0)
            throw new IllegalArgumentException("ID bài hát không hợp lệ");
        Optional<Songs> found = songRepo.findByIdSong(id);
        if (found.isEmpty())
            throw new IllegalArgumentException("Không tìm thấy bài hát");
        return toSongData(found.get());
    }

    public SongData detail(Long id, String externalId) {
        if (externalId != null && !externalId.isBlank())
            id = ensureSong(externalId);
        if (id == null || id <= 0)
            throw new IllegalArgumentException("ID bài hát không hợp lệ");

        songRepo.updateSearchCount(id);

        SongData cached = cache.getValue(String.valueOf(id), "SONG_DETAIL", SongData.class);
        if (cached != null && cached.audio_url() != null && !cached.audio_url().isBlank()) {
            String videoId = cached.video_url() != null
                    && cached.video_url().startsWith("https://www.youtube.com/watch?v=")
                            ? cached.video_url().replace("https://www.youtube.com/watch?v=", "")
                            : null;
            File audioDir = new File(System.getProperty("music.audio.directory", "storage/audio"));
            if (videoId != null && new File(audioDir, videoId + ".mp3").isFile()) {
                return cached;
            }
        }

        SongData song = getSong(id);
        if (song.video_url() == null || !song.video_url().startsWith("https://www.youtube.com/watch?v="))
            throw new IllegalArgumentException("Bài hát chưa có nguồn YouTube");
        String videoId = song.video_url().replace("https://www.youtube.com/watch?v=", "");

        if (apiService.downloadAudioToCache(videoId) == null)
            throw new IllegalStateException("Không chuẩn bị được MP3. Xem lỗi yt-dlp/FFmpeg tại server.");

        String audioUrl = AudioStreamServer.getAudioUrl(videoId);
        if (!songRepo.updateAudioUrl(id, audioUrl))
            throw new IllegalStateException("Không cập nhật được link audio vào DB");

        SongData result = new SongData(song.songID(), song.title(), song.composerList(), song.artistNames(),
                song.durations_seconds(), audioUrl, song.video_url(), song.thumbnail_url(),
                song.searchCount(), song.preview_url(), song.externalId());

        // LƯU BÀI HÁT ĐÃ PHÁT VÀO CACHE
        cache.putValue(String.valueOf(id), "SONG_DETAIL", result, 900);

        return result;

    }

    private SongData toSongData(Songs song) {
        List<String> names = new ArrayList<>();
        for (Artists artist : artistRepo.findAllArtistByASongId(song.getSongId()))
            names.add(artist.getArtistName());
        return DtoMapper.toSongData(song, names);
    }

    private List<SongData> toSongData(List<Songs> songs) {
        List<SongData> result = new ArrayList<>();
        for (Songs song : songs)
            result.add(toSongData(song));
        return result;
    }
}
