package com.example.spring.integration.tcpperfdemo.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.yggd.client.tcp.TcpClient;
import org.yggd.client.tcp.TcpClientImpl;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;

@Slf4j
public class ClientService {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private final BlockingQueue responseQueue;
    private final int port;

    public ClientService(BlockingQueue responseQueue, int port) {
        this.responseQueue = responseQueue;
        this.port = port;
    }

    @Async("clientExecutor")
    @SuppressWarnings("unchecked")
    public Future<String> exchange(String msg) {
        log.info("send msg:{}", msg);
        final byte[] rawRequest = serialize(msg);
        final byte[] rawResponse = new TcpClientImpl()
                .connect("localhost", port)
                .exchange(rawRequest);
//        final byte[] rawResponse = sendAndReceive(rawRequest);
        final String responseStr = deserialize(rawResponse);
        try {
            responseQueue.put(responseStr);
        } catch (InterruptedException e) {
            log.error("interrupted exception occurred", e);
            throw new IllegalStateException(e);
        }
        return new AsyncResult<>(responseStr);
    }

    private byte[] serialize(String str) {
        assert str != null;
        final String serializedStr = str.endsWith("\r\n") ? str : str + "\r\n";
        return serializedStr.getBytes(UTF_8);
    }

    private String deserialize(byte[] bytes) {
        final String rawString = new String(bytes, UTF_8);
        return rawString.endsWith("\r\n") ? rawString.substring(0, rawString.length() - 2) : rawString;
    }

    private byte[] sendAndReceive(byte[] bytes) {
        try (Socket socket = new Socket("localhost", port);
             InputStream is = socket.getInputStream();
             OutputStream os = socket.getOutputStream()) {
            os.write(bytes, 0, bytes.length);
            return extractBytes(is);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private byte[] extractBytes(InputStream is) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte first;
        byte second = 0x00;
        while (true) {
            try {
                byte buf = (byte) is.read();
                if (buf < 0) {
                    break;
                }
                first = second;
                second = buf;
                baos.write(buf);
                if (first == '\r' && second == '\n') {
                    break;
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return baos.toByteArray();
    }

}
