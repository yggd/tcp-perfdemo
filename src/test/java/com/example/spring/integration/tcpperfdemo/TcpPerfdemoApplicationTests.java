package com.example.spring.integration.tcpperfdemo;

import com.example.spring.integration.tcpperfdemo.client.ClientService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TcpPerfdemoApplicationTests {

    @Autowired
    private BlockingQueue blockingQueue;

    @Autowired
    private ClientService clientService;

    @Autowired
    private ThreadPoolTaskExecutor tcpExecutor;

    @Value("${app.thread-size}")
    private int threadSize;

    @Test
    public void testSendMessage() throws InterruptedException, ExecutionException {
        final BlockingQueue<Future<String>> futures = new ArrayBlockingQueue<>(threadSize);
        IntStream.range(0, threadSize).forEach(i -> {
            try {
                futures.put(clientService.exchange("execute[" + i + "]"));
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        });

        IntStream.range(0, threadSize).forEach(i -> {
            try {
                assertThat(futures.take().get(), is(startsWith("execute[")));
            } catch (InterruptedException | ExecutionException e) {
                throw new IllegalStateException(e);
            }
        });
    }

}
