package co.edu.unimagdalena.samar.cuentas.integracion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Implementación de desarrollo: escribe el correo en el log. Se reemplaza por SMTP real. */
@Component
class LogCorreoClient implements CorreoClient {

  private static final Logger log = LoggerFactory.getLogger(LogCorreoClient.class);

  @Override
  public void enviar(String destinatario, String asunto, String cuerpo) {
    log.info("[CORREO] para={} asunto='{}' cuerpo='{}'", destinatario, asunto, cuerpo);
  }
}
