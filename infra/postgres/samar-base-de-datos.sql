-- =====================================================================================
-- SAMAR · Script de base de datos (PostgreSQL 16)
-- Crea los 4 usuarios, las 4 bases (una por microservicio, ADR-06) y todo su esquema.
--
-- Uso, como superusuario:
--   psql -U postgres -f infra/postgres/samar-base-de-datos.sql
--
-- No es obligatorio: con Docker (docker compose up) las bases se crean solas
-- (infra/postgres/init) y cada servicio aplica su esquema con Flyway al arrancar.
-- Este script deja el mismo resultado, incluido el historial de Flyway, para que los
-- servicios arranquen sobre él sin volver a crear las tablas.
--
-- Contraseñas de desarrollo: cámbialas en producción (y en las variables DB_PASSWORD).
-- Generado desde las migraciones aplicadas (pg_dump --schema-only).
-- =====================================================================================

CREATE USER cuentas WITH PASSWORD 'cuentas';
CREATE USER oferta WITH PASSWORD 'oferta';
CREATE USER reservas WITH PASSWORD 'reservas';
CREATE USER recomendaciones WITH PASSWORD 'recomendaciones';

CREATE DATABASE samar_cuentas OWNER cuentas;
CREATE DATABASE samar_oferta OWNER oferta;
CREATE DATABASE samar_reservas OWNER reservas;
CREATE DATABASE samar_recomendaciones OWNER recomendaciones;

REVOKE CONNECT ON DATABASE samar_cuentas FROM PUBLIC;
REVOKE CONNECT ON DATABASE samar_oferta FROM PUBLIC;
REVOKE CONNECT ON DATABASE samar_reservas FROM PUBLIC;
REVOKE CONNECT ON DATABASE samar_recomendaciones FROM PUBLIC;

-- =====================================================================================
-- Base samar_cuentas (cuentas-service)
-- =====================================================================================
\connect samar_cuentas

CREATE TABLE public.administrador (
    id_usuario bigint NOT NULL,
    area_responsable character varying(120) NOT NULL
);

ALTER TABLE public.administrador OWNER TO cuentas;

CREATE TABLE public.agencia_idioma (
    id_usuario bigint NOT NULL,
    idioma character varying(40) NOT NULL
);

ALTER TABLE public.agencia_idioma OWNER TO cuentas;

CREATE TABLE public.bitacora_admin (
    id bigint NOT NULL,
    id_administrador bigint NOT NULL,
    area_responsable character varying(120),
    id_usuario bigint NOT NULL,
    accion character varying(30) NOT NULL,
    detalle character varying(400),
    fecha timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT bitacora_admin_accion_check CHECK (((accion)::text = ANY ((ARRAY['VALIDAR_PRESTADOR'::character varying, 'RECHAZAR_PRESTADOR'::character varying, 'CAMBIAR_ESTADO'::character varying, 'EDITAR_DATOS'::character varying, 'CREAR_ADMINISTRADOR'::character varying])::text[])))
);

ALTER TABLE public.bitacora_admin OWNER TO cuentas;

CREATE SEQUENCE public.bitacora_admin_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.bitacora_admin_id_seq OWNER TO cuentas;

ALTER SEQUENCE public.bitacora_admin_id_seq OWNED BY public.bitacora_admin.id;

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);

ALTER TABLE public.flyway_schema_history OWNER TO cuentas;

CREATE TABLE public.foto_perfil (
    id_usuario bigint NOT NULL,
    contenido bytea NOT NULL,
    tipo character varying(20) NOT NULL,
    actualizada_en timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT foto_perfil_tipo_check CHECK (((tipo)::text = ANY ((ARRAY['image/jpeg'::character varying, 'image/png'::character varying])::text[])))
);

ALTER TABLE public.foto_perfil OWNER TO cuentas;

CREATE TABLE public.prestador_turistico (
    id_usuario bigint NOT NULL,
    tipo_prestador character varying(30) NOT NULL,
    nit character varying(40) NOT NULL,
    rnt character varying(40) NOT NULL,
    direccion character varying(200) NOT NULL,
    zona character varying(80),
    latitud double precision,
    longitud double precision,
    validado_en timestamp with time zone,
    estado_validacion character varying(20) DEFAULT 'PENDIENTE'::character varying NOT NULL,
    motivo_rechazo character varying(300),
    CONSTRAINT prestador_turistico_estado_validacion_check CHECK (((estado_validacion)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'VALIDADO'::character varying, 'RECHAZADO'::character varying])::text[]))),
    CONSTRAINT prestador_turistico_tipo_prestador_check CHECK (((tipo_prestador)::text = ANY ((ARRAY['HOTEL'::character varying, 'RESTAURANTE'::character varying, 'AGENCIA_TURISTICA'::character varying])::text[])))
);

ALTER TABLE public.prestador_turistico OWNER TO cuentas;

CREATE TABLE public.turista (
    id_usuario bigint NOT NULL,
    tipo_documento character varying(20) NOT NULL,
    numero_documento character varying(40) NOT NULL,
    nacionalidad character varying(80),
    pais_residencia character varying(80),
    fecha_nacimiento date,
    discapacidad boolean DEFAULT false NOT NULL,
    CONSTRAINT turista_tipo_documento_check CHECK (((tipo_documento)::text = ANY ((ARRAY['CC'::character varying, 'CE'::character varying, 'PASAPORTE'::character varying])::text[])))
);

