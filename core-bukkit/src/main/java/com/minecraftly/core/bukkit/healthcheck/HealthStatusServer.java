package com.minecraftly.core.bukkit.healthcheck;

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

    private Lock lock = new ReentrantLock();
    private Condition mainThreadResponse = lock.newCondition();

    public static void main(String[] args) { // testing purposes
        HealthStatusServer webServer = new HealthStatusServer("Test", 80, (r) -> {
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
        httpServer.stop();
    }

    private class HealthStatusHttpServer implements HttpRequestHandler {
        @Override
        public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
            boolean mainServerResponding = isMainThreadResponding();
            response.setStatusCode(mainServerResponding ? 200 : 503);

            StringEntity entity = new StringEntity(
                    "<html><head><title>" + serviceName + " Status</title></head><body><h1>" + (mainServerResponding ? "OK" : "Not OK") + "</h1></body></html>\n",
                    ContentType.create("text/html", "UTF-8")
            );

            response.setEntity(entity);
        }
    }

}
