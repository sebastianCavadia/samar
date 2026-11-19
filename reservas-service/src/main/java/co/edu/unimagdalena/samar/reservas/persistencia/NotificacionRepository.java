package co.edu.unimagdalena.samar.reservas.persistencia;

import co.edu.unimagdalena.samar.reservas.modelo.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

  List<Notificacion> findTop20ByIdUsuarioOrderByCreadoEnDescIdDesc(Long idUsuario);

  long countByIdUsuarioAndLeidaFalse(Long idUsuario);

  @Modifying
  @Query("update Notificacion n set n.leida = true where n.idUsuario = :usuario and n.leida = false")
  int marcarTodasLeidas(@Param("usuario") Long usuario);
}