ALTER TABLE public.turista OWNER TO cuentas;

CREATE TABLE public.usuario (
    id bigint NOT NULL,
    nombre character varying(120) NOT NULL,
    correo character varying(150) NOT NULL,
    password_hash character varying(100) NOT NULL,
    telefono character varying(40),
    idioma character varying(10) DEFAULT 'es'::character varying NOT NULL,
    rol character varying(20) NOT NULL,
    estado character varying(20) DEFAULT 'ACTIVO'::character varying NOT NULL,
    intentos_fallidos integer DEFAULT 0 NOT NULL,
    bloqueado_hasta timestamp with time zone,
    ultimo_acceso timestamp with time zone,
    creado_en timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT usuario_estado_check CHECK (((estado)::text = ANY ((ARRAY['ACTIVO'::character varying, 'INACTIVO'::character varying, 'BLOQUEADO'::character varying])::text[]))),
    CONSTRAINT usuario_rol_check CHECK (((rol)::text = ANY ((ARRAY['TURISTA'::character varying, 'PRESTADOR'::character varying, 'ADMINISTRADOR'::character varying])::text[])))
);

ALTER TABLE public.usuario OWNER TO cuentas;

CREATE SEQUENCE public.usuario_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.usuario_id_seq OWNER TO cuentas;

ALTER SEQUENCE public.usuario_id_seq OWNED BY public.usuario.id;

ALTER TABLE ONLY public.bitacora_admin ALTER COLUMN id SET DEFAULT nextval('public.bitacora_admin_id_seq'::regclass);

ALTER TABLE ONLY public.usuario ALTER COLUMN id SET DEFAULT nextval('public.usuario_id_seq'::regclass);

ALTER TABLE ONLY public.administrador
    ADD CONSTRAINT administrador_pkey PRIMARY KEY (id_usuario);

ALTER TABLE ONLY public.agencia_idioma
    ADD CONSTRAINT agencia_idioma_pkey PRIMARY KEY (id_usuario, idioma);

ALTER TABLE ONLY public.bitacora_admin
    ADD CONSTRAINT bitacora_admin_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);

ALTER TABLE ONLY public.foto_perfil
    ADD CONSTRAINT foto_perfil_pkey PRIMARY KEY (id_usuario);

ALTER TABLE ONLY public.prestador_turistico
    ADD CONSTRAINT prestador_turistico_nit_key UNIQUE (nit);

ALTER TABLE ONLY public.prestador_turistico
    ADD CONSTRAINT prestador_turistico_pkey PRIMARY KEY (id_usuario);

ALTER TABLE ONLY public.prestador_turistico
    ADD CONSTRAINT prestador_turistico_rnt_key UNIQUE (rnt);

ALTER TABLE ONLY public.turista
    ADD CONSTRAINT turista_numero_documento_key UNIQUE (numero_documento);

ALTER TABLE ONLY public.turista
    ADD CONSTRAINT turista_pkey PRIMARY KEY (id_usuario);

ALTER TABLE ONLY public.usuario
    ADD CONSTRAINT usuario_correo_key UNIQUE (correo);

ALTER TABLE ONLY public.usuario
    ADD CONSTRAINT usuario_pkey PRIMARY KEY (id);

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);

CREATE INDEX idx_bitacora_fecha ON public.bitacora_admin USING btree (fecha DESC);

CREATE INDEX idx_bitacora_usuario ON public.bitacora_admin USING btree (id_usuario, fecha DESC);

CREATE INDEX idx_prestador_validacion ON public.prestador_turistico USING btree (estado_validacion);

CREATE INDEX idx_usuario_busqueda ON public.usuario USING btree (rol, estado);

