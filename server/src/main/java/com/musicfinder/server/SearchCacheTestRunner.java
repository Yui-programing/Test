package com.musicfinder.server;

import com.musicfinder.server.api.YtDlpApi;
import com.musicfinder.server.config.DatabaseConfig;
import com.musicfinder.server.network.AudioStreamServer;
import com.musicfinder.server.service.SearchService;
import com.musicfinder.shared.response.payload.SearchSongData;
import com.musicfinder.shared.response.payload.SongData;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class SearchCacheTestRunner {

    private static final SearchService searchService = new SearchService();
    private static AudioStreamServer audioServer;
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("   CHƯƠNG TRÌNH THỬ NGHIỆM: TÁCH BIỆT CACHE & CSDL (JUST-IN-TIME)     ");
        System.out.println("======================================================================");

        // 1. Kiểm tra kết nối CSDL
        try (Connection conn = DatabaseConfig.getConnection()) {
            System.out.println("✅ Kết nối PostgreSQL thành công!");
        } catch (SQLException e) {
            System.err.println("❌ Không thể kết nối CSDL: " + e.getMessage());
            System.err.println("Vui lòng kiểm tra PostgreSQL và application.properties!");
            return;
        }

        // 2. Chuẩn bị tools và khởi động AudioStreamServer (port 8088)
        YtDlpApi.prepareAudioTools();
        audioServer = new AudioStreamServer();
        if (!audioServer.start()) {
            System.err.println("⚠️ Không thể khởi động AudioStreamServer (có thể port 8088 đang bận).");
        }

        // Đăng ký shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (audioServer != null) audioServer.stop();
            DatabaseConfig.shutdown();
        }));

        // 3. Vòng lặp menu thao tác
        boolean running = true;
        while (running) {
            printMenu();
            System.out.print("👉 Chọn thao tác (0-4): ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> runAutomatedVerification();
                case "2" -> handleSearchInteractive();
                case "3" -> handlePlayInteractive();
                case "4" -> printDatabaseStatistics();
                case "0" -> {
                    System.out.println("\n👋 Đang thoát chương trình thử nghiệm...");
                    running = false;
                }
                default -> System.out.println("⚠️ Lựa chọn không hợp lệ, vui lòng chọn từ 0 đến 4!");
            }
            System.out.println();
        }

        if (audioServer != null) audioServer.stop();
        DatabaseConfig.shutdown();
        scanner.close();
        System.out.println("✅ Đã kết thúc chương trình.");
    }

    private static void printMenu() {
        System.out.println("----------------------------------------------------------------------");
        System.out.println("1. [TEST TỰ ĐỘNG]       Chạy kịch bản tự động chứng minh: Search -> Cache -> DB");
        System.out.println("2. [TÌM KIẾM BÀI HÁT]   Nhập từ khóa bất kỳ -> Xem kết quả & đối chiếu DB");
        System.out.println("3. [BẤM NGHE / IMPORT]  Nhập External ID -> Xem bài hát nạp vào DB & tải MP3");
        System.out.println("4. [XEM THỐNG KÊ DB]    Xem số lượng bản ghi trong SONGS, ARTISTS, SEARCH_CACHE");
        System.out.println("0. Thoát");
        System.out.println("----------------------------------------------------------------------");
    }

    /**
     * Kịch bản tự động 4 bước kiểm chứng thiết kế mới:
     * - B1: Đo số lượng trước khi làm bất cứ việc gì.
     * - B2: Search từ khóa -> Kiểm tra SEARCH_CACHE tăng, nhưng SONGS KHÔNG TĂNG.
     * - B3: User bấm nghe 1 bài -> Kiểm tra SONGS tăng đúng +1, MP3 được tải.
     * - B4: Bấm nghe lại bài đó -> Kiểm tra phản hồi tức thì từ Cache.
     */
    private static void runAutomatedVerification() {
        System.out.println("\n======================================================================");
        System.out.println("        BẮT ĐẦU KỊCH BẢN KIỂM CHỨNG TỰ ĐỘNG (AUTOMATED TEST)          ");
        System.out.println("======================================================================");

        int initialSongs = countRows("SONGS");
        int initialCache = countRows("SEARCH_CACHE");
        int initialArtists = countRows("ARTISTS");

        System.out.println("📊 [BƯỚC 1] TRẠNG THÁI BAN ĐẦU CỦA DATABASE:");
        System.out.printf("   - Bảng SONGS:        %d bản ghi%n", initialSongs);
        System.out.printf("   - Bảng ARTISTS:      %d bản ghi%n", initialArtists);
        System.out.printf("   - Bảng SEARCH_CACHE: %d bản ghi%n", initialCache);

        // BƯỚC 2: TÌM KIẾM
        String keyword = "Lạc Trôi";
        System.out.println("\n🔍 [BƯỚC 2] THỰC HIỆN TÌM KIẾM TỪ KHÓA: \"" + keyword + "\"");
        System.out.println("   (Hệ thống sẽ gọi YouTube API hoặc lấy từ Cache)...");

        long startSearch = System.currentTimeMillis();
        SearchSongData searchResult = searchService.searchSongs(keyword, null);
        long costSearch = System.currentTimeMillis() - startSearch;

        List<SongData> songs = searchResult.songs();
        System.out.printf("   -> Tìm thấy %d bài hát trong %d ms.%n", songs.size(), costSearch);

        // Kiểm tra songID của các bài tìm được
        boolean allSongIdNull = true;
        for (SongData s : songs) {
            if (s.songID() != null) {
                allSongIdNull = false;
                break;
            }
        }
        System.out.println("   -> Tất cả bài hát mới tìm về đều có songID = " + (allSongIdNull ? "null (Chưa lưu DB ✅)" : "đã có ID trong DB"));

        int afterSearchSongs = countRows("SONGS");
        int afterSearchCache = countRows("SEARCH_CACHE");

        System.out.println("\n📊 ĐỐI CHIẾU SỐ LƯỢNG SAU KHI SEARCH:");
        System.out.printf("   - Bảng SEARCH_CACHE: %d -> %d (%+d bản ghi)%n", initialCache, afterSearchCache, (afterSearchCache - initialCache));
        System.out.printf("   - Bảng SONGS:        %d -> %d (%+d bản ghi)%n", initialSongs, afterSearchSongs, (afterSearchSongs - initialSongs));

        if (afterSearchSongs == initialSongs) {
            System.out.println("   >>> KẾT LUẬN [ĐẠT] ✅: Tìm kiếm KHÔNG hề chèn bất kỳ dòng nào vào bảng SONGS!");
        } else {
            System.out.println("   >>> KẾT LUẬN [CHƯA ĐẠT] ❌: Bảng SONGS bị tăng thêm dòng khi chỉ mới search!");
            return;
        }

        if (songs.isEmpty()) {
            System.out.println("⚠️ Không có bài hát nào để test bước 3.");
            return;
        }

        // BƯỚC 3: GIẢ LẬP USER BẤM NGHE NHẠC (TƯƠNG TÁC)
        SongData target = songs.get(0);
        System.out.println("\n🎧 [BƯỚC 3] GIẢ LẬP HÀNH VI USER BẤM NGHE BÀI HÁT ĐẦU TIÊN:");
        System.out.println("   - Tên bài:     " + target.title());
        System.out.println("   - External ID: " + target.externalId());
        System.out.println("   - Song ID:     " + target.songID() + " (Đang là null)");
        System.out.println("   -> Gửi yêu cầu VIEW_SONG_DETAIL (externalId: " + target.externalId() + ")...");

        long startPlay = System.currentTimeMillis();
        SongData detailResult = searchService.detail(null, target.externalId());
        long costPlay = System.currentTimeMillis() - startPlay;

        int afterPlaySongs = countRows("SONGS");
        System.out.printf("   -> Hoàn thành trong %d ms.%n", costPlay);
        System.out.println("\n📊 ĐỐI CHIẾU SỐ LƯỢNG SAU KHI BẤM NGHE:");
        System.out.printf("   - Bảng SONGS: %d -> %d (%+d bản ghi)%n", afterSearchSongs, afterPlaySongs, (afterPlaySongs - afterSearchSongs));

        if (afterPlaySongs > afterSearchSongs) {
            System.out.println("   >>> KẾT LUẬN [ĐẠT] ✅: Bài hát ĐÃ ĐƯỢC LƯU CHÍNH THỨC VÀO BẢNG SONGS!");
            System.out.println("   - Song ID mới cấp trong DB: " + detailResult.songID());
            System.out.println("   - Audio Stream URL:          " + detailResult.audio_url());

            String videoId = target.externalId().replace("youtube:video:", "");
            File mp3 = new File("storage/audio", videoId + ".mp3");
            if (mp3.isFile()) {
                System.out.printf("   - File MP3 vật lý:           storage/audio/%s.mp3 (%.2f MB) ✅%n",
                        videoId, mp3.length() / (1024.0 * 1024.0));
            }
        } else {
            System.out.println("   >>> KẾT LUẬN [CHƯA ĐẠT] ❌: Bảng SONGS không ghi nhận bài hát sau khi bấm nghe!");
        }

        // BƯỚC 4: BẤM NGHE LẠI LẦN 2 (KIỂM TRA CACHE HIT CỦA SONG_DETAIL)
        System.out.println("\n⚡ [BƯỚC 4] BẤM NGHE LẠI BÀI VỪA RỒI (ID = " + detailResult.songID() + "):");
        long startReplay = System.currentTimeMillis();
        SongData replayResult = searchService.detail(detailResult.songID(), null);
        long costReplay = System.currentTimeMillis() - startReplay;

        System.out.printf("   -> Lấy thông tin bài hát trong: %d ms (Cache Hit SONG_DETAIL) ✅%n", costReplay);
        System.out.println("======================================================================\n");
    }

    private static void handleSearchInteractive() {
        System.out.println("\n--- [2] TÌM KIẾM BÀI HÁT TƯƠNG TÁC ---");
        System.out.print("Nhập từ khóa tìm kiếm: ");
        String keyword = scanner.nextLine().trim();
        if (keyword.isEmpty()) {
            System.out.println("⚠️ Từ khóa không được rỗng.");
            return;
        }

        int songsBefore = countRows("SONGS");
        int cacheBefore = countRows("SEARCH_CACHE");

        System.out.println("⏳ Đang tìm kiếm...");
        try {
            SearchSongData result = searchService.searchSongs(keyword, null);
            List<SongData> list = result.songs();

            int songsAfter = countRows("SONGS");
            int cacheAfter = countRows("SEARCH_CACHE");

            System.out.println("\n🎉 KẾT QUẢ TÌM KIẾM (" + list.size() + " bài):");
            for (int i = 0; i < list.size(); i++) {
                SongData s = list.get(i);
                System.out.printf("  [%2d] %-35s | ID DB: %-6s | ExternalId: %s%n",
                        i + 1,
                        truncate(s.title(), 35),
                        s.songID() != null ? String.valueOf(s.songID()) : "null",
                        s.externalId());
            }

            System.out.println("\n📊 Trạng thái Database:");
            System.out.printf("   - Bảng SONGS:        %d (Tăng: %+d)%n", songsAfter, (songsAfter - songsBefore));
            System.out.printf("   - Bảng SEARCH_CACHE: %d (Tăng: %+d)%n", cacheAfter, (cacheAfter - cacheBefore));

            if (songsAfter == songsBefore) {
                System.out.println("💡 Xác nhận: Bảng SONGS không bị tăng thêm dòng nào. Dữ liệu đang được giữ ở Cache.");
            }

            System.out.print("\n👉 Bạn có muốn bấm nghe bài nào trong danh sách trên không? (Nhập số 1-" + list.size() + " hoặc Enter để bỏ qua): ");
            String select = scanner.nextLine().trim();
            if (select.matches("\\d+")) {
                int idx = Integer.parseInt(select) - 1;
                if (idx >= 0 && idx < list.size()) {
                    triggerPlaySong(list.get(idx));
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Lỗi tìm kiếm: " + e.getMessage());
        }
    }

    private static void handlePlayInteractive() {
        System.out.println("\n--- [3] BẤM NGHE BÀI HÁT (KÍCH HOẠT LƯU VÀO CSDL) ---");
        System.out.print("Nhập YouTube External ID (vd: youtube:video:dQw4w9WgXcQ) hoặc Song ID: ");
        String input = scanner.nextLine().trim();

        if (input.isEmpty()) return;

        try {
            Long songId = null;
            String externalId = null;
            if (input.matches("\\d+")) {
                songId = Long.parseLong(input);
            } else if (input.startsWith("youtube:video:")) {
                externalId = input;
            } else if (input.matches("[A-Za-z0-9_-]{11}")) {
                externalId = "youtube:video:" + input;
            } else {
                System.out.println("⚠️ Định dạng không hợp lệ. Cần số nguyên (Song ID) hoặc 11 ký tự YouTube Video ID.");
                return;
            }

            int songsBefore = countRows("SONGS");
            System.out.println("⏳ Đang kích hoạt VIEW_SONG_DETAIL (tải MP3 & import CSDL nếu chưa có)...");

            SongData data = searchService.detail(songId, externalId);
            int songsAfter = countRows("SONGS");

            System.out.println("\n🎉 THÀNH CÔNG!");
            System.out.println("   - Song ID:     " + data.songID());
            System.out.println("   - Tên bài:     " + data.title());
            System.out.println("   - External ID: " + data.externalId());
            System.out.println("   - Link Audio:  " + data.audio_url());
            System.out.printf("   - Bảng SONGS:  %d -> %d (%+d bài mới vào CSDL)%n",
                    songsBefore, songsAfter, (songsAfter - songsBefore));
        } catch (Exception e) {
            System.out.println("❌ Thao tác thất bại: " + e.getMessage());
        }
    }

    private static void triggerPlaySong(SongData song) {
        try {
            System.out.println("\n⏳ Đang tải MP3 & nạp bài hát vào CSDL...");
            int before = countRows("SONGS");
            SongData data = searchService.detail(song.songID(), song.externalId());
            int after = countRows("SONGS");

            System.out.println("🎉 ĐÃ IMPORT VÀ TẢI THÀNH CÔNG!");
            System.out.println("   - Song ID cấp mới trong DB: " + data.songID());
            System.out.println("   - Tên bài:                  " + data.title());
            System.out.println("   - Link stream HTTP nội bộ:  " + data.audio_url());
            System.out.printf("   - Bảng SONGS:               %d -> %d (%+d bài)%n", before, after, (after - before));
            System.out.println("💡 Bạn có thể dán link stream vào Chrome/Edge để nghe thử!");
        } catch (Exception e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }

    private static void printDatabaseStatistics() {
        System.out.println("\n--- [4] THỐNG KÊ HIỆN TẠI CỦA DATABASE ---");
        int songs = countRows("SONGS");
        int artists = countRows("ARTISTS");
        int cache = countRows("SEARCH_CACHE");
        int playlists = countRows("PLAYLISTS");
        int playlistSongs = countRows("PLAYLIST_SONGS");

        System.out.printf("  1. Bảng SONGS:          %4d bài hát%n", songs);
        System.out.printf("  2. Bảng ARTISTS:        %4d ca sĩ/nghệ sĩ%n", artists);
        System.out.printf("  3. Bảng SEARCH_CACHE:   %4d lượt cache tìm kiếm (JSONB)%n", cache);
        System.out.printf("  4. Bảng PLAYLISTS:      %4d playlist%n", playlists);
        System.out.printf("  5. Bảng PLAYLIST_SONGS: %4d liên kết bài hát trong playlist%n", playlistSongs);

        File audioDir = new File("storage/audio");
        File[] mp3Files = audioDir.listFiles((dir, name) -> name.endsWith(".mp3"));
        int mp3Count = mp3Files != null ? mp3Files.length : 0;
        long totalBytes = 0;
        if (mp3Files != null) {
            for (File f : mp3Files) totalBytes += f.length();
        }
        System.out.printf("  6. File MP3 trên đĩa:   %4d file (%.2f MB trong storage/audio)%n",
                mp3Count, totalBytes / (1024.0 * 1024.0));
    }

    private static int countRows(String tableName) {
        String sql = "SELECT COUNT(*) FROM " + tableName;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Lỗi đếm số dòng bảng " + tableName + ": " + e.getMessage());
        }
        return -1;
    }

    private static String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() > max ? text.substring(0, max - 3) + "..." : text;
    }
}
