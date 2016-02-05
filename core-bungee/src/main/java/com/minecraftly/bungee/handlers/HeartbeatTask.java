package com.minecraftly.bungee.handlers;

import com.minecraftly.MinecraftlyCommon;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

/**
 * Sends a UDP packet to the Bukkit partner server every interval.
 * This packet should be sent from the main thread, we don't want heartbeats to be sent if the main thread is frozen.
 */
public class HeartbeatTask implements Runnable {

    private final int port;
    private DatagramSocket datagramSocket;

    public HeartbeatTask(int port) throws SocketException {
        this.port = port;
        this.datagramSocket = new DatagramSocket();
    }

    @Override
    public void run() {
        byte[] bytes = MinecraftlyCommon.UDP_HEARTBEAT_CONTENTS.getBytes(Charset.forName("UTF-8"));

        try {
            DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length, InetAddress.getByName("localhost"), port);
            datagramSocket.send(datagramPacket);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) { // todo
            e.printStackTrace();
        }
    }
}
