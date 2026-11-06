package com.musicfinder.server.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data // Tự sinh: Getter, Setter, toString, equals, hashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Artists {
    private Long artistId;
    private String externalId;
    private String artistName;
    private String normalizedName;
    private LocalDate birthDate;
    private String homeTown;
    private String bio;
    private String imageUrl;
    private Long searchCount;
    private LocalDateTime createdAt;
    private LocalDateTime updateAt;
}
