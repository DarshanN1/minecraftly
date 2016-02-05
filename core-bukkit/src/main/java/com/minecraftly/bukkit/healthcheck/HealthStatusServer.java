package com.minecraftly.bukkit.healthcheck;

import com.minecraftly.MinecraftlyCommon;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Single-threaded simple HTTP server to respond with code 200 (success) when a client connection is received.
 * This allows Java applications to respond to Google Compute health checks.
 */
public class HealthStatusServer {

    private final String serviceName;
    private final Consumer<Runnable> runOnMainThread;

    protected final HttpServer httpServer;
    private boolean running = true;

    private Lock lock = new ReentrantLock();
    private Condition mainThreadResponse = lock.newCondition();
    private long proxyLastHeartbeat = System.currentTimeMillis();

    public static void main(String[] args) { // testing purposes
        HealthStatusServer webServer = new HealthStatusServer("Test", 8080, (r) -> {
            new Thread(() -> {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                r.run();
            }).start();
        });

        try {
            webServer.httpServer.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public HealthStatusServer(String instanceName, int port, Consumer<Runnable> runOnMainThread) {
        this.serviceName = instanceName;
        this.runOnMainThread = runOnMainThread;

        SocketConfig socketConfig = SocketConfig.custom()
                .setSoTimeout(15000)
                .setTcpNoDelay(true)
                .build();

        httpServer = ServerBootstrap.bootstrap()
                .setListenerPort(port)
                .setServerInfo("Minecraftly/1.1")
                .setSocketConfig(socketConfig)
                .registerHandler("*", new HealthStatusHttpServer())
                .create();

        try {
            httpServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isMainThreadResponding() {
        lock.lock();

        try {
            runOnMainThread.accept(() -> {
                lock.lock();

                try {
                    mainThreadResponse.signal();
                } finally {
                    lock.unlock();
                }
            });

            try {
                return mainThreadResponse.await(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            }
        } finally {
            lock.unlock();
        }
    }

    public void stop() {
        running = false;
        httpServer.stop();
    }

    private class HealthStatusHttpServer implements HttpRequestHandler {

        private final static String OK = "<span style=\"color:green\">OK</span>";
        private final static String NOT_OK = "<span style=\"color:red\">Not OK</span>";

        @Override
        public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
            boolean mainThreadResponding = isMainThreadResponding();
            long lastProxyResponseTime = System.currentTimeMillis() - proxyLastHeartbeat;
            boolean proxyHealthy = lastProxyResponseTime <= TimeUnit.SECONDS.toMillis(3);
            boolean overallHealthy = mainThreadResponding && proxyHealthy;

            response.setStatusCode(overallHealthy ? 200 : 503);

            String html = "<html><head><title>" + serviceName + " Status</title></head>"
                    + "<body><h1>" + (overallHealthy ? OK : NOT_OK) + "</h1>"
                    + "Bukkit (Real-time) - " + (mainThreadResponding ? OK : NOT_OK) + ""
                    + "<br />BungeeCord - Last Response " + TimeUnit.MILLISECONDS.toSeconds(lastProxyResponseTime) + " seconds ago, " + (proxyHealthy ? OK : NOT_OK)
                    + "</body></html>\n";

            response.setEntity(new StringEntity(html, ContentType.create("text/html", "UTF-8")));
        }
    }

    /**
     * Handles incoming heartbeat datagram packets.
     * This should be run asynchronously as blocking methods are used.
     */
    public class HeartbeatDatagramPacketHandler implements Runnable {

        private final DatagramSocket datagramSocket;

        public HeartbeatDatagramPacketHandler(int port) throws SocketException {
            this.datagramSocket = new DatagramSocket(port);
        }

        @Override
        public void run() {
            while (running) {
                byte[] buffer = new byte[MinecraftlyCommon.UDP_HEARTBEAT_CONTENTS.getBytes().length]; // this is the only thing we send, so that's as big as it gets
                DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);

                try {
                    datagramSocket.receive(datagramPacket);
                    String message = new String(datagramPacket.getData(), 0, datagramPacket.getLength(), Charset.forName("UTF-8"));

                    if (message.equals(MinecraftlyCommon.UDP_HEARTBEAT_CONTENTS)) {
                        proxyLastHeartbeat = System.currentTimeMillis();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