ALTER TABLE ONLY public.administrador
    ADD CONSTRAINT administrador_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES public.usuario(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.agencia_idioma
    ADD CONSTRAINT agencia_idioma_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES public.prestador_turistico(id_usuario) ON DELETE CASCADE;

ALTER TABLE ONLY public.bitacora_admin
    ADD CONSTRAINT bitacora_admin_id_administrador_fkey FOREIGN KEY (id_administrador) REFERENCES public.usuario(id);

ALTER TABLE ONLY public.bitacora_admin
    ADD CONSTRAINT bitacora_admin_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES public.usuario(id);

ALTER TABLE ONLY public.foto_perfil
    ADD CONSTRAINT foto_perfil_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES public.usuario(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.prestador_turistico
    ADD CONSTRAINT prestador_turistico_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES public.usuario(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.turista
    ADD CONSTRAINT turista_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES public.usuario(id) ON DELETE CASCADE;

-- Historial de Flyway: los servicios ven el esquema como ya migrado.
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (1, '1', 'esquema cuentas', 'SQL', 'V1__esquema_cuentas.sql', 1918927422, 'cuentas', '2026-10-27 02:37:56.783876', 377, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (2, '2', 'administracion', 'SQL', 'V2__administracion.sql', -1389495010, 'cuentas', '2026-11-03 19:09:40.601126', 248, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (3, '3', 'perfil', 'SQL', 'V3__perfil.sql', -922432498, 'cuentas', '2026-11-19 01:35:10.805498', 473, true);

-- =====================================================================================
-- Base samar_oferta (oferta-service)
-- =====================================================================================
\connect samar_oferta

CREATE TABLE public.actividad_turistica (
    id_servicio bigint NOT NULL,
    duracion_horas numeric(4,1),
    hora_inicio time without time zone,
    dificultad character varying(20),
    edad_minima integer,
    CONSTRAINT actividad_turistica_dificultad_check CHECK (((dificultad)::text = ANY ((ARRAY['BAJA'::character varying, 'MEDIA'::character varying, 'ALTA'::character varying])::text[])))
);

ALTER TABLE public.actividad_turistica OWNER TO oferta;

CREATE TABLE public.calificacion (
    id bigint NOT NULL,
    id_servicio bigint NOT NULL,
    id_turista bigint NOT NULL,
    id_reserva bigint NOT NULL,
    puntuacion smallint NOT NULL,
    comentario text,
    respuesta_prestador text,
    fecha timestamp with time zone DEFAULT now() NOT NULL,
    nombre_turista character varying(120),
    fecha_respuesta timestamp with time zone,
    CONSTRAINT calificacion_puntuacion_check CHECK (((puntuacion >= 1) AND (puntuacion <= 5))),
    CONSTRAINT ck_calificacion_respuesta CHECK (((respuesta_prestador IS NULL) = (fecha_respuesta IS NULL)))
);

ALTER TABLE public.calificacion OWNER TO oferta;

CREATE SEQUENCE public.calificacion_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.calificacion_id_seq OWNER TO oferta;

ALTER SEQUENCE public.calificacion_id_seq OWNED BY public.calificacion.id;

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);

ALTER TABLE public.flyway_schema_history OWNER TO oferta;

CREATE TABLE public.hospedaje (
    id_servicio bigint NOT NULL,
    tipo_habitacion character varying(60),
    capacidad_personas integer NOT NULL,
    hora_check_in time without time zone,
    hora_check_out time without time zone,
    CONSTRAINT hospedaje_capacidad_personas_check CHECK ((capacidad_personas > 0))
);

ALTER TABLE public.hospedaje OWNER TO oferta;

CREATE TABLE public.imagen_archivo (
    id bigint NOT NULL,
    id_prestador bigint NOT NULL,
    contenido bytea NOT NULL,
    tipo character varying(20) NOT NULL,
    tamano integer NOT NULL,
    creado_en timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT imagen_archivo_tamano_check CHECK ((tamano > 0)),
    CONSTRAINT imagen_archivo_tipo_check CHECK (((tipo)::text = ANY ((ARRAY['image/jpeg'::character varying, 'image/png'::character varying])::text[])))
);

ALTER TABLE public.imagen_archivo OWNER TO oferta;

CREATE SEQUENCE public.imagen_archivo_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.imagen_archivo_id_seq OWNER TO oferta;

ALTER SEQUENCE public.imagen_archivo_id_seq OWNED BY public.imagen_archivo.id;

CREATE TABLE public.imagen_servicio (
    id bigint NOT NULL,
    id_servicio bigint NOT NULL,
    url character varying(500) NOT NULL,
    descripcion character varying(200) NOT NULL
);

ALTER TABLE public.imagen_servicio OWNER TO oferta;

CREATE SEQUENCE public.imagen_servicio_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.imagen_servicio_id_seq OWNER TO oferta;

ALTER SEQUENCE public.imagen_servicio_id_seq OWNED BY public.imagen_servicio.id;

CREATE TABLE public.servicio_categoria (
    id_servicio bigint NOT NULL,
    categoria character varying(30) NOT NULL,
    CONSTRAINT servicio_categoria_categoria_check CHECK (((categoria)::text = ANY ((ARRAY['PLAYA'::character varying, 'NATURALEZA'::character varying, 'CULTURA'::character varying, 'AVENTURA'::character varying, 'GASTRONOMIA'::character varying, 'DESCANSO'::character varying])::text[])))
);

ALTER TABLE public.servicio_categoria OWNER TO oferta;

CREATE TABLE public.servicio_gastronomico (
    id_servicio bigint NOT NULL,
    tipo_comida character varying(60),
    aforo integer,
    hora_apertura time without time zone,
    hora_cierre time without time zone
);

ALTER TABLE public.servicio_gastronomico OWNER TO oferta;

CREATE TABLE public.servicio_gastronomico_dieta (
    id_servicio bigint NOT NULL,
    tipo_dieta character varying(20) NOT NULL,
    CONSTRAINT servicio_gastronomico_dieta_tipo_dieta_check CHECK (((tipo_dieta)::text = ANY ((ARRAY['VEGETARIANA'::character varying, 'VEGANA'::character varying, 'CELIACA'::character varying, 'KOSHER'::character varying, 'HALAL'::character varying])::text[])))
);

ALTER TABLE public.servicio_gastronomico_dieta OWNER TO oferta;

CREATE TABLE public.servicio_turistico (
    id bigint NOT NULL,
    id_prestador bigint NOT NULL,
    tipo character varying(20) NOT NULL,
    nombre character varying(150) NOT NULL,
    descripcion text,
    modalidad character varying(20) DEFAULT 'RESERVA'::character varying NOT NULL,
    precio_base numeric(12,2) NOT NULL,
    moneda character varying(3) DEFAULT 'COP'::character varying NOT NULL,
    direccion character varying(200),
    zona character varying(80),
    latitud double precision,
    longitud double precision,
    accesible boolean DEFAULT false NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    calificacion_promedio numeric(2,1),
    total_calificaciones integer DEFAULT 0 NOT NULL,
    creado_en timestamp with time zone DEFAULT now() NOT NULL,
    prestador_suspendido boolean DEFAULT false NOT NULL,
    CONSTRAINT servicio_turistico_modalidad_check CHECK (((modalidad)::text = ANY ((ARRAY['RESERVA'::character varying, 'WALK_IN'::character varying])::text[]))),
    CONSTRAINT servicio_turistico_precio_base_check CHECK ((precio_base >= (0)::numeric)),
    CONSTRAINT servicio_turistico_tipo_check CHECK (((tipo)::text = ANY ((ARRAY['HOSPEDAJE'::character varying, 'ACTIVIDAD'::character varying, 'GASTRONOMICO'::character varying])::text[])))
);

ALTER TABLE public.servicio_turistico OWNER TO oferta;

CREATE SEQUENCE public.servicio_turistico_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.servicio_turistico_id_seq OWNER TO oferta;

ALTER SEQUENCE public.servicio_turistico_id_seq OWNED BY public.servicio_turistico.id;

ALTER TABLE ONLY public.calificacion ALTER COLUMN id SET DEFAULT nextval('public.calificacion_id_seq'::regclass);

ALTER TABLE ONLY public.imagen_archivo ALTER COLUMN id SET DEFAULT nextval('public.imagen_archivo_id_seq'::regclass);

ALTER TABLE ONLY public.imagen_servicio ALTER COLUMN id SET DEFAULT nextval('public.imagen_servicio_id_seq'::regclass);

ALTER TABLE ONLY public.servicio_turistico ALTER COLUMN id SET DEFAULT nextval('public.servicio_turistico_id_seq'::regclass);

ALTER TABLE ONLY public.actividad_turistica
    ADD CONSTRAINT actividad_turistica_pkey PRIMARY KEY (id_servicio);

ALTER TABLE ONLY public.calificacion
    ADD CONSTRAINT calificacion_id_reserva_key UNIQUE (id_reserva);

ALTER TABLE ONLY public.calificacion
    ADD CONSTRAINT calificacion_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);

ALTER TABLE ONLY public.hospedaje
    ADD CONSTRAINT hospedaje_pkey PRIMARY KEY (id_servicio);

ALTER TABLE ONLY public.imagen_archivo
    ADD CONSTRAINT imagen_archivo_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.imagen_servicio
    ADD CONSTRAINT imagen_servicio_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.servicio_categoria
    ADD CONSTRAINT servicio_categoria_pkey PRIMARY KEY (id_servicio, categoria);

ALTER TABLE ONLY public.servicio_gastronomico_dieta
    ADD CONSTRAINT servicio_gastronomico_dieta_pkey PRIMARY KEY (id_servicio, tipo_dieta);

ALTER TABLE ONLY public.servicio_gastronomico
    ADD CONSTRAINT servicio_gastronomico_pkey PRIMARY KEY (id_servicio);

ALTER TABLE ONLY public.servicio_turistico
    ADD CONSTRAINT servicio_turistico_pkey PRIMARY KEY (id);

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);

CREATE INDEX idx_calificacion_fecha ON public.calificacion USING btree (id_servicio, fecha DESC);

CREATE INDEX idx_calificacion_servicio ON public.calificacion USING btree (id_servicio, puntuacion);

CREATE INDEX idx_imagen_archivo_prestador ON public.imagen_archivo USING btree (id_prestador);

CREATE INDEX idx_imagen_servicio_url ON public.imagen_servicio USING btree (url);

CREATE INDEX idx_servicio_busqueda ON public.servicio_turistico USING btree (tipo, zona, precio_base) WHERE activo;

CREATE INDEX idx_servicio_prestador ON public.servicio_turistico USING btree (id_prestador);

CREATE INDEX idx_servicio_suspendido ON public.servicio_turistico USING btree (prestador_suspendido) WHERE prestador_suspendido;

CREATE INDEX idx_servicio_texto ON public.servicio_turistico USING gin (to_tsvector('spanish'::regconfig, (((nombre)::text || ' '::text) || COALESCE(descripcion, ''::text))));

ALTER TABLE ONLY public.actividad_turistica
    ADD CONSTRAINT actividad_turistica_id_servicio_fkey FOREIGN KEY (id_servicio) REFERENCES public.servicio_turistico(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.calificacion
    ADD CONSTRAINT calificacion_id_servicio_fkey FOREIGN KEY (id_servicio) REFERENCES public.servicio_turistico(id);

ALTER TABLE ONLY public.hospedaje
    ADD CONSTRAINT hospedaje_id_servicio_fkey FOREIGN KEY (id_servicio) REFERENCES public.servicio_turistico(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.imagen_servicio
    ADD CONSTRAINT imagen_servicio_id_servicio_fkey FOREIGN KEY (id_servicio) REFERENCES public.servicio_turistico(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.servicio_categoria
    ADD CONSTRAINT servicio_categoria_id_servicio_fkey FOREIGN KEY (id_servicio) REFERENCES public.servicio_turistico(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.servicio_gastronomico_dieta
    ADD CONSTRAINT servicio_gastronomico_dieta_id_servicio_fkey FOREIGN KEY (id_servicio) REFERENCES public.servicio_gastronomico(id_servicio) ON DELETE CASCADE;

ALTER TABLE ONLY public.servicio_gastronomico
    ADD CONSTRAINT servicio_gastronomico_id_servicio_fkey FOREIGN KEY (id_servicio) REFERENCES public.servicio_turistico(id) ON DELETE CASCADE;

-- Historial de Flyway: los servicios ven el esquema como ya migrado.
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (1, '1', 'esquema oferta', 'SQL', 'V1__esquema_oferta.sql', -35140216, 'oferta', '2026-10-27 02:37:55.728339', 577, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (2, '2', 'prestador suspendido', 'SQL', 'V2__prestador_suspendido.sql', -1927592143, 'oferta', '2026-11-03 19:09:40.622551', 98, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (3, '3', 'calificaciones', 'SQL', 'V3__calificaciones.sql', 936718328, 'oferta', '2026-11-03 23:05:16.469426', 837, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (4, '4', 'imagenes', 'SQL', 'V4__imagenes.sql', 461919815, 'oferta', '2026-11-19 01:35:17.465146', 456, true);

-- =====================================================================================
-- Base samar_reservas (reservas-service)
-- =====================================================================================
\connect samar_reservas

CREATE TABLE public.bitacora_reserva (
    id bigint NOT NULL,
    id_reserva bigint NOT NULL,
    estado_anterior character varying(20),
    estado_nuevo character varying(20) NOT NULL,
    id_usuario bigint,
    fecha timestamp with time zone DEFAULT now() NOT NULL,
    detalle character varying(200)
);

ALTER TABLE public.bitacora_reserva OWNER TO reservas;

CREATE SEQUENCE public.bitacora_reserva_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.bitacora_reserva_id_seq OWNER TO reservas;

ALTER SEQUENCE public.bitacora_reserva_id_seq OWNED BY public.bitacora_reserva.id;

CREATE TABLE public.detalle_reserva (
    id bigint NOT NULL,
    id_reserva bigint NOT NULL,
    id_disponibilidad bigint NOT NULL,
    id_servicio bigint NOT NULL,
    id_prestador bigint NOT NULL,
    nombre_servicio character varying(150) NOT NULL,
    cantidad integer NOT NULL,
    precio_unitario numeric(12,2) NOT NULL,
    subtotal numeric(12,2) NOT NULL,
    tipo_servicio character varying(20),
    CONSTRAINT detalle_reserva_cantidad_check CHECK ((cantidad > 0))
);

ALTER TABLE public.detalle_reserva OWNER TO reservas;

CREATE SEQUENCE public.detalle_reserva_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.detalle_reserva_id_seq OWNER TO reservas;

ALTER SEQUENCE public.detalle_reserva_id_seq OWNED BY public.detalle_reserva.id;

CREATE TABLE public.disponibilidad (
    id bigint NOT NULL,
    id_servicio bigint NOT NULL,
    fecha date NOT NULL,
    hora time without time zone,
    cupos_totales integer NOT NULL,
    cupos_reservados integer DEFAULT 0 NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT disponibilidad_check CHECK (((cupos_reservados >= 0) AND (cupos_reservados <= cupos_totales))),
    CONSTRAINT disponibilidad_cupos_totales_check CHECK ((cupos_totales >= 0))
);

ALTER TABLE public.disponibilidad OWNER TO reservas;

CREATE SEQUENCE public.disponibilidad_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.disponibilidad_id_seq OWNER TO reservas;

ALTER SEQUENCE public.disponibilidad_id_seq OWNED BY public.disponibilidad.id;

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);

ALTER TABLE public.flyway_schema_history OWNER TO reservas;

CREATE TABLE public.notificacion (
    id bigint NOT NULL,
    id_usuario bigint NOT NULL,
    asunto character varying(120) NOT NULL,
    mensaje character varying(500) NOT NULL,
    leida boolean DEFAULT false NOT NULL,
    creado_en timestamp with time zone DEFAULT now() NOT NULL
);

ALTER TABLE public.notificacion OWNER TO reservas;

CREATE SEQUENCE public.notificacion_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.notificacion_id_seq OWNER TO reservas;

ALTER SEQUENCE public.notificacion_id_seq OWNED BY public.notificacion.id;

CREATE TABLE public.pago (
    id bigint NOT NULL,
    id_reserva bigint NOT NULL,
    referencia character varying(80) NOT NULL,
    medio character varying(20) NOT NULL,
    estado character varying(20) NOT NULL,
    monto numeric(12,2) NOT NULL,
    creado_en timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT pago_estado_check CHECK (((estado)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'APROBADO'::character varying, 'RECHAZADO'::character varying, 'REEMBOLSADO'::character varying])::text[]))),
    CONSTRAINT pago_medio_check CHECK (((medio)::text = ANY ((ARRAY['PASARELA'::character varying, 'COMPROBANTE'::character varying])::text[])))
);

ALTER TABLE public.pago OWNER TO reservas;

CREATE SEQUENCE public.pago_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.pago_id_seq OWNER TO reservas;

ALTER SEQUENCE public.pago_id_seq OWNED BY public.pago.id;

CREATE TABLE public.reserva (
    id bigint NOT NULL,
    id_turista bigint NOT NULL,
    estado character varying(20) DEFAULT 'PENDIENTE'::character varying NOT NULL,
    total numeric(12,2) NOT NULL,
    moneda character varying(3) DEFAULT 'COP'::character varying NOT NULL,
    motivo character varying(200),
    creado_en timestamp with time zone DEFAULT now() NOT NULL,
    personas integer DEFAULT 1 NOT NULL,
    nombre_turista character varying(120),
    version bigint DEFAULT 0 NOT NULL,
    calificacion smallint,
    calificada_en timestamp with time zone,
    CONSTRAINT ck_reserva_calificada CHECK (((calificacion IS NULL) = (calificada_en IS NULL))),
    CONSTRAINT reserva_calificacion_check CHECK (((calificacion >= 1) AND (calificacion <= 5))),
    CONSTRAINT reserva_estado_check CHECK (((estado)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'CONFIRMADA'::character varying, 'RECHAZADA'::character varying, 'CANCELADA'::character varying, 'COMPLETADA'::character varying])::text[]))),
    CONSTRAINT reserva_personas_check CHECK ((personas > 0)),
    CONSTRAINT reserva_total_check CHECK ((total >= (0)::numeric))
);

ALTER TABLE public.reserva OWNER TO reservas;

CREATE SEQUENCE public.reserva_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.reserva_id_seq OWNER TO reservas;

ALTER SEQUENCE public.reserva_id_seq OWNED BY public.reserva.id;

ALTER TABLE ONLY public.bitacora_reserva ALTER COLUMN id SET DEFAULT nextval('public.bitacora_reserva_id_seq'::regclass);

ALTER TABLE ONLY public.detalle_reserva ALTER COLUMN id SET DEFAULT nextval('public.detalle_reserva_id_seq'::regclass);

ALTER TABLE ONLY public.disponibilidad ALTER COLUMN id SET DEFAULT nextval('public.disponibilidad_id_seq'::regclass);

ALTER TABLE ONLY public.notificacion ALTER COLUMN id SET DEFAULT nextval('public.notificacion_id_seq'::regclass);

ALTER TABLE ONLY public.pago ALTER COLUMN id SET DEFAULT nextval('public.pago_id_seq'::regclass);

ALTER TABLE ONLY public.reserva ALTER COLUMN id SET DEFAULT nextval('public.reserva_id_seq'::regclass);

ALTER TABLE ONLY public.bitacora_reserva
    ADD CONSTRAINT bitacora_reserva_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.detalle_reserva
    ADD CONSTRAINT detalle_reserva_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.disponibilidad
    ADD CONSTRAINT disponibilidad_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);

ALTER TABLE ONLY public.notificacion
    ADD CONSTRAINT notificacion_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.pago
    ADD CONSTRAINT pago_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.pago
    ADD CONSTRAINT pago_referencia_key UNIQUE (referencia);

ALTER TABLE ONLY public.reserva
    ADD CONSTRAINT reserva_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.disponibilidad
    ADD CONSTRAINT uq_disponibilidad UNIQUE NULLS NOT DISTINCT (id_servicio, fecha, hora);

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);

