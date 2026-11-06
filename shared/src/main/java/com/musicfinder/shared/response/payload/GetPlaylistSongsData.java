package com.musicfinder.shared.response.payload;

import java.util.List;

public record GetPlaylistSongsData(List<SongData> songs) {
}
