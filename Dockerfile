# ==========================================
# ETAPA 1: Construcción del Proyecto con Maven
# ==========================================
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Copiamos todo el código fuente del proyecto multimódulo
COPY . .

# Compilamos el proyecto completo omitiendo pruebas para agilizar
RUN mvn clean package -DskipTests

# ==========================================
# ETAPA 2: Despliegue en Apache Tomcat
# ==========================================
# Spring Boot 4 usa Jakarta EE 11 (Servlet 6.1): requiere Apache Tomcat 11.
FROM tomcat:11.0-jdk21
WORKDIR /usr/local/tomcat

# Eliminamos las aplicaciones por defecto de Tomcat
RUN rm -rf webapps/*

# Copiamos el .WAR generado en el módulo infraestructura hacia el ROOT.war de Tomcat
COPY --from=builder /app/infraestructura/target/*.war webapps/ROOT.war

# Exponemos el puerto estándar de Tomcat
EXPOSE 8080

CMD ["catalina.sh", "run"]