package com.musicfinder.server.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.text.similarity.LevenshteinDistance;

import com.musicfinder.server.model.Artists;
import com.musicfinder.server.model.Songs;

public class FuzzyMatcher {
    private static final double MIN_SCORE = 0.65;
    private static final int MAX_SUGGESTIONS = 5;
    private static final int MIN_KEYWORD_LENGTH = 3;

    private final LevenshteinDistance distance = new LevenshteinDistance();

    public double score(String s1, String s2) {
        String a = TextNormalizer.normalize(s1);
        String b = TextNormalizer.normalize(s2);

        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }

        int n = distance.apply(a, b);
        int maxLength = Math.max(a.length(), b.length());

        double difference = (double) n / maxLength;
        double result = 1.0 - difference;
        return result;
    }

    public List<Songs> suggestSongs(String s, List<Songs> songs) {
        List<String> titles = new ArrayList<>();
        for (Songs song : songs) {
            titles.add(song.getTitle());
        }

        List<Songs> result = new ArrayList<>();
        for (int index : topMatches(s, titles)) {
            result.add(songs.get(index));
        }
        return result;
    }

    public List<Artists> suggestArtists(String s, List<Artists> artists) {
        List<String> names = new ArrayList<>();
        for (Artists artist : artists) {
            names.add(artist.getArtistName());
        }

        List<Artists> result = new ArrayList<>();
        for (int index : topMatches(s, names)) {
            result.add(artists.get(index));
        }
        return result;
    }

    private List<Integer> topMatches(String keyword, List<String> names) {
        List<Integer> result = new ArrayList<>();
        if (TextNormalizer.normalize(keyword).length() < MIN_KEYWORD_LENGTH) {
            return result;
        }

        boolean[] used = new boolean[names.size()]; // tên nào đã chọn rồi thì bỏ qua

        for (int count = 0; count < MAX_SUGGESTIONS; count++) {
            int best = -1;
            double bestScore = MIN_SCORE;

            for (int i = 0; i < names.size(); i++) {
                if (used[i]) {
                    continue;
                }
                double value = score(keyword, names.get(i));
                if (value >= MIN_SCORE && (best == -1 || value > bestScore)) {
                    best = i;
                    bestScore = value;
                }
            }

            if (best == -1) {
                break;
            }

            used[best] = true;
            result.add(best);
        }

        return result;
    }
}