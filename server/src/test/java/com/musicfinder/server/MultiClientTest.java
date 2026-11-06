package com.musicfinder.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.musicfinder.server.handler.RequestDispatcher;
import com.musicfinder.server.network.TcpServer;
import com.musicfinder.shared.enums.Action;
import com.musicfinder.shared.enums.Status;
import com.musicfinder.shared.request.Request;
import com.musicfinder.shared.response.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MultiClientTest {
    private static final int PORT = 54321;
    private static TcpServer server;

    @BeforeAll
    static void startServer() throws Exception {
        server = new TcpServer(PORT, new RequestDispatcher(), 5, 50);
        new Thread(server::start).start();
        Thread.sleep(500); // chờ server mở cổng
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    // Gửi 1 dòng, nhận 1 dòng
    private Response sendLine(String line) throws IOException {
        try (Socket socket = new Socket("localhost", PORT);
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter writer = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {
            writer.println(line);
            Response res = Response.fromJson(reader.readLine());
            writer.println("bye");
            return res;
        }
    }

    @Test
    void ping20ClientCungLuc() throws Exception {
        int clients = 20;
        ExecutorService pool = Executors.newFixedThreadPool(clients);
        List<Future<Response>> results = new ArrayList<>();

        for (int i = 0; i < clients; i++) {
            final String requestId = "req-" + i;
            results.add(pool.submit(() -> {
                Request req = Request.of(Action.PING, null);
                req.setRequestId(requestId);
                return sendLine(req.toJson());
            }));
        }

        for (int i = 0; i < clients; i++) {
            Response res = results.get(i).get(10, TimeUnit.SECONDS);
            assertTrue(res.isSuccess());
            assertEquals("req-" + i, res.requestId); // mỗi client nhận đúng response của mình
        }
        pool.shutdown();
    }

    @Test
    void jsonHongKhongLamSapServer() throws Exception {
        Response bad = sendLine("abc");
        assertEquals(Status.INVALID_REQUEST, bad.status);

        // server vẫn phục vụ bình thường sau đó
        Response ok = sendLine(Request.of(Action.PING, null).toJson());
        assertTrue(ok.isSuccess());
    }
}