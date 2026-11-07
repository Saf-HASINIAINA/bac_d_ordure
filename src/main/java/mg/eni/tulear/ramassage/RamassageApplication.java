package mg.eni.tulear.ramassage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RamassageApplication {

    public static void main(String[] args) {
        SpringApplication.run(RamassageApplication.class, args);
    }
}
