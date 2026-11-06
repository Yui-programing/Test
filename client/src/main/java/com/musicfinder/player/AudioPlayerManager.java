package com.musicfinder.player;

import com.musicfinder.network.ClientConnection;
import com.musicfinder.shared.enums.Action;
import com.musicfinder.shared.request.Request;
import com.musicfinder.shared.request.payload.SongPayload;
import com.musicfinder.shared.response.payload.SongData;
import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class AudioPlayerManager {
    private static final AudioPlayerManager INSTANCE = new AudioPlayerManager();

    private SongData currentSong;
    private List<SongData> playlist = new ArrayList<>();
    private int currentIndex = -1;
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private boolean isLoading = false;

    private final List<Consumer<SongData>> songChangeListeners = new ArrayList<>();
    private final List<Consumer<Boolean>> playStateListeners = new ArrayList<>();
    private final List<Consumer<String>> statusMessageListeners = new ArrayList<>();

    private AudioPlayerManager() {
    }

    public static AudioPlayerManager getInstance() {
        return INSTANCE;
    }

    public synchronized SongData getCurrentSong() {
        return currentSong;
    }

    public synchronized boolean isPlaying() {
        return isPlaying;
    }

    public synchronized boolean isLoading() {
        return isLoading;
    }

    public synchronized void playSong(SongData song, List<SongData> newPlaylist) {
        if (song == null) return;

        if (newPlaylist != null) {
            this.playlist = new ArrayList<>(newPlaylist);
            this.currentIndex = this.playlist.indexOf(song);
        } else if (!this.playlist.contains(song)) {
            this.playlist = new ArrayList<>(List.of(song));
            this.currentIndex = 0;
        } else {
            this.currentIndex = this.playlist.indexOf(song);
        }

        this.currentSong = song;
        notifySongChanged(song);

        // Nếu đã có audio_url và hợp lệ -> phát ngay
        if (song.audio_url() != null && !song.audio_url().isBlank()) {
            startMediaPlayback(song.audio_url());
            return;
        }

        // Nếu chưa có audio_url -> gọi VIEW_SONG_DETAIL đến server để stream proxy tải
        notifyStatusMessage("Đang tải bài hát: " + song.title() + "...");
        setLoading(true);

        SongPayload payload = new SongPayload(song.songID(), song.externalId());
        Request req = Request.of(Action.VIEW_SONG_DETAIL, payload);

        try {
            ClientConnection.getInstance().send(req, response -> {
                Platform.runLater(() -> {
                    setLoading(false);
                    if (response.isSuccess() && response.getData() != null) {
                        SongData detailed = response.dataAs(SongData.class);
                        if (detailed != null && detailed.audio_url() != null && !detailed.audio_url().isBlank()) {
                            this.currentSong = detailed;
                            // cập nhật lại trong playlist nếu có
                            if (currentIndex >= 0 && currentIndex < playlist.size()) {
                                playlist.set(currentIndex, detailed);
                            }
                            notifySongChanged(detailed);
                            startMediaPlayback(detailed.audio_url());
                        } else {
                            notifyStatusMessage("Không thể phát bài hát này");
                        }
                    } else {
                        notifyStatusMessage("Lỗi tải bài hát: " + response.getMessage());
                    }
                });
            });
        } catch (IOException e) {
            Platform.runLater(() -> {
                setLoading(false);
                notifyStatusMessage("Lỗi kết nối khi tải bài hát");
            });
        }
    }

    private void startMediaPlayback(String audioUrl) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
                mediaPlayer = null;
            }

            Media media = new Media(audioUrl);
            mediaPlayer = new MediaPlayer(media);

            mediaPlayer.setOnReady(() -> {
                mediaPlayer.play();
                setPlaying(true);
                notifyStatusMessage("Đang phát: " + (currentSong != null ? currentSong.title() : ""));
            });

            mediaPlayer.setOnEndOfMedia(() -> {
                setPlaying(false);
                next();
            });

            mediaPlayer.setOnError(() -> {
                setPlaying(false);
                String err = mediaPlayer.getError() != null ? mediaPlayer.getError().getMessage() : "Unknown error";
                notifyStatusMessage("Lỗi phát audio: " + err);
            });

        } catch (Exception e) {
            setPlaying(false);
            notifyStatusMessage("Không thể phát audio: " + e.getMessage());
        }
    }

    public synchronized void togglePlayPause() {
        if (mediaPlayer == null) {
            if (currentSong != null) {
                playSong(currentSong, playlist);
            }
            return;
        }

        if (isPlaying) {
            mediaPlayer.pause();
            setPlaying(false);
        } else {
            mediaPlayer.play();
            setPlaying(true);
        }
    }

    public synchronized void next() {
        if (playlist == null || playlist.isEmpty()) return;
        int nextIndex = currentIndex + 1;
        if (nextIndex < playlist.size()) {
            currentIndex = nextIndex;
            playSong(playlist.get(currentIndex), playlist);
        } else if (!playlist.isEmpty()) {
            currentIndex = 0;
            playSong(playlist.get(0), playlist);
        }
    }

    public synchronized void previous() {
        if (playlist == null || playlist.isEmpty()) return;
        int prevIndex = currentIndex - 1;
        if (prevIndex >= 0) {
            currentIndex = prevIndex;
            playSong(playlist.get(currentIndex), playlist);
        } else if (!playlist.isEmpty()) {
            currentIndex = playlist.size() - 1;
            playSong(playlist.get(currentIndex), playlist);
        }
    }

    public synchronized void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
        setPlaying(false);
        setLoading(false);
    }

    private void setPlaying(boolean playing) {
        this.isPlaying = playing;
        for (Consumer<Boolean> l : new ArrayList<>(playStateListeners)) {
            try {
                l.accept(playing);
            } catch (Exception ignored) {
            }
        }
    }

    private void setLoading(boolean loading) {
        this.isLoading = loading;
    }

    private void notifySongChanged(SongData song) {
        for (Consumer<SongData> l : new ArrayList<>(songChangeListeners)) {
            try {
                l.accept(song);
            } catch (Exception ignored) {
            }
        }
    }

    private void notifyStatusMessage(String msg) {
        for (Consumer<String> l : new ArrayList<>(statusMessageListeners)) {
            try {
                l.accept(msg);
            } catch (Exception ignored) {
            }
        }
    }

    public synchronized void addSongChangeListener(Consumer<SongData> listener) {
        songChangeListeners.add(listener);
        if (currentSong != null) {
            listener.accept(currentSong);
        }
    }

    public synchronized void addPlayStateListener(Consumer<Boolean> listener) {
        playStateListeners.add(listener);
        listener.accept(isPlaying);
    }

    public synchronized void addStatusMessageListener(Consumer<String> listener) {
        statusMessageListeners.add(listener);
    }
}
