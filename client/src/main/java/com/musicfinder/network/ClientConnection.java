package com.musicfinder.network;

import com.musicfinder.shared.request.Request;
import com.musicfinder.shared.response.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ClientConnection {

    private static final ClientConnection INSTANCE = new ClientConnection();

    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private final Map<String, Consumer<Response>> pendingRequest = new ConcurrentHashMap<>();

    private ClientConnection() {
    }

    public static ClientConnection getInstance() {
        return INSTANCE;
    }

    public void connect(String host, int port) throws IOException {

        if (isConnected()) {
            return;
        }

        socket = new Socket(host, port);

        writer = new PrintWriter(
                new OutputStreamWriter(
                        socket.getOutputStream(),
                        StandardCharsets.UTF_8),
                true);
        reader = new BufferedReader(
                new InputStreamReader(
                        socket.getInputStream(),
                        StandardCharsets.UTF_8));
        startListening();
    }

    public void send(Request request, Consumer<Response> callback) throws IOException {

        if (!isConnected()) {
            throw new IOException("Client chưa kết nối tới server");
        }
        pendingRequest.put(request.getRequestId(), callback);

        writer.println(request.toJson());

        if (writer.checkError()) {
            throw new IOException("Không thể gửi request");
        }
    }

    public void send(Request request) throws IOException {
        send(request, null);
    }

    private void startListening() {
        Thread listenerThread = new Thread(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    try {

                        Response response = Response.fromJson(line);

                        Consumer<Response> callback = pendingRequest.remove(
                                response.getRequestId());

                        if (callback != null) {
                            callback.accept(response);
                        }

                    } catch (Exception e) {

                        System.out.println(
                                "Không thể xử lý response: "
                                        + e.getMessage());
                    }
                }
            } catch (Exception e) {
                System.out.println(
                        "Mất kết nối tới server: "
                                + e.getMessage());
            } finally {

                pendingRequest.clear();
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public boolean isConnected() {

        return socket != null
                && socket.isConnected()
                && !socket.isClosed();
    }

    public synchronized void close()
            throws IOException {

        pendingRequest.clear();

        if (writer != null) {
            writer.close();
            writer = null;
        }

        if (reader != null) {
            reader.close();
            reader = null;
        }

        if (socket != null) {
            socket.close();
            socket = null;
        }
    }
}