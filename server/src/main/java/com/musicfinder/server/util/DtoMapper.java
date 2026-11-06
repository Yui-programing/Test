package com.musicfinder.server.util;

import java.util.ArrayList;
import java.util.List;

import com.musicfinder.server.model.*;
import com.musicfinder.server.repository.ArtistRepository;
import com.musicfinder.shared.enums.PlayList_Type;
import com.musicfinder.shared.request.payload.Add_RemoveToPlaylistPayload;
import com.musicfinder.shared.response.payload.*;

// Đổi Entity (model của DB) sang DTO (payload gửi cho Client).
// Dùng chung cho StatisticsService, ArtistService và sau này là SearchService, PlaylistService.
public class DtoMapper {

    public static SongData toSongData(Songs s, List<String> artistNames) {
        return new SongData(
                s.getSongId(),
                s.getTitle(),
                s.getComposer(), // cột composer -> composerList
                artistNames,
                s.getDurationSec(),
                s.getAudioUrl(),
                s.getVideoUrl(),
                s.getThumbnailUrl(),
                s.getSearchCount(),
                s.getPreviewUrl(),
                s.getExternalId());
    }

    public static ArtistData toArtistData(Artists a) {
        return new ArtistData(
                a.getArtistId(),
                a.getArtistName(),
                a.getBirthDate(),
                a.getHomeTown(),
                a.getBio(),
                a.getImageUrl(),
                a.getSearchCount());
    }

    public static UserData toUserData(Users a) {
        return new UserData(
                a.getUserId(),
                a.getUsername(),
                a.getFullName(),
                a.getEmail(),
                a.getAvatarUrl());
    }

    public static List<SongData> toSongDataList(List<Songs> songs, ArtistRepository artistRepo) {
        List<SongData> result = new ArrayList<>();
        for (Songs song : songs) {
            List<String> artistNames = artistRepo.findAllArtistByASongId(song.getSongId())
                    .stream()
                    .map(Artists::getArtistName)
                    .toList();
            result.add(toSongData(song, artistNames));
        }
        return result;
    }

    public static PlayListData toPlayListData(Playlists p) {
        if (p == null)
            return null;
        PlayList_Type type = (p.getType() != null)
                ? PlayList_Type.valueOf(p.getType())
                : PlayList_Type.NORMAL;
        return new PlayListData(
                p.getPlaylistId(),
                p.getName(),
                p.getDescription(),
                type);
    }

}
