package com.rishabhkohli.terminal;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

interface SocketDelegate {
    void onMessageReceived(String message);
    void onDisconnect();
}

class SocketHandler {
    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;

    synchronized void printOut(String message) {
        final String outputMessage = message;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    out.write(outputMessage);
                    out.flush();
                } catch (IOException | NullPointerException e) {
                    e.printStackTrace();
                    closeSocket();
                }
            }
        }).start();
    }

    synchronized boolean setSocket(String IP, int Port, final SocketDelegate sD) {
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(IP, Port), 2000);
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            new Thread(new Runnable() {
                @Override
                public void run() {
                    StringBuilder sb = new StringBuilder();
                    while (true) {
                        sb.setLength(0);
                        try {
                            do  {
                                char c = (char) in.read();
                                if (c == '\uFFFF') throw new IOException("Connection closed by server");
                                sb.append(c);
                            } while (in.ready());
                            sD.onMessageReceived(sb.toString());
                        } catch (IOException e) {
                            e.printStackTrace();
                            sD.onDisconnect();
                            break;
                        }
                    }
                }
            }).start();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    synchronized void closeSocket() {
        try {
            socket.close();
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    synchronized boolean isClosed() {
        return socket.isClosed();
    }

    synchronized boolean isConnected() {
        return socket.isConnected();
    }
}
