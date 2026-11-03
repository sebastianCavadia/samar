package co.edu.unimagdalena.samar.oferta.persistencia;

import co.edu.unimagdalena.samar.oferta.modelo.ServicioTuristico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ServicioTuristicoRepository
    extends JpaRepository<ServicioTuristico, Long>, JpaSpecificationExecutor<ServicioTuristico> {

  List<ServicioTuristico> findByIdPrestadorOrderByCreadoEnDesc(Long idPrestador);

  Optional<ServicioTuristico> findByIdAndActivoTrue(Long id);
}
