package com.musicfinder.shared.response.payload;

import java.util.List;

public record SongData(Long songID, String title, String composerList, List<String> artistNames,
        Integer durations_seconds,
        String audio_url, String video_url, String thumbnail_url, Long searchCount, String preview_url,
        String externalId) {
}
