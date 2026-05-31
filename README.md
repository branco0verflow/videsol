# videsol-backend

Backend en Spring Boot para integrar la web de la concesionaria con la API de **Pilot CRM**.
Actúa como capa intermedia (BFF) entre el frontend y Pilot, encapsulando autenticación,
cache de token, y mapeo de datos.

## Stack

- **Java 21** (LTS)
- **Spring Boot 3.3.5**
- **Maven** (build tool)
- **WebClient** (Spring WebFlux) para llamadas HTTP a Pilot
- **Caffeine** para cache en memoria
- **Lombok** para reducir boilerplate
- **springdoc-openapi** para documentación Swagger UI

## Estructura del proyecto

```
src/main/java/com/videsol/backend/
├── VidesolBackendApplication.java   # Entry point
├── config/                          # WebClient, CORS, properties
├── controller/                      # Endpoints REST que expone tu backend
├── service/                         # Lógica de negocio (auth, etc.)
├── client/                          # Wrapper de llamadas HTTP a Pilot
├── dto/
│   ├── pilot/                       # DTOs internos para hablar con Pilot
│   └── response/                    # DTOs limpios que devuelve tu API al frontend
└── exception/                       # Manejo centralizado de errores
```

## Cómo arrancar

### 1. Prerequisitos

- JDK 21 instalado (`java -version`)
- Maven 3.8+ (o usar el wrapper `./mvnw`)
- IntelliJ IDEA Community o Ultimate

### 2. Importar en IntelliJ

1. Abrir IntelliJ → **File → Open**
2. Seleccionar la carpeta `videsol-backend` (la que contiene `pom.xml`)
3. Esperar a que Maven descargue las dependencias
4. **Habilitar Lombok**: Settings → Plugins → buscar "Lombok" → Install (si no está)
5. **Habilitar annotation processing**: Settings → Build → Compiler → Annotation Processors → Enable

### 3. Configurar credenciales de Pilot

**Nunca commitees credenciales reales al repo.** Configurarlas como variables de entorno:

**Opción A — En IntelliJ (recomendado para dev):**
- Run → Edit Configurations → seleccionar `VidesolBackendApplication`
- En "Environment variables" agregar:
  ```
  PILOT_USERNAME=tu_usuario;PILOT_PASSWORD=tu_password
  ```

**Opción B — Por terminal:**
```bash
export PILOT_USERNAME=tu_usuario
export PILOT_PASSWORD=tu_password
./mvnw spring-boot:run
```

### 4. Correr el proyecto

Desde IntelliJ: click derecho en `VidesolBackendApplication` → Run

O desde terminal:
```bash
./mvnw spring-boot:run
```

El servidor arranca en `http://localhost:8080`.

### 5. Verificar que funciona

```bash
# Healthcheck básico
curl http://localhost:8080/api/health

# Healthcheck con Pilot (prueba que la auth funcione)
curl http://localhost:8080/api/health/pilot
```

### 6. Swagger UI

Una vez corriendo, abrí en el navegador:
```
http://localhost:8080/swagger-ui.html
```

## Próximos pasos a implementar

- [ ] `PilotCatalogService` con `leerProducto(id)` y `buscarPorCodigo(code)`
- [ ] `PilotPriceListService` con `listarPreciosPorCodigo(code)`
- [ ] DTOs limpios de respuesta (`VehiculoDTO`, `PrecioDTO`)
- [ ] `VehiculoController` que ensambla catálogo + precios
- [ ] Tests unitarios de los servicios mockeando `WebClient`
- [ ] Circuit breaker (Resilience4j) para tolerancia a fallos de Pilot

## Notas de seguridad

- El token de Pilot dura 5h. Se cachea en memoria y se refresca automáticamente.
- Si querés escalar a múltiples instancias del backend, mover el cache a Redis.
- Las credenciales NUNCA en el código. Solo via environment variables o secret manager.
- En producción: agregar HTTPS, rate limiting, y autenticación de tu propia API.
