package com.musicfinder.server;

import com.musicfinder.server.handler.RequestDispatcher;
import com.musicfinder.server.network.TcpServer;
import com.musicfinder.server.network.AudioStreamServer;
import com.musicfinder.server.api.YtDlpApi;

public class ServerApplication {
    private static final int PORT = 12345;
    private static final int CORE_POOL_SIZE = 10;
    private static final int MAX_POOL_SIZE = 50;

    public static void main(String[] args) {
        // Lần đầu tự tải công cụ trên Windows; lỗi tải vẫn cho phép tra cứu.
        YtDlpApi.prepareAudioTools();
        AudioStreamServer audioServer = new AudioStreamServer();
        if (!audioServer.start())
            return;
        RequestDispatcher dispatcher = new RequestDispatcher();
        TcpServer server = new TcpServer(PORT, dispatcher, CORE_POOL_SIZE, MAX_POOL_SIZE);

        // Nhấn Ctrl+C thì dừng server gọn gàng
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            audioServer.stop();
            server.stop();
        }));

        try {
            server.start();
        } finally {
            audioServer.stop();
        }
    }
}
