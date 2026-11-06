package com.musicfinder.server.network;

import com.musicfinder.server.handler.RequestDispatcher;
import com.musicfinder.shared.enums.Status;
import com.musicfinder.shared.response.Response;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TcpServer {
    private final int port;
    private final RequestDispatcher dispatcher;
    private final ThreadPoolExecutor executor;
    private ServerSocket serverSocket;
    private volatile boolean running = true;

    public TcpServer(int port, RequestDispatcher dispatcher, int corePoolSize, int maxPoolSize) {
        this.port = port;
        this.dispatcher = dispatcher;
        this.executor = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                60, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new ThreadPoolExecutor.AbortPolicy());
    }

    public void start() {
        try (ServerSocket server = new ServerSocket(port)) {
            serverSocket = server;
            System.out.println("Server đang lắng nghe tại cổng " + port);

            while (running) {
                Socket client = server.accept();
                System.out.println("Client " + client.getRemoteSocketAddress() + " đã kết nối!");
                try {
                    executor.execute(new ClientHandler(client, dispatcher));
                } catch (RejectedExecutionException e) {
                    refuse(client); // đã hết thread
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Lỗi ServerSocket: " + e.getMessage());
            }
        }
    }

    // Báo cho client biết server đang bận rồi đóng kết nối
    private void refuse(Socket client) {
        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8), true)) {
            Response res = new Response();
            res.status = Status.SERVER_ERROR;
            res.message = "Server đang quá tải, vui lòng thử lại sau";
            writer.println(res.toJson());
        } catch (IOException ignored) {
        } finally {
            try {
                client.close();
            } catch (IOException ignored) {
            }
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close(); // làm accept() thoát ra
            }
        } catch (IOException ignored) {
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("Server đã dừng.");
    }
}