CREATE INDEX idx_bitacora_reserva ON public.bitacora_reserva USING btree (id_reserva);

CREATE INDEX idx_detalle_prestador ON public.detalle_reserva USING btree (id_prestador);

CREATE INDEX idx_detalle_reserva ON public.detalle_reserva USING btree (id_reserva);

CREATE INDEX idx_disponibilidad_servicio ON public.disponibilidad USING btree (id_servicio, fecha);

CREATE INDEX idx_notificacion_no_leidas ON public.notificacion USING btree (id_usuario) WHERE (NOT leida);

CREATE INDEX idx_notificacion_usuario ON public.notificacion USING btree (id_usuario, creado_en DESC);

CREATE INDEX idx_pago_reserva ON public.pago USING btree (id_reserva);

CREATE INDEX idx_reserva_estado ON public.reserva USING btree (estado, creado_en);

CREATE INDEX idx_reserva_turista ON public.reserva USING btree (id_turista, creado_en DESC);

ALTER TABLE ONLY public.bitacora_reserva
    ADD CONSTRAINT bitacora_reserva_id_reserva_fkey FOREIGN KEY (id_reserva) REFERENCES public.reserva(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.detalle_reserva
    ADD CONSTRAINT detalle_reserva_id_disponibilidad_fkey FOREIGN KEY (id_disponibilidad) REFERENCES public.disponibilidad(id);

ALTER TABLE ONLY public.detalle_reserva
    ADD CONSTRAINT detalle_reserva_id_reserva_fkey FOREIGN KEY (id_reserva) REFERENCES public.reserva(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.pago
    ADD CONSTRAINT pago_id_reserva_fkey FOREIGN KEY (id_reserva) REFERENCES public.reserva(id);

-- Historial de Flyway: los servicios ven el esquema como ya migrado.
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (1, '1', 'esquema reservas', 'SQL', 'V1__esquema_reservas.sql', 1480867646, 'reservas', '2026-10-27 02:37:57.320176', 318, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (2, '2', 'reservas flujo completo', 'SQL', 'V2__reservas_flujo_completo.sql', -2033706087, 'reservas', '2026-11-03 18:40:40.963804', 363, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (3, '3', 'calificacion reserva', 'SQL', 'V3__calificacion_reserva.sql', -1228050180, 'reservas', '2026-11-03 23:05:17.069994', 263, true);
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (4, '4', 'notificaciones', 'SQL', 'V4__notificaciones.sql', -1272481225, 'reservas', '2026-11-19 01:35:12.481489', 599, true);

-- =====================================================================================
-- Base samar_recomendaciones (recomendaciones-service)
-- =====================================================================================
\connect samar_recomendaciones

CREATE TABLE public.configuracion_algoritmo (
    id bigint NOT NULL,
    version character varying(20) NOT NULL,
    peso_calificacion double precision NOT NULL,
    peso_presupuesto double precision NOT NULL,
    peso_categorias double precision NOT NULL,
    peso_accesibilidad double precision NOT NULL,
    max_aparicion_prestador integer NOT NULL,
    activo boolean DEFAULT false NOT NULL
);

ALTER TABLE public.configuracion_algoritmo OWNER TO recomendaciones;

CREATE SEQUENCE public.configuracion_algoritmo_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.configuracion_algoritmo_id_seq OWNER TO recomendaciones;

ALTER SEQUENCE public.configuracion_algoritmo_id_seq OWNED BY public.configuracion_algoritmo.id;

CREATE TABLE public.ejecucion_algoritmo (
    id bigint NOT NULL,
    id_solicitud bigint NOT NULL,
    id_configuracion bigint NOT NULL,
    fecha_hora timestamp with time zone DEFAULT now() NOT NULL,
    tiempo_ms double precision,
    candidatos_evaluados integer,
    exitoso boolean NOT NULL
);

ALTER TABLE public.ejecucion_algoritmo OWNER TO recomendaciones;

CREATE SEQUENCE public.ejecucion_algoritmo_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.ejecucion_algoritmo_id_seq OWNER TO recomendaciones;

ALTER SEQUENCE public.ejecucion_algoritmo_id_seq OWNED BY public.ejecucion_algoritmo.id;

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);

ALTER TABLE public.flyway_schema_history OWNER TO recomendaciones;

CREATE TABLE public.medicion (
    id bigint NOT NULL,
    id_reporte bigint NOT NULL,
    nombre character varying(100) NOT NULL,
    valor double precision NOT NULL,
    unidad character varying(30)
);

ALTER TABLE public.medicion OWNER TO recomendaciones;

CREATE SEQUENCE public.medicion_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.medicion_id_seq OWNER TO recomendaciones;

ALTER SEQUENCE public.medicion_id_seq OWNED BY public.medicion.id;

CREATE TABLE public.recomendacion (
    id bigint NOT NULL,
    id_solicitud bigint NOT NULL,
    id_servicio bigint NOT NULL,
    puntaje double precision NOT NULL,
    explicacion text NOT NULL,
    version_algoritmo character varying(20) NOT NULL,
    fecha timestamp with time zone DEFAULT now() NOT NULL
);

ALTER TABLE public.recomendacion OWNER TO recomendaciones;

CREATE SEQUENCE public.recomendacion_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.recomendacion_id_seq OWNER TO recomendaciones;

ALTER SEQUENCE public.recomendacion_id_seq OWNED BY public.recomendacion.id;

CREATE TABLE public.registro_flujo (
    id bigint NOT NULL,
    zona character varying(80) NOT NULL,
    fecha date NOT NULL,
    numero_visitantes integer DEFAULT 0 NOT NULL,
    numero_reservas integer DEFAULT 0 NOT NULL
);

ALTER TABLE public.registro_flujo OWNER TO recomendaciones;

CREATE SEQUENCE public.registro_flujo_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.registro_flujo_id_seq OWNER TO recomendaciones;

ALTER SEQUENCE public.registro_flujo_id_seq OWNED BY public.registro_flujo.id;

CREATE TABLE public.reporte (
    id bigint NOT NULL,
    id_administrador bigint NOT NULL,
    tipo character varying(20) NOT NULL,
    periodo_inicio date NOT NULL,
    periodo_fin date NOT NULL,
    generado_en timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT reporte_tipo_check CHECK (((tipo)::text = ANY ((ARRAY['ESTADISTICAS'::character varying, 'FLUJO'::character varying, 'ALGORITMO'::character varying])::text[])))
);

ALTER TABLE public.reporte OWNER TO recomendaciones;

CREATE SEQUENCE public.reporte_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.reporte_id_seq OWNER TO recomendaciones;

ALTER SEQUENCE public.reporte_id_seq OWNED BY public.reporte.id;

CREATE TABLE public.solicitud_categoria (
    id_solicitud bigint NOT NULL,
    categoria character varying(30) NOT NULL
);

ALTER TABLE public.solicitud_categoria OWNER TO recomendaciones;

CREATE TABLE public.solicitud_dieta (
    id_solicitud bigint NOT NULL,
    tipo_dieta character varying(20) NOT NULL
);

ALTER TABLE public.solicitud_dieta OWNER TO recomendaciones;

CREATE TABLE public.solicitud_recomendacion (
    id bigint NOT NULL,
    id_turista bigint NOT NULL,
    numero_personas integer NOT NULL,
    fecha_inicio date NOT NULL,
    fecha_fin date NOT NULL,
    presupuesto numeric(12,2),
    solo_accesibles boolean DEFAULT false NOT NULL,
    creado_en timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT solicitud_recomendacion_check CHECK ((fecha_fin >= fecha_inicio)),
    CONSTRAINT solicitud_recomendacion_numero_personas_check CHECK ((numero_personas > 0))
);

ALTER TABLE public.solicitud_recomendacion OWNER TO recomendaciones;

CREATE SEQUENCE public.solicitud_recomendacion_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.solicitud_recomendacion_id_seq OWNER TO recomendaciones;

ALTER SEQUENCE public.solicitud_recomendacion_id_seq OWNED BY public.solicitud_recomendacion.id;

ALTER TABLE ONLY public.configuracion_algoritmo ALTER COLUMN id SET DEFAULT nextval('public.configuracion_algoritmo_id_seq'::regclass);

ALTER TABLE ONLY public.ejecucion_algoritmo ALTER COLUMN id SET DEFAULT nextval('public.ejecucion_algoritmo_id_seq'::regclass);

ALTER TABLE ONLY public.medicion ALTER COLUMN id SET DEFAULT nextval('public.medicion_id_seq'::regclass);

ALTER TABLE ONLY public.recomendacion ALTER COLUMN id SET DEFAULT nextval('public.recomendacion_id_seq'::regclass);

ALTER TABLE ONLY public.registro_flujo ALTER COLUMN id SET DEFAULT nextval('public.registro_flujo_id_seq'::regclass);

ALTER TABLE ONLY public.reporte ALTER COLUMN id SET DEFAULT nextval('public.reporte_id_seq'::regclass);

ALTER TABLE ONLY public.solicitud_recomendacion ALTER COLUMN id SET DEFAULT nextval('public.solicitud_recomendacion_id_seq'::regclass);

ALTER TABLE ONLY public.configuracion_algoritmo
    ADD CONSTRAINT configuracion_algoritmo_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.configuracion_algoritmo
    ADD CONSTRAINT configuracion_algoritmo_version_key UNIQUE (version);

ALTER TABLE ONLY public.ejecucion_algoritmo
    ADD CONSTRAINT ejecucion_algoritmo_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);

ALTER TABLE ONLY public.medicion
    ADD CONSTRAINT medicion_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.recomendacion
    ADD CONSTRAINT recomendacion_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.registro_flujo
    ADD CONSTRAINT registro_flujo_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.registro_flujo
    ADD CONSTRAINT registro_flujo_zona_fecha_key UNIQUE (zona, fecha);

ALTER TABLE ONLY public.reporte
    ADD CONSTRAINT reporte_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.solicitud_categoria
    ADD CONSTRAINT solicitud_categoria_pkey PRIMARY KEY (id_solicitud, categoria);

ALTER TABLE ONLY public.solicitud_dieta
    ADD CONSTRAINT solicitud_dieta_pkey PRIMARY KEY (id_solicitud, tipo_dieta);

ALTER TABLE ONLY public.solicitud_recomendacion
    ADD CONSTRAINT solicitud_recomendacion_pkey PRIMARY KEY (id);

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);

ALTER TABLE ONLY public.ejecucion_algoritmo
    ADD CONSTRAINT ejecucion_algoritmo_id_configuracion_fkey FOREIGN KEY (id_configuracion) REFERENCES public.configuracion_algoritmo(id);

ALTER TABLE ONLY public.ejecucion_algoritmo
    ADD CONSTRAINT ejecucion_algoritmo_id_solicitud_fkey FOREIGN KEY (id_solicitud) REFERENCES public.solicitud_recomendacion(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.medicion
    ADD CONSTRAINT medicion_id_reporte_fkey FOREIGN KEY (id_reporte) REFERENCES public.reporte(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.recomendacion
    ADD CONSTRAINT recomendacion_id_solicitud_fkey FOREIGN KEY (id_solicitud) REFERENCES public.solicitud_recomendacion(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.solicitud_categoria
    ADD CONSTRAINT solicitud_categoria_id_solicitud_fkey FOREIGN KEY (id_solicitud) REFERENCES public.solicitud_recomendacion(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.solicitud_dieta
    ADD CONSTRAINT solicitud_dieta_id_solicitud_fkey FOREIGN KEY (id_solicitud) REFERENCES public.solicitud_recomendacion(id) ON DELETE CASCADE;

-- Historial de Flyway: los servicios ven el esquema como ya migrado.
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (1, '1', 'esquema recomendaciones', 'SQL', 'V1__esquema_recomendaciones.sql', -777670059, 'recomendaciones', '2026-10-27 02:37:55.61791', 374, true);
