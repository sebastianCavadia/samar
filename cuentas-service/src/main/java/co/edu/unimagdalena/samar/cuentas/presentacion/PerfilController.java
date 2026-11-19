package co.edu.unimagdalena.samar.cuentas.presentacion;

import co.edu.unimagdalena.samar.cuentas.servicio.PerfilService;
import co.edu.unimagdalena.samar.cuentas.servicio.UsuarioService;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.Identidad;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.Perfil;
import co.edu.unimagdalena.samar.cuentas.servicio.dto.UsuarioResponse;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/** RF008 y RF009: el usuario consulta y edita su propia cuenta. */
@RestController
@RequestMapping("/api/cuentas/perfil")
public class PerfilController {

  private final UsuarioService usuarioService;
  private final PerfilService perfilService;

  public PerfilController(UsuarioService usuarioService, PerfilService perfilService) {
    this.usuarioService = usuarioService;
    this.perfilService = perfilService;
  }

  @GetMapping
  public UsuarioResponse miPerfil(Identidad actor) {
    return usuarioService.perfil(actor);
  }

  @PutMapping
  public UsuarioResponse editar(Identidad actor, @Valid @RequestBody Perfil.Editar req) {
    return perfilService.editar(actor, req);
  }

  @PutMapping("/password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cambiarPassword(Identidad actor, @Valid @RequestBody Perfil.CambiarPassword req) {
    perfilService.cambiarPassword(actor, req);
  }

  @PutMapping(path = "/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void subirFoto(Identidad actor, @RequestParam("archivo") MultipartFile archivo) throws IOException {
    perfilService.guardarFoto(actor, archivo.getBytes());
  }

  /** Exige sesión: el frontend la descarga con el token y la muestra como blob. */
  @GetMapping("/foto")
  public ResponseEntity<byte[]> foto(Identidad actor) {
    Perfil.Foto foto = perfilService.foto(actor);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(foto.tipo()))
        .cacheControl(CacheControl.noCache().cachePrivate())
        .body(foto.contenido());
  }

  @DeleteMapping("/foto")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminarFoto(Identidad actor) {
    perfilService.eliminarFoto(actor);
  }
}
