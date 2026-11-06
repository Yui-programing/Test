-- =============================================================================
-- schema.sql
-- PostgreSQL schema cho ứng dụng nghe nhạc (java-tcp-music-finder)
-- Dựa trên thiết kế ERD trong database/README.md
-- =============================================================================



-- Thứ tự tạo bảng:
--   1. USERS, ARTISTS, SONGS          (không phụ thuộc FK nào)
--   2. SONG_ARTISTS                   (FK → SONGS, ARTISTS)
--   3. PLAYLISTS                      (FK → USERS)
--   4. PLAYLIST_SONGS                 (FK → PLAYLISTS, SONGS)
--   5. SEARCH_HISTORY                 (FK → USERS)
--   6. SEARCH_CACHE, (độc lập, không có FK)

-- =============================================================================
-- 1. USERS
-- Lưu thông tin tài khoản và thông tin cơ bản của người dùng.
-- =============================================================================
CREATE TABLE IF NOT EXISTS USERS (
    user_id       BIGSERIAL    PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(100),
    email         VARCHAR(150) UNIQUE,
    avatar_url    TEXT,
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- 2. ARTISTS
-- Lưu thông tin nghệ sĩ. external_id cho phép liên kết với nguồn API ngoài.
-- =============================================================================
CREATE TABLE IF NOT EXISTS ARTISTS (
    artist_id       BIGSERIAL    PRIMARY KEY,
    external_id     VARCHAR(100) UNIQUE,
    name            VARCHAR(150) NOT NULL,
    normalized_name VARCHAR(150),
    birth_date      DATE,
    hometown        VARCHAR(255),
    biography       TEXT,
    image_url       TEXT,
    search_count    BIGINT    DEFAULT 0 CHECK (search_count >= 0),
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- 3. SONGS
-- Lưu thông tin các bài hát. external_id cho phép liên kết với nguồn API ngoài.
-- =============================================================================
CREATE TABLE IF NOT EXISTS SONGS (
    song_id          BIGSERIAL    PRIMARY KEY,
    external_id      VARCHAR(100) UNIQUE,
    title            VARCHAR(255) NOT NULL,
    normalized_title VARCHAR(255),
    composer         VARCHAR(255),
    duration_seconds INTEGER      CHECK (duration_seconds >= 0),
    audio_url        TEXT,
    video_url        TEXT,
    preview_url      TEXT,
    thumbnail_url    TEXT,
    search_count     BIGINT     DEFAULT 0 CHECK (search_count >= 0),
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- 4. SONG_ARTISTS
-- Bảng trung gian Many-to-Many giữa SONGS và ARTISTS.
-- Composite PK (song_id, artist_id) đảm bảo không trùng cặp.
-- =============================================================================
CREATE TABLE IF NOT EXISTS SONG_ARTISTS (
    song_id   BIGINT NOT NULL,
    artist_id BIGINT NOT NULL,
    PRIMARY KEY (song_id, artist_id),
    CONSTRAINT fk_song_artists_song   FOREIGN KEY (song_id)   REFERENCES SONGS(song_id) ON DELETE CASCADE,
    CONSTRAINT fk_song_artists_artist FOREIGN KEY (artist_id) REFERENCES ARTISTS(artist_id) ON DELETE CASCADE
);

-- =============================================================================
-- 5. PLAYLISTS
-- Lưu các playlist do người dùng tạo.
-- FK user_id → USERS.user_id (1 user : N playlists).
-- =============================================================================
CREATE TABLE IF NOT EXISTS PLAYLISTS (
    playlist_id BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    type VARCHAR(20) NOT NULL DEFAULT 'NORMAL' CHECK (type IN ('NORMAL', 'FAVORITES')),
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_playlists_user FOREIGN KEY (user_id) REFERENCES USERS(user_id) ON DELETE CASCADE
);

-- =============================================================================
-- 6. PLAYLIST_SONGS
-- Bảng trung gian Many-to-Many giữa PLAYLISTS và SONGS.
-- Lưu thêm vị trí bài hát (position) và thời điểm thêm (added_at).
-- =============================================================================
CREATE TABLE IF NOT EXISTS PLAYLIST_SONGS (
    playlist_id BIGINT    NOT NULL,
    song_id     BIGINT    NOT NULL,
    position    INTEGER   CHECK (position >= 0),
    added_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (playlist_id, song_id),
    CONSTRAINT fk_playlist_songs_playlist FOREIGN KEY (playlist_id) REFERENCES PLAYLISTS(playlist_id) ON DELETE CASCADE,
    CONSTRAINT fk_playlist_songs_song     FOREIGN KEY (song_id)     REFERENCES SONGS(song_id) ON DELETE CASCADE
);

-- =============================================================================
-- 7. SEARCH_HISTORY
-- Lưu lịch sử tìm kiếm của người dùng.
-- FK user_id → USERS.user_id (1 user : N lịch sử).
-- =============================================================================
CREATE TABLE IF NOT EXISTS SEARCH_HISTORY (
    history_id         BIGSERIAL    PRIMARY KEY,
    user_id            BIGINT,
    keyword            VARCHAR(255) NOT NULL,
    normalized_keyword VARCHAR(255),
    search_type        VARCHAR(20),
    result_count       INTEGER      CHECK (result_count >= 0),
    searched_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_search_history_user FOREIGN KEY (user_id) REFERENCES USERS(user_id) ON DELETE CASCADE
);

-- =============================================================================
-- 8. SEARCH_CACHE
-- Cache kết quả tìm kiếm nhằm hạn chế truy vấn lặp lại.
-- response_json dùng JSONB để lưu trực tiếp kết quả tìm kiếm.
-- Không có FK tới bảng khác theo thiết kế.
-- =============================================================================
CREATE TABLE IF NOT EXISTS SEARCH_CACHE (
    cache_id           BIGSERIAL    PRIMARY KEY,
    cache_key          VARCHAR(300) NOT NULL UNIQUE,
    keyword            VARCHAR(255) NOT NULL,
    normalized_keyword VARCHAR(255) NOT NULL,
    search_type        VARCHAR(20)  NOT NULL,
    response_json      JSONB        NOT NULL,
    created_at         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    expires_at         TIMESTAMP    NOT NULL
);

-- =============================================================================
-- INDEXES
-- Tạo index cho các FK và các trường thường xuyên tìm kiếm.
-- =============================================================================

-- USERS
CREATE INDEX IF NOT EXISTS idx_users_username ON USERS(username);
CREATE INDEX IF NOT EXISTS idx_users_email    ON USERS(email);

-- ARTISTS
CREATE INDEX IF NOT EXISTS idx_artists_normalized_name ON ARTISTS(normalized_name);

-- SONGS
CREATE INDEX IF NOT EXISTS idx_songs_normalized_title ON SONGS(normalized_title);

-- SONG_ARTISTS (PK đã cover song_id; thêm index riêng cho artist_id phục vụ lookup ngược)
CREATE INDEX IF NOT EXISTS idx_song_artists_artist_id ON SONG_ARTISTS(artist_id);

-- PLAYLISTS
CREATE INDEX IF NOT EXISTS idx_playlists_user_id ON PLAYLISTS(user_id);

-- Mỗi user chỉ có tối đa một playlist FAVORITES
CREATE UNIQUE INDEX IF NOT EXISTS ux_playlists_user_favorites
ON PLAYLISTS(user_id)
WHERE type = 'FAVORITES';

-- PLAYLIST_SONGS (PK đã cover playlist_id; thêm index riêng cho song_id phục vụ lookup ngược)
CREATE INDEX IF NOT EXISTS idx_playlist_songs_song_id ON PLAYLIST_SONGS(song_id);

-- SEARCH_HISTORY
CREATE INDEX IF NOT EXISTS idx_search_history_user_id            ON SEARCH_HISTORY(user_id);
CREATE INDEX IF NOT EXISTS idx_search_history_keyword            ON SEARCH_HISTORY(keyword);
CREATE INDEX IF NOT EXISTS idx_search_history_normalized_keyword ON SEARCH_HISTORY(normalized_keyword);

-- SEARCH_CACHE
CREATE INDEX IF NOT EXISTS idx_search_cache_cache_key          ON SEARCH_CACHE(cache_key);
CREATE INDEX IF NOT EXISTS idx_search_cache_expires_at         ON SEARCH_CACHE(expires_at);
CREATE INDEX IF NOT EXISTS idx_search_cache_normalized_keyword ON SEARCH_CACHE(normalized_keyword);



-- =============================================================================
-- SEED DATA (tối thiểu, chỉ phục vụ test / demo)
-- =============================================================================

-- Users mẫu (password_hash là bcrypt placeholder của "abc123")
INSERT INTO USERS (username, password_hash, full_name, email) VALUES
    ('alice', '$2a$12$/vlsaZBqCSU4QtlaI/LqiO97.0afx0r3zr1hPWRpBb7bRsfDf3DKe', 'Alice Nguyen', 'alice@example.com'),
    ('bob',   '$2a$12$/vlsaZBqCSU4QtlaI/LqiO97.0afx0r3zr1hPWRpBb7bRsfDf3DKe', 'Bob Tran',    'bob@example.com'),
    ('dong',   '$2a$12$/vlsaZBqCSU4QtlaI/LqiO97.0afx0r3zr1hPWRpBb7bRsfDf3DKe', 'Dong Nguyen',    'Dong@example.com')
ON CONFLICT DO NOTHING;

-- Artists mẫu
INSERT INTO ARTISTS (external_id, name, normalized_name) VALUES
    ('ext-artist-001', 'Son Tung M-TP', 'son tung m-tp'),
    ('ext-artist-002', 'My Tam',        'my tam')
ON CONFLICT DO NOTHING;

-- Songs mẫu
INSERT INTO SONGS (external_id, title, normalized_title, duration_seconds) VALUES
    ('ext-song-001', 'Lac Troi',    'lac troi',    237),
    ('ext-song-002', 'Dung Hoi Em', 'dung hoi em', 268)
ON CONFLICT DO NOTHING;

-- Song – Artist liên kết
INSERT INTO SONG_ARTISTS (song_id, artist_id) VALUES
    (1, 1),
    (2, 2)
ON CONFLICT DO NOTHING;

-- Playlist của alice
INSERT INTO PLAYLISTS (user_id, name, description) VALUES
    (1, 'Nhac buoi sang', 'Nhung bai nhac thu gian buoi sang')
ON CONFLICT DO NOTHING;

-- Bài hát trong playlist
INSERT INTO PLAYLIST_SONGS (playlist_id, song_id, position) VALUES
    (1, 1, 1),
    (1, 2, 2)
ON CONFLICT DO NOTHING;

-- Lịch sử tìm kiếm của alice
INSERT INTO SEARCH_HISTORY (user_id, keyword, normalized_keyword, search_type, result_count) VALUES
    (1, 'Son Tung', 'son tung', 'SONG', 5),
    (1, 'My Tam',   'my tam',   'SONG', 3)
ON CONFLICT DO NOTHING;

-- Search cache mẫu
INSERT INTO SEARCH_CACHE (cache_key, keyword, normalized_keyword, search_type, response_json, expires_at) VALUES
    (
        'SONG:son_tung',
        'Son Tung',
        'son tung',
        'SONG',
        '{"results": [{"id": "ext-song-001", "title": "Lac Troi"}]}',
        CURRENT_TIMESTAMP + INTERVAL '1 hour'
    )
ON CONFLICT DO NOTHING;
