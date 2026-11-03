package co.edu.unimagdalena.samar.oferta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OfertaApplication {

  public static void main(String[] args) {
    SpringApplication.run(OfertaApplication.class, args);
  }
}
