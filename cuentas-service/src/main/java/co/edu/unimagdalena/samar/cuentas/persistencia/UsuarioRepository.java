package co.edu.unimagdalena.samar.cuentas.persistencia;

import co.edu.unimagdalena.samar.cuentas.modelo.Rol;
import co.edu.unimagdalena.samar.cuentas.modelo.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

  Optional<Usuario> findByCorreoIgnoreCase(String correo);

  boolean existsByCorreoIgnoreCase(String correo);

  boolean existsByRol(Rol rol);
}
