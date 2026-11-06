package com.musicfinder.server.api;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;


import lombok.extern.slf4j.Slf4j;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
 
@Slf4j 
public class WikipediaApi {
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";
    private static final int TIMEOUT_MS = 3000;

    public record WikiArtist(
            String externalId,
            String name,
            String bio,
            String imageUrl,
            LocalDate birthDate,
            String hometown) {
    }

    public WikiArtist searchArtist(String keyword) {
        if (keyword == null || keyword.isBlank())
            return null;

        try {
            // Tìm tiêu đề chuẩn từ keyword
            String searchUrl = "https://vi.wikipedia.org/w/api.php?action=opensearch&search="
                    + URLEncoder.encode(keyword.trim(), StandardCharsets.UTF_8)
                    + "&limit=1&namespace=0&format=json";

            String searchJson = Jsoup.connect(searchUrl)
                    .ignoreContentType(true)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .execute()
                    .body();

            JSONArray searchArr = new JSONArray(searchJson);
            JSONArray titles = searchArr.optJSONArray(1);
            if (titles == null || titles.isEmpty())
                return null;

            String exactTitle = titles.getString(0);

            // Lấy summary (Bio, Ảnh, Page ID)
            String summaryUrl = "https://vi.wikipedia.org/api/rest_v1/page/summary/"
                    + URLEncoder.encode(exactTitle.replace(" ", "_"), StandardCharsets.UTF_8);

            String summaryJson = Jsoup.connect(summaryUrl)
                    .ignoreContentType(true)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .execute()
                    .body();

            JSONObject root = new JSONObject(summaryJson);
            String title = root.optString("title", exactTitle);
            String extract = root.optString("extract", "");
            long pageId = root.optLong("pageid", 0);

            String imageUrl = null;
            if (root.has("thumbnail")) {
                imageUrl = root.getJSONObject("thumbnail").optString("source", null);
            }

            // Lấy hometown và birthDate từ trang HTML
            PageDetail detail = extractDetailFromPage(exactTitle);

            return new WikiArtist("wiki:page:" + pageId, title, extract, imageUrl, detail.birthDate, detail.hometown);
        } catch (Exception e) {
            log.warn("Lỗi gọi Wikipedia API: {}", e.getMessage());
            return null;
        }
    }

    private record PageDetail(LocalDate birthDate, String hometown) {
    }

    /**
     * Quét trang HTML và bảng Infobox để lấy Ngày sinh và Quê quán
     */
    private PageDetail extractDetailFromPage(String title) {
        String hometown = null;
        String ancestralHome = null;
        LocalDate birthDate = null;
        try {
            String pageUrl = "https://vi.wikipedia.org/wiki/"
                    + URLEncoder.encode(title.replace(" ", "_"), StandardCharsets.UTF_8);

            Document doc = Jsoup.connect(pageUrl)
                    .maxBodySize(500 * 1024)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .get();

            //Quét ngày sinh từ đoạn văn bản giới thiệu đầu trang
            birthDate = extractBirthDate(doc.text());

            //  Quét bảng Infobox
            Element infobox = doc.selectFirst("table.infobox");
            if (infobox != null) {
                for (Element row : infobox.select("tr")) {
                    Element th = row.selectFirst("th");
                    Element td = row.selectFirst("td");
                    if (th == null || td == null)
                        continue;

                    String label = th.text().toLowerCase();

                    //  thẻ <span class="birthplace"> trong hàng Sinh
                    Element birthplace = td.selectFirst("span.birthplace");
                    if (birthplace != null && hometown == null) {
                        hometown = birthplace.text().replaceAll("\\[\\d+\\]", "").trim();
                    }

                    //  Hàng nhãn "nơi sinh" hoặc "quê quán"
                    if (hometown == null && (label.contains("nơi sinh") || label.contains("quê quán"))) {
                        hometown = td.text().replaceAll("\\[\\d+\\]", "").trim();
                    }

                    //  Nhãn "nguyên quán"
                    if (ancestralHome == null && label.contains("nguyên quán")) {
                        ancestralHome = td.text().replaceAll("\\[\\d+\\]", "").trim();
                    }

                    // Nếu chưa có ngày sinh, quét thêm trong hàng "Sinh" của Infobox 
                    if (birthDate == null && label.contains("sinh")) {
                        birthDate = extractBirthDateFromInfobox(td.text());
                    }
                }
            }

            if (hometown == null) {
                hometown = ancestralHome;
            }
        } catch (Exception ignored) {
        }
        return new PageDetail(birthDate, hometown);
    }

    private LocalDate extractBirthDate(String text) {
        if (text == null || text.isBlank())
            return null;
        Pattern p = Pattern.compile("sinh(?: ngày)?\\s+(\\d{1,2})\\s+tháng\\s+(\\d{1,2})\\s+năm\\s+(\\d{4})");
        Matcher m = p.matcher(text);
        if (m.find()) {
            try {
                return LocalDate.of(Integer.parseInt(m.group(3)),
                        Integer.parseInt(m.group(2)),
                        Integer.parseInt(m.group(1)));
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private LocalDate extractBirthDateFromInfobox(String text) {
        if (text == null || text.isBlank())
            return null;
        Pattern p = Pattern.compile("(?:sinh(?: ngày)?\\s+)?(\\d{1,2})\\s+tháng\\s+(\\d{1,2})(?:\\s+năm|\\s*,)\\s*(\\d{4})");
        Matcher m = p.matcher(text);
        if (m.find()) {
            try {
                return LocalDate.of(Integer.parseInt(m.group(3)),
                        Integer.parseInt(m.group(2)),
                        Integer.parseInt(m.group(1)));
            } catch (Exception ignored) {
            }
        }
        return null;
    }
 

}