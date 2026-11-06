package com.musicfinder.shared.request.payload;

public record Create_UpdateToPlaylistPayload(Long playlistId, String name, String description) {
    public Create_UpdateToPlaylistPayload(String name, String description) {
        this(null, name, description);
    }
}