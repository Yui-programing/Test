package com.musicfinder.shared.request.payload;

import com.musicfinder.shared.enums.PlayList_Type;
import com.musicfinder.shared.response.payload.SongData;

public record Add_RemoveToPlaylistPayload(Long playlistId, SongData song, PlayList_Type type) {
}