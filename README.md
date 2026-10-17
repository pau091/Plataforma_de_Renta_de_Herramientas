# 🛠️ Plataforma de Renta de Herramientas y Equipos de Construcción

Plataforma backend y frontend desarrollada bajo una estricta **Arquitectura Hexagonal (DDD)** utilizando **Java 17**, **Spring Boot** y despliegue sobre **Apache Tomcat**.

---

## 🚀 Tecnologías Utilizadas
* **Backend:** Java 17, Spring Boot, Spring Security (JWT), Spring Data JPA.
* **Base de Datos:** PostgreSQL / MySQL, gestionada mediante **Flyway Migrations**.
* **Documentación:** Springdoc OpenAPI (Swagger).
* **Infraestructura & Despliegue:** Docker, Docker Compose, Apache Tomcat 10.1.
* **Frontend:** HTML5, CSS3, JavaScript (Bootstrap 5).

---

## 📂 Estructura del Proyecto (Multimódulo Maven)
El proyecto está desacoplado en tres capas principales:
1. **`domain`**: Modelos de negocio puros, Value Objects y puertos de salida.
2. **`application`**: Casos de uso y servicios de lógica de negocio.
3. **`infraestructura`**: Adaptadores REST (Controllers), JPA (Entities/Repositories), configuración de seguridad JWT y archivos estáticos del Frontend.

---

## ⚙️ Instrucciones de Configuración y Ejecución

### 1. Clonar el repositorio
```bash
git clone [https://github.com/tu-usuario/Proyecto_RentaHerramientas_TuNombre.git](https://github.com/tu-usuario/Proyecto_RentaHerramientas_TuNombre.git)
cd Proyecto_RentaHerramientas_TuNombre