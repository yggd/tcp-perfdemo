package com.example.spring.integration.tcpperfdemo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;

import java.util.concurrent.CountDownLatch;

@Slf4j
public class InboundService {

    private final CountDownLatch countDownLatch;

    public InboundService(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }

    public Message<String> execute(Message<String> msg) throws InterruptedException {
        String body = msg.getPayload();
        log.info("msg:{}", body);
        countDownLatch.countDown();
        countDownLatch.await(); // maxスレッド接続まで滞留
        return msg;
    }
}
