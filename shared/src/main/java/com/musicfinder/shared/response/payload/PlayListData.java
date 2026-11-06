package com.musicfinder.shared.response.payload;

import com.musicfinder.shared.enums.PlayList_Type;

public record PlayListData(Long playlistID, String name, String description, PlayList_Type type) {

}
