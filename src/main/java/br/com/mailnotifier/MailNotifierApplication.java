package br.com.mailnotifier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MailNotifierApplication {

    public static void main(String[] args) {
        SpringApplication.run(MailNotifierApplication.class, args);
    }

}
