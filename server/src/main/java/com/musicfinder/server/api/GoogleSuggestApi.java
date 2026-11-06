package com.musicfinder.server.api;

import org.json.JSONArray;
import org.jsoup.Jsoup;

import com.mysql.cj.xdevapi.JsonArray;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class GoogleSuggestApi {
    private static final String BASE_URL = "https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&q=";

    public static List<String> suggest(String keyword) {
        List<String> suggestions = new ArrayList<>();
        if (keyword == null || keyword.isBlank()) {
            return suggestions;
        }
        try {
            String url = BASE_URL + URLEncoder.encode(keyword.trim(), StandardCharsets.UTF_8);
            String json = Jsoup.connect(url)
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .timeout(3000)
                    .execute()
                    .body();
            JSONArray root = new JSONArray(json);
            if (root.length() > 1) {
                JSONArray items = root.getJSONArray(1);
                for (int i = 0; i < items.length() && i < 7; i++) {
                    suggestions.add(items.getString(i));
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi lấy Google Suggest: " + e.getMessage());
        }
        return suggestions;
    }
}
