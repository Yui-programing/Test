package com.musicfinder.shared.response.payload;

public record GetProfileData(UserData user, long totalSearches, long favoriteCount, long playlistCount) {
}
