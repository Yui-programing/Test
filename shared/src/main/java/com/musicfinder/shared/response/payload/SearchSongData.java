package com.musicfinder.shared.response.payload;

import java.util.List;

public record SearchSongData(List<SongData> songs, List<SongData> suggestions) {
    public SearchSongData(List<SongData> songs) {
        this(songs, List.of());
    }
}