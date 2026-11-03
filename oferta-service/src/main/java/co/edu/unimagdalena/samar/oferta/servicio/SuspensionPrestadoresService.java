package co.edu.unimagdalena.samar.oferta.servicio;

import co.edu.unimagdalena.samar.oferta.persistencia.ServicioTuristicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * ADR-13: deja el catálogo coherente con el estado de las cuentas en cuentas-service.
 * Recibe la lista completa de prestadores suspendidos, así que también reactiva a los que ya no lo están.
 */
@Service
public class SuspensionPrestadoresService {

  public record Resultado(int suspendidos, int reactivados) {
  }

  private final ServicioTuristicoRepository servicios;

  public SuspensionPrestadoresService(ServicioTuristicoRepository servicios) {
    this.servicios = servicios;
  }

  @Transactional
  public Resultado aplicar(Set<Long> prestadoresSuspendidos) {
    if (prestadoresSuspendidos.isEmpty()) {
      return new Resultado(0, servicios.reactivarTodos()); // NOT IN () vacío no es SQL válido
    }
    return new Resultado(servicios.suspenderPrestadores(prestadoresSuspendidos),
        servicios.reactivarExcepto(prestadoresSuspendidos));
  }
}
