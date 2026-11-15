# Se construye desde la raíz del monorepo (context: .) para resolver el POM padre.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
# Solo el POM padre y este servicio: un cambio en otro servicio no reconstruye esta imagen.
COPY pom.xml .
COPY oferta-service oferta-service
# La caché de Maven sobrevive entre builds: redesplegar no vuelve a descargar las dependencias.
RUN --mount=type=cache,target=/root/.m2 mvn -q -f oferta-service/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/oferta-service/target/oferta-service-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
