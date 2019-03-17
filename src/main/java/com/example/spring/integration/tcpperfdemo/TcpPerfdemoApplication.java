package com.example.spring.integration.tcpperfdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

@SpringBootApplication
@ImportResource("classpath:META-INF/spring/integration.xml")
public class TcpPerfdemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(TcpPerfdemoApplication.class, args);
    }

}
