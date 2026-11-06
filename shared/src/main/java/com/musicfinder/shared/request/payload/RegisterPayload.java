package com.musicfinder.shared.request.payload;

public record RegisterPayload(
                String username,
                String password,
                String fullName,
                String email) {
}