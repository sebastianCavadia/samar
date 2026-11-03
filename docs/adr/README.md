# Registro de decisiones arquitectónicas (ADR)

ADR-01 a ADR-04 vienen de los entregables 1 y 2 (RUP, PostgreSQL, REST, microservicios).
Estas son las decisiones que se tomaron al empezar la construcción.

## ADR-05: Monorepo con un proyecto Maven por microservicio

- **Estado:** Aceptada.
- **Contexto:** Cada microservicio debe ser un proyecto independiente (se compila, despliega y escala
  solo). El equipo tiene 5 personas y un solo GitHub.
- **Decisión:** Un repositorio con una carpeta por servicio. Cada carpeta tiene su propio `pom.xml`,
  `Dockerfile`, base de datos y migraciones. El POM raíz solo fija versiones y permite `mvn verify`
  global. **No hay librerías compartidas entre servicios**: duplicar un DTO pequeño es preferible a
  acoplar despliegues.
- **Consecuencia:** Un solo lugar para issues y PRs. Cada servicio se puede construir aislado
  (`mvn -pl oferta-service -am package`).

## ADR-06: Una base y un usuario de PostgreSQL por servicio, en un solo contenedor

- **Estado:** Aceptada.
- **Contexto:** El entregable 2 dice que cada servicio es dueño de sus datos. Hay que cuidar el
  presupuesto (USD 20.000) y los recursos de los portátiles del equipo.
- **Decisión:** Un contenedor Postgres con `samar_cuentas`, `samar_oferta`, `samar_reservas` y
  `samar_recomendaciones`, cada una con su propio usuario. Las referencias a datos de otro servicio
  (`id_turista`, `id_prestador`, `id_servicio`...) se guardan como números **sin FOREIGN KEY**.
- **Consecuencia:** El aislamiento lo impone la base de datos, no la disciplina. En producción cada
  base puede moverse a su propio servidor sin cambiar código. El diagrama ER se divide en cuatro.

## ADR-07: La disponibilidad pertenece a reservas-service

- **Estado:** Aceptada.
- **Contexto:** Descontar cupos y crear la reserva deben ocurrir juntos. Si vivieran en servicios
  distintos haría falta una transacción distribuida (sagas), que es demasiado para 6 meses.
- **Decisión:** `disponibilidad` vive en la base de reservas. Los cupos se ocupan con un único
  `UPDATE ... SET cupos_reservados = cupos_reservados + n WHERE ... AND cupos_reservados + n <= cupos_totales`:
  la condición y el incremento son atómicos en PostgreSQL, sin bloqueos explícitos ni reintentos.
  Como respaldo, `CHECK (cupos_reservados <= cupos_totales)`. Las ediciones del prestador usan
  bloqueo optimista (`version`). Al reservar se copian el nombre y el precio del servicio (RF047).
- **Consecuencia:** Verificado con 40 reservas simultáneas sobre 5 cupos: exactamente 5 creadas y
  35 rechazadas con 409, sin sobreventa (`pruebas/e2e-reservas.sh`). Las reservas antiguas se
  muestran aunque oferta esté caído.

## ADR-08: Dependencias entre servicios en una sola dirección

- **Estado:** Aceptada. Corrige los ciclos del diagrama de paquetes del entregable 2.
- **Decisión:** `recomendaciones → reservas → oferta → cuentas`. Además, recomendaciones y reservas
  pueden llamar directamente a cuentas. Nunca en sentido contrario. Si un servicio "de abajo"
  necesita reaccionar a algo "de arriba", se usará un evento o un job, no una llamada REST de vuelta.
- **Consecuencia:** Un fallo en recomendaciones (IA) nunca tumba la reserva ni el catálogo. Todo
  cliente REST interno tiene timeouts.

## ADR-09: Autenticación centralizada en el gateway con JWT

- **Estado:** Aceptada.
- **Decisión:** `cuentas-service` firma el JWT (HS256; claims `sub` = id, `rol`). El gateway lo
  valida, **borra** cualquier `X-Usuario-*` que mande el cliente y reenvía la identidad en esas
  cabeceras. Los servicios no publican puertos, solo se alcanzan por el gateway o la red interna.
  La autorización (qué rol puede hacer qué) se decide en la capa servicio de cada microservicio.
- **Consecuencia:** Un solo punto de validación. El secreto se pasa por la variable `JWT_SECRET`
  y nunca va en el repositorio en producción.

## ADR-10: RF085 se cumple impidiendo publicar, no filtrando en cada búsqueda

- **Estado:** Aceptada.
- **Contexto:** RF085 pide no mostrar ofertas de prestadores sin validar. Filtrar en cada búsqueda
  obligaría a oferta a consultar cuentas por cada resultado (lento y acoplado), o a recibir avisos
  de cuentas, lo que invertiría la dirección de dependencias (ADR-08).
- **Decisión:** Al registrar un servicio, oferta consulta a cuentas (`/internal/usuarios/{id}`) y
  solo lo acepta si el prestador está validado. Si cuentas no responde (timeout de 2-3 s) se
  rechaza con 503: **falla cerrada**, es mejor no publicar que publicar sin validar.
- **Consecuencia:** El catálogo nunca llama a cuentas: buscar sigue funcionando aunque cuentas esté
  caído (verificado apagando el contenedor). Editar o desactivar tampoco dependen de cuentas.
  Pendiente: si un administrador desactiva a un prestador ya validado, sus ofertas deberán
  ocultarse con un job de sincronización o un evento.

## ADR-12: Reservas consulta otros servicios fuera de la transacción y se degrada con control

- **Estado:** Aceptada.
- **Contexto:** Crear una reserva necesita datos de oferta (precio, dueño, capacidad, edad mínima)
  y de cuentas (nombre, fecha de nacimiento). Si esas llamadas ocurren dentro de la transacción,
  cada reserva retiene una conexión de la base mientras espera la red, y en temporada alta el pool
  de conexiones se agota.
- **Decisión:** Primero las llamadas remotas (con timeouts de 2 y 3 s), luego una transacción corta
  (`TransactionTemplate`) que solo ocupa cupos y guarda. Si cuentas no responde se sigue reservando
  sin el nombre del turista, salvo que el servicio tenga edad mínima (no hay forma segura de
  validarla: 503). Si oferta no responde, 503: sin precio confiable no se reserva.
  Cada cambio de estado incrementa la versión de la reserva (`OPTIMISTIC_FORCE_INCREMENT`), así
  que confirmar y expirar al mismo tiempo nunca se pisan.
- **Consecuencia:** Verificado congelando cada servicio en Docker. La consulta pública de cupos
  no depende de nadie.

## ADR-11: Las calificaciones entran por reservas-service

- **Estado:** Propuesta (se implementa con reservas).
- **Contexto:** RF076 exige que solo califique quien tenga una reserva COMPLETADA. Ese dato vive en
  reservas, y reservas ya depende de oferta: si oferta le preguntara a reservas se formaría un ciclo.
- **Decisión:** El turista califica a través de reservas-service, que comprueba la reserva y luego
  registra la calificación en oferta (`POST /internal/calificaciones`). Oferta guarda la calificación
  y recalcula el promedio (RF080). Así la dependencia sigue yendo de reservas hacia oferta.
