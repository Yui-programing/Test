package com.musicfinder.navigation;

public enum Screen {
    LOGIN("/com/musicfinder/login-view.fxml", "Music Finder - Login", "Login", 1000, 680),
    HOME("/com/musicfinder/home-content.fxml", "Music Finder - Home", "Discover", 1100, 720),
    SEARCH("/com/musicfinder/search-content.fxml", "Music Finder - Search", "Search", 1100, 720),
    ARTISTS("/com/musicfinder/artists-content.fxml", "Music Finder - Artists", "Artists", 1100, 720),
    PLAYLISTS("/com/musicfinder/my-playlists-content.fxml", "Music Finder - My Playlists", "My Playlists", 1100, 720),
    FAVORITES("/com/musicfinder/favorites-content.fxml", "Music Finder - Favorites", "Favorites", 1100, 720),
    SEARCH_HISTORY("/com/musicfinder/search-history-content.fxml", "Music Finder - Search History", "Search History",
            1100, 720),
    TRENDING("/com/musicfinder/trending-content.fxml", "Music Finder - Trending", "Trending", 1100, 720),
    PROFILE("/com/musicfinder/profile-content.fxml", "Music Finder - Profile", "Your Profile", 1100, 720);

    private final String fxml;
    private final String title;
    private final String headerTitle;
    private final double minWidth;
    private final double minHeight;

    Screen(String fxml, String title, String headerTitle, double minWidth, double minHeight) {
        this.fxml = fxml;
        this.title = title;
        this.headerTitle = headerTitle;
        this.minWidth = minWidth;
        this.minHeight = minHeight;
    }

    public String getFxml() {
        return fxml;
    }

    public String getTitle() {
        return title;
    }

    public String getHeaderTitle() {
        return headerTitle;
    }

    public double getMinWidth() {
        return minWidth;
    }

    public double getMinHeight() {
        return minHeight;
    }

    public boolean isLogin() {
        return this == LOGIN;
    }
}
