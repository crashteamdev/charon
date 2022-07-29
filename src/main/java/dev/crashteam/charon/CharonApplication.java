package dev.crashteam.charon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class CharonApplication {

    public static void main(String[] args) {
        SpringApplication.run(CharonApplication.class, args);
    }

}
