package imitationgame.aibotservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AiBotServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiBotServiceApplication.class, args);
    }
}
