package io.github.teams4j.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/** The one thing the JDK transport decides on its own: how it speaks to a plain-HTTP address. */
class JdkHttpTransportTest {

    /**
     * A raw socket, because a real HTTP server would hide the point: the JDK client, left to its
     * default, asks a plain-HTTP server to upgrade to HTTP/2, and a Node server (the Agents
     * Playground among them) answers by closing the socket. The transport must not ask.
     */
    @Test
    void speaksHttp11ToAPlainHttpAddressWithoutAskingForAnUpgrade() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            CompletableFuture<List<String>> requestLines = CompletableFuture.supplyAsync(() -> serveOnce(server));
            HttpTransport transport = HttpTransport.jdk(HttpClient.newBuilder().build());

            HttpExchange.Response response = transport
                    .send(new HttpExchange.Request(
                            "POST",
                            URI.create("http://127.0.0.1:" + server.getLocalPort() + "/v3/conversations"),
                            Map.of("Content-Type", "application/json"),
                            "{}",
                            Duration.ofSeconds(5)))
                    .get(10, TimeUnit.SECONDS);

            assertThat(response.statusCode()).isEqualTo(201);
            List<String> lines = requestLines.get(10, TimeUnit.SECONDS);
            assertThat(lines.get(0)).endsWith(" HTTP/1.1");
            assertThat(lines).noneMatch(line -> line.toLowerCase(Locale.ROOT).startsWith("upgrade:"));
            assertThat(lines).noneMatch(line -> line.toLowerCase(Locale.ROOT).startsWith("http2-settings:"));
        }
    }

    /** A remote server chooses how much it sends; past the limit the exchange fails instead of buffering it. */
    @Test
    void aResponseBodyOverTheLimitFailsTheExchange() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            CompletableFuture<Void> serving = CompletableFuture.runAsync(() -> serveBody(server, 64 * 1024));
            HttpTransport transport = HttpTransport.jdk(HttpClient.newBuilder().build(), 1024);

            CompletableFuture<HttpExchange.Response> sent = transport.send(new HttpExchange.Request(
                    "GET",
                    URI.create("http://127.0.0.1:" + server.getLocalPort() + "/big"),
                    Map.of(),
                    null,
                    Duration.ofSeconds(5)));

            assertThatThrownBy(() -> sent.get(10, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(IOException.class)
                    .hasMessageContaining("longer than 1024 bytes");
            serving.get(10, TimeUnit.SECONDS);
        }
    }

    private static void serveBody(ServerSocket server, int length) {
        try (Socket socket = server.accept()) {
            BufferedReader in =
                    new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                // the request head is not looked at
            }
            OutputStream out = socket.getOutputStream();
            out.write(("HTTP/1.1 200 OK\r\nContent-Length: " + length + "\r\nConnection: close\r\n\r\n")
                    .getBytes(StandardCharsets.US_ASCII));
            out.write(new byte[length]);
            out.flush();
        } catch (IOException ignored) {
            // the client may hang up once it has seen enough
        }
    }

    /** Reads one request's head, answers 201, and hands the head back. */
    private static List<String> serveOnce(ServerSocket server) {
        try (Socket socket = server.accept();
                BufferedReader in =
                        new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                lines.add(line);
            }
            OutputStream out = socket.getOutputStream();
            out.write("HTTP/1.1 201 Created\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                    .getBytes(StandardCharsets.US_ASCII));
            out.flush();
            return lines;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
