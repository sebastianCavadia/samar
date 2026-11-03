package co.edu.unimagdalena.samar.recomendaciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RecomendacionesApplication {

  public static void main(String[] args) {
    SpringApplication.run(RecomendacionesApplication.class, args);
  }
}
