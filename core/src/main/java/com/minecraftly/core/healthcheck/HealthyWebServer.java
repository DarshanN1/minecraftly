package com.minecraftly.core.healthcheck;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Single-threaded simple HTTP server to respond with code 200 (success) when a client connection is received.
 * This allows Java applications to respond to Google Compute health checks.
 */
public class HealthyWebServer implements Runnable {

    private static final DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

    private final int port;
    private final Consumer<Runnable> runOnMainThread;

    private AtomicBoolean running = new AtomicBoolean(true);

    private Lock lock = new ReentrantLock();
    private Condition mainThreadResponse = lock.newCondition();

    public HealthyWebServer(int port, Consumer<Runnable> runOnMainThread) {
        this.port = port;
        this.runOnMainThread = runOnMainThread;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (running.get()) {
                try (Socket clientSocket = serverSocket.accept()) {
                    try (BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {
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

                            String responseCode;

                            try {
                                responseCode = mainThreadResponse.await(3, TimeUnit.SECONDS) ? "200 OK" : "503 Service Unavailable";
                            } catch (InterruptedException e) {
                                responseCode = "503 Service Unavailable";
                            }

                            bufferedWriter.write("HTTP/1.0 " + responseCode + "\r\n");
                            bufferedWriter.write("Date: " + dateFormat.format(new Date()) + "\r\n");
                            bufferedWriter.write("Content-Type: text/html\r\n");
                            bufferedWriter.write("Server: Minecraftly/1.0.0\r\n");
                            bufferedWriter.write("Connection: close");
                            bufferedWriter.write("\r\n");
                        } finally {
                            lock.unlock();
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        running.set(false);
    }
}
