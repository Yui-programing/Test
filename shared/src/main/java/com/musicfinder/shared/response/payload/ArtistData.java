package com.musicfinder.shared.response.payload;

import java.time.LocalDate;

public record ArtistData(Long artistID, String name, LocalDate birth_date, String homeTown, String biagraphy,
        String image_url, Long searchCount) {

}
