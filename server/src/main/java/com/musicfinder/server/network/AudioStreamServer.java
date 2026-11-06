package com.musicfinder.server.network;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.musicfinder.server.api.YtDlpApi;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AudioStreamServer {
    private static final long CACHE_TIME_MS = 24L * 60 * 60 * 1000; // 24 giờ không sử dụng
    private final int port;
    private final File directory;
    private final Map<HttpExchange, File> activeFiles = new HashMap<>();
    private HttpServer server;
    private ScheduledExecutorService pool;

    public AudioStreamServer() {
        this(Integer.getInteger("music.audio.port", 8088),
                new File(System.getProperty("music.audio.directory", "storage/audio")));
    }

    public AudioStreamServer(int port, File directory) {
        this.port = port;
        this.directory = directory;
    }

    public static String getAudioUrl(String videoId) {
        int port = Integer.getInteger("music.audio.port", 8088);
        String base = System.getProperty("music.audio.baseUrl", "http://127.0.0.1:" + port);
        if (base.endsWith("/"))
            base = base.substring(0, base.length() - 1);
        return base + "/audio/" + videoId + ".mp3";
    }

    public boolean start() {
        try {
            Files.createDirectories(directory.toPath());
            server = HttpServer.create(new InetSocketAddress(port), 0);
            pool = Executors.newScheduledThreadPool(8);
            server.setExecutor(pool);
            server.createContext("/audio/", this::sendAudio);
            deleteOldAudio();
            pool.scheduleAtFixedRate(this::deleteOldAudio, 1, 1, TimeUnit.HOURS);
            server.start();
            System.out.println("HTTP audio đang lắng nghe tại cổng " + server.getAddress().getPort());
            return true;
        } catch (IOException e) {
            System.out.println("Không mở được HTTP audio: " + e.getMessage());
            stop();
            return false;
        }
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    private void sendAudio(HttpExchange exchange) {
        try {
            String method = exchange.getRequestMethod();
            if (!method.equals("GET") && !method.equals("HEAD")) {
                exchange.getResponseHeaders().set("Allow", "GET, HEAD");
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            String name = exchange.getRequestURI().getPath().substring("/audio/".length());
            // Chỉ nhận videoId.mp3; không nhận đường dẫn đến file khác trên máy.
            if (!name.matches("[A-Za-z0-9_-]{11}\\.mp3")) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            File file = new File(directory, name);
            synchronized (activeFiles) {
                activeFiles.put(exchange, file);
                if (file.isFile() && !Files.isSymbolicLink(file.toPath()))
                    file.setLastModified(System.currentTimeMillis());
            }
            if (!file.isFile() || file.length() == 0 || Files.isSymbolicLink(file.toPath())) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            try (RandomAccessFile input = new RandomAccessFile(file, "r")) {
                long size = input.length();
                long start = 0;
                long end = size - 1;
                int status = 200;
                String range = exchange.getRequestHeaders().getFirst("Range");
                if (method.equals("GET") && range != null && !range.contains(",")) {
                    try {
                        if (!range.matches("bytes=\\d*-\\d*"))
                            throw new IllegalArgumentException();
                        String[] parts = range.substring(6).split("-", -1);
                        if (parts[0].isEmpty()) {
                            long lastBytes = Long.parseLong(parts[1]);
                            if (lastBytes <= 0)
                                throw new IllegalArgumentException();
                            start = Math.max(0, size - lastBytes);
                        } else {
                            start = Long.parseLong(parts[0]);
                            if (!parts[1].isEmpty())
                                end = Math.min(end, Long.parseLong(parts[1]));
                        }
                        if (start >= size || start > end)
                            throw new IllegalArgumentException();
                        status = 206;
                    } catch (IllegalArgumentException e) {
                        exchange.getResponseHeaders().set("Content-Range", "bytes */" + size);
                        exchange.sendResponseHeaders(416, -1);
                        return;
                    }
                }
                long length = end - start + 1;
                exchange.getResponseHeaders().set("Content-Type", "audio/mpeg");
                exchange.getResponseHeaders().set("Accept-Ranges", "bytes");
                exchange.getResponseHeaders().set("Content-Length", String.valueOf(length));
                if (status == 206)
                    exchange.getResponseHeaders().set("Content-Range", "bytes " + start + "-" + end + "/" + size);
                if (method.equals("HEAD")) {
                    exchange.sendResponseHeaders(200, -1);
                    return;
                }
                exchange.sendResponseHeaders(status, length);
                input.seek(start);
                try (OutputStream output = exchange.getResponseBody()) {
                    byte[] buffer = new byte[16 * 1024];
                    while (length > 0) {
                        int count = input.read(buffer, 0, (int) Math.min(buffer.length, length));
                        if (count == -1)
                            break;
                        output.write(buffer, 0, count);
                        length -= count;
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Ngắt gửi audio: " + e.getMessage());
        } finally {
            exchange.close();
            synchronized (activeFiles) {
                activeFiles.remove(exchange);
            }
        }
    }

    void deleteOldAudio() {
        synchronized (YtDlpApi.class) {
            synchronized (activeFiles) {
                File[] files = directory.listFiles();
                if (files == null)
                    return;
                long now = System.currentTimeMillis();
                for (File file : files) {
                    if (file.isFile() && file.getName().matches("[A-Za-z0-9_-]{11}\\.mp3")
                            && !Files.isSymbolicLink(file.toPath()) && !activeFiles.containsValue(file)
                            && now - file.lastModified() >= CACHE_TIME_MS) {
                        if (file.delete())
                            System.out.println("Đã xóa MP3 hết hạn: " + file.getName());
                        else
                            System.out.println("Chưa xóa được MP3 hết hạn: " + file.getName());
                    }
                }
            }
        }
    }

    public void stop() {
        if (server != null)
            server.stop(0);
        if (pool != null)
            pool.shutdownNow();
    }
}
