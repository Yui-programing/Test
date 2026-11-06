package com.musicfinder.shared.request.payload;

public record LoginPayload(String usernameOrEmail, String password) {
}