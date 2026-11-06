package com.musicfinder.server.api;

import org.jsoup.Jsoup;
import org.json.JSONObject;
import org.json.JSONArray;
import com.musicfinder.shared.response.payload.SongData;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class YtDlpApi {
    public static final int MAX_RESULTS = 15;
    private static final int TIMEOUT_SECONDS = 12;
    private static final int MAX_JSON_BYTES = 2 * 1024 * 1024;
    private final String binaryPath;

    public YtDlpApi() {
        this.binaryPath = resolveBinary();
    }

    public YtDlpApi(String binaryPath) {
        this.binaryPath = binaryPath;
    }

    public record RemoteArtist(String externalId, String name) {
    }

    public record RemoteSong(String externalId, String title, int durationSeconds,
            String previewUrl, String thumbnailUrl, String storeUrl, RemoteArtist artist) {
    }

    private static synchronized String resolveBinary() {
        String configured = System.getenv("YTDLP_PATH");
        if (configured != null && !configured.isBlank())
            return configured;
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
        boolean windows = os.startsWith("windows");
        File directory = new File("tools");
        File binary = new File(directory, windows ? "yt-dlp.exe" : "yt-dlp");
        File partial = new File(directory, binary.getName() + ".part");
        try {
            if (!binary.isFile() || binary.length() == 0) {
                String asset;
                if (windows) {
                    if (arch.equals("aarch64") || arch.equals("arm64"))
                        asset = "yt-dlp_arm64.exe";
                    else if (arch.equals("x86") || arch.equals("i386"))
                        asset = "yt-dlp_x86.exe";
                    else
                        asset = "yt-dlp.exe";
                } else if (os.contains("mac")) {
                    asset = "yt-dlp_macos";
                } else if (os.contains("linux")) {
                    if (arch.equals("aarch64") || arch.equals("arm64"))
                        asset = "yt-dlp_linux_aarch64";
                    else if (arch.equals("amd64") || arch.equals("x86_64"))
                        asset = "yt-dlp_linux";
                    else
                        throw new IllegalStateException("Chưa hỗ trợ kiến trúc Linux: " + arch);
                } else {
                    throw new IllegalStateException("Chưa hỗ trợ hệ điều hành: " + os);
                }
                Files.createDirectories(directory.toPath());
                String url = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/" + asset;
                System.out.println("Đang tải yt-dlp vào " + binary.getAbsolutePath());
                try (InputStream input = Jsoup.connect(url).ignoreContentType(true).maxBodySize(0)
                        .timeout(60000).execute().bodyStream();
                        FileOutputStream output = new FileOutputStream(partial)) {
                    input.transferTo(output);
                }
                if (partial.length() == 0)
                    throw new IllegalStateException("File công cụ tải về bị rỗng");
                Files.move(partial.toPath(), binary.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            if (!windows && !binary.canExecute() && !binary.setExecutable(true))
                throw new IllegalStateException("Không cấp được quyền chạy yt-dlp");
            return binary.getAbsolutePath();
        } catch (Exception e) {
            try {
                Files.deleteIfExists(partial.toPath());
            } catch (Exception ignored) {
            }
            System.out.println("Không chuẩn bị được yt-dlp: " + e.getMessage());
            return null;
        }
    }

    // Tự chuẩn bị FFmpeg/ffprobe trên Windows; không cần cài vào PATH.
    public static synchronized boolean prepareAudioTools() {
        boolean windows = System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("windows");
        File tools = new File("tools");
        File ffmpeg = new File(tools, windows ? "ffmpeg.exe" : "ffmpeg");
        File ffprobe = new File(tools, windows ? "ffprobe.exe" : "ffprobe");
        if (ffmpeg.isFile() && ffmpeg.length() > 0 && ffprobe.isFile() && ffprobe.length() > 0)
            return true;
        File ffmpegPart = new File(tools, "ffmpeg.exe.part");
        File ffprobePart = new File(tools, "ffprobe.exe.part");
        try {
            if (!windows) {
                System.out.println("Hãy đặt ffmpeg và ffprobe có quyền chạy vào thư mục tools trên Linux/macOS.");
                return false;
            }
            String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
            String platform = "win64";
            if (arch.equals("arm64") || arch.equals("aarch64"))
                platform = "winarm64";
            else if (arch.equals("x86") || arch.equals("i386"))
                platform = "win32";
            if (!tools.isDirectory() && !tools.mkdirs()) {
                System.out.println("Không tạo được thư mục tools");
                return false;
            }
            Files.deleteIfExists(ffmpegPart.toPath());
            Files.deleteIfExists(ffprobePart.toPath());
            String url = "https://github.com/yt-dlp/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-"
                    + platform + "-gpl.zip";
            System.out.println("Đang tải FFmpeg và ffprobe vào " + tools.getAbsolutePath());
            try (InputStream input = Jsoup.connect(url).ignoreContentType(true).maxBodySize(0)
                    .timeout(60000).execute().bodyStream();
                    ZipInputStream zip = new ZipInputStream(input)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (!entry.isDirectory() && entry.getName().endsWith("/bin/ffmpeg.exe")) {
                        if (!saveFile(zip, ffmpegPart))
                            return false;
                    }
                    if (!entry.isDirectory() && entry.getName().endsWith("/bin/ffprobe.exe")) {
                        if (!saveFile(zip, ffprobePart))
                            return false;
                    }
                    if (ffmpegPart.isFile() && ffprobePart.isFile())
                        break;
                }
            }
            if (ffmpegPart.length() == 0 || ffprobePart.length() == 0) {
                System.out.println("Gói tải về chưa có đủ FFmpeg và ffprobe");
                return false;
            }
            Files.move(ffmpegPart.toPath(), ffmpeg.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.move(ffprobePart.toPath(), ffprobe.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception e) {
            System.out.println("Không chuẩn bị được FFmpeg: " + e.getMessage());
            return false;
        } finally {
            try {
                Files.deleteIfExists(ffmpegPart.toPath());
                Files.deleteIfExists(ffprobePart.toPath());
            } catch (Exception ignored) {
            }
        }
    }

    private static boolean saveFile(InputStream input, File file) {
        try (FileOutputStream output = new FileOutputStream(file)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1)
                output.write(buffer, 0, count);
            return true;
        } catch (Exception e) {
            System.out.println("Không ghi được file công cụ: " + e.getMessage());
            return false;
        }
    }

    public File downloadAudioToCache(String videoId) {
        if (videoId == null || !videoId.matches("[A-Za-z0-9_-]{11}"))
            return null;
        synchronized (YtDlpApi.class) {
            File directory = new File(System.getProperty("music.audio.directory", "storage/audio"));
            File audio = new File(directory, videoId + ".mp3");
            File pending = new File(directory, videoId + ".download.mp3");
            if (audio.isFile() && audio.length() > 0 && !Files.isSymbolicLink(audio.toPath())) {
                audio.setLastModified(System.currentTimeMillis());
                return audio;
            }
            Process process = null;
            try {
                if (binaryPath == null || !prepareAudioTools())
                    return null;
                if (!directory.isDirectory() && !directory.mkdirs()) {
                    System.out.println("Không tạo được thư mục storage/audio");
                    return null;
                }
                System.out.println("Đang tải và chuyển MP3: " + videoId);
                String ffmpegName = System.getProperty("os.name").startsWith("Windows") ? "ffmpeg.exe" : "ffmpeg";
                String[] command = { binaryPath, "--ignore-config", "--no-playlist", "--no-progress",
                        "--force-overwrites",
                        "--socket-timeout", "20", "--retries", "1", "--extractor-retries", "1",
                        "--ffmpeg-location", new File("tools", ffmpegName).getAbsolutePath(), "-f", "ba/b", "-x",
                        "--audio-format", "mp3", "--audio-quality", "128K", "-o",
                        new File(directory, videoId + ".download.%(ext)s").getAbsolutePath(),
                        "--", "https://www.youtube.com/watch?v=" + videoId };
                process = new ProcessBuilder(command).inheritIO().start();
                if (!process.waitFor(180, TimeUnit.SECONDS)) {
                    System.out.println("Tải MP3 quá thời gian chờ 180 giây");
                    return null;
                }
                if (process.exitValue() != 0 || !pending.isFile() || pending.length() == 0) {
                    System.out.println("Không tải/chuyển được MP3: " + videoId);
                    return null;
                }
                Files.move(pending.toPath(), audio.toPath(), StandardCopyOption.REPLACE_EXISTING);
                audio.setLastModified(System.currentTimeMillis());
                return audio;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception e) {
                System.out.println("Không chuẩn bị được MP3: " + e.getMessage());
                return null;
            } finally {
                if (process != null) {
                    for (ProcessHandle child : process.descendants().toList())
                        child.destroyForcibly();
                    process.destroyForcibly();
                }
                try {
                    Files.deleteIfExists(pending.toPath());
                } catch (Exception ignored) {
                }
            }
        }
    }

    public List<RemoteSong> searchSongs(String keyword) {
        return searchSongs(keyword, MAX_RESULTS);
    }

    public List<RemoteSong> searchSongs(String keyword, int limit) {
        if (keyword == null || keyword.isBlank())
            return new ArrayList<>();
        if (limit < 1 || limit > 20)
            limit = MAX_RESULTS;
        JSONObject result = execute(List.of("--flat-playlist", "--playlist-end", String.valueOf(limit),
                "--", "ytsearch" + limit + ":" + keyword.trim()));
        if (result == null)
            return null;
        return readSongs(result);
    }

    public List<RemoteArtist> searchArtists(String keyword) {
        if (keyword == null || keyword.isBlank())
            return new ArrayList<>();
        JSONObject json = execute(
                List.of("--flat-playlist", "--playlist-end", "10", "--", "ytsearch10:" + keyword.trim()));
        if (json == null)
            return null;
        List<RemoteArtist> result = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        JSONArray entries = json.optJSONArray("entries");
        if (entries == null)
            return result;
        for (int i = 0; i < entries.length(); i++) {
            JSONObject item = entries.optJSONObject(i);
            if (item == null)
                continue;
            String id = item.optString("channel_id", "");
            String name = item.optString("channel", item.optString("uploader", ""));
            if (!id.matches("UC[A-Za-z0-9_-]{22}") || name.isBlank() || ids.contains(id))
                continue;
            result.add(new RemoteArtist("youtube:channel:" + id, name));
            ids.add(id);
        }
        return result;
    }

    public List<RemoteSong> songsForArtist(String sourceId) {
        if (sourceId == null || !sourceId.matches("youtube:channel:UC[A-Za-z0-9_-]{22}"))
            return new ArrayList<>();
        String channel = sourceId.substring("youtube:channel:".length());
        String uploads = "UU" + channel.substring(2);
        JSONObject result = execute(List.of("--flat-playlist", "--playlist-end", String.valueOf(MAX_RESULTS), "--",
                "https://www.youtube.com/playlist?list=" + uploads));
        if (result == null)
            return null;
        return readSongs(result);
    }

    private List<RemoteSong> readSongs(JSONObject json) {
        List<RemoteSong> result = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        JSONArray entries = json.optJSONArray("entries");
        if (entries == null)
            return result;
        for (int i = 0; i < entries.length(); i++) {
            JSONObject item = entries.optJSONObject(i);
            if (item == null || item.optBoolean("is_live", false))
                continue;
            String id = item.optString("id", "");
            String title = item.optString("title", "").trim();
            String channelId = item.optString("channel_id", json.optString("channel_id", ""));
            String channel = item.optString("channel",
                    item.optString("uploader", json.optString("channel", "Kênh chưa rõ")));
            if (!id.matches("[A-Za-z0-9_-]{11}") || title.isBlank() || ids.contains(id))
                continue;
            String artistId = "youtube:channel:" + channelId;
            if (channelId.isBlank())
                artistId = "youtube:unknown:" + id;
            RemoteArtist artist = new RemoteArtist(artistId, channel);
            String thumbnail = item.optString("thumbnail", "https://i.ytimg.com/vi/" + id + "/hqdefault.jpg");
            int duration = (int) Math.max(0, Math.min(Integer.MAX_VALUE, item.optDouble("duration", 0)));
            result.add(new RemoteSong("youtube:video:" + id, title, duration, null, thumbnail,
                    "https://www.youtube.com/watch?v=" + id, artist));
            ids.add(id);
        }
        return result;
    }

    public SongData getSongDetail(String videoId) {
        if (videoId == null || !videoId.matches("[A-Za-z0-9_-]{11}"))
            return null;
        String url = "https://www.youtube.com/watch?v=" + videoId;
        JSONObject json = execute(List.of("--no-playlist", "-f", "ba[ext=m4a]/ba/b", "--", url));
        if (json == null)
            return null;
        String audioUrl = json.optString("url", "");
        if (!audioUrl.startsWith("https://") && !audioUrl.startsWith("http://")) {
            System.out.println("yt-dlp chưa lấy được link audio trực tiếp");
            return null;
        }
        String composer = null;
        for (String line : json.optString("description", "").split("\n")) {
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.contains("sáng tác:") || lower.contains("composer:") || lower.contains("nhạc sĩ:")) {
                composer = line.trim();
                break;
            }
        }
        String channel = json.optString("channel", json.optString("uploader", "Kênh chưa rõ"));
        int duration = (int) Math.max(0, Math.min(Integer.MAX_VALUE, json.optDouble("duration", 0)));
        return new SongData(null, json.optString("title", ""), composer, List.of(channel), duration,
                audioUrl, url, json.optString("thumbnail", "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg"), 0L,
                null, "youtube:video:" + videoId);
    }

    private JSONObject execute(List<String> arguments) {
        if (binaryPath == null)
            return null;
        Process process = null;
        Thread timeout = null;
        try {
            List<String> command = new ArrayList<>();
            command.add(binaryPath);
            command.addAll(List.of("--ignore-config", "--dump-single-json", "--skip-download", "--no-warnings",
                    "--ignore-no-formats-error", "--socket-timeout", "8", "--retries", "1", "--extractor-retries",
                    "1"));
            command.addAll(arguments);
            process = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.INHERIT).start();
            Process started = process;
            Thread caller = Thread.currentThread();
            // Phải canh trong lúc đọc stdout; waitFor sau khi đọc không tự chống treo.
            timeout = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        long end = System.currentTimeMillis() + TIMEOUT_SECONDS * 1000L;
                        while (started.isAlive() && System.currentTimeMillis() < end && !caller.isInterrupted())
                            Thread.sleep(200);
                        if (started.isAlive())
                            started.destroyForcibly();
                    } catch (InterruptedException e) {
                    }
                }
            });
            timeout.setDaemon(true);
            timeout.start();
            byte[] bytes = process.getInputStream().readNBytes(MAX_JSON_BYTES + 1);
            if (bytes.length > MAX_JSON_BYTES || process.waitFor() != 0) {
                System.out.println("yt-dlp lỗi, quá thời gian hoặc JSON quá lớn");
                return null;
            }
            return new JSONObject(new String(bytes, StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            System.out.println("Không lấy được metadata yt-dlp: " + e.getMessage());
            return null;
        } finally {
            if (timeout != null)
                timeout.interrupt();
            if (process != null)
                process.destroyForcibly();
        }
    }
}
