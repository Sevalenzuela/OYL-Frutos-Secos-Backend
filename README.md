# OYL Frutos Secos — Backend

Backend de la aplicación móvil OYL Frutos Secos, desarrollado mediante una arquitectura de microservicios.

## Tecnologías

- Java 21.
- Spring Boot.
- Spring Security.
- Spring Data JPA.
- H2 Database.
- Maven.
- JWT.

## Microservicios

| Servicio | Puerto | Responsabilidad |
|---|---:|---|
| Productos | 8081 | Catálogo, productos y fotografías. |
| Inventario | 8082 | Existencias y movimientos de stock. |
| Clientes | 8083 | Registro, autenticación y perfiles. |
| Pedidos | 8084 | Compras, historial y estados de pedidos. |

## Ejecución

Requisitos: Java 21, PowerShell y los puertos 8081 a 8084 disponibles.

Primer inicio:

`.\iniciar-backend.ps1 -CrearAdmin`

Ingresar una contraseña de al menos 12 caracteres para el administrador `admin_oyl`.

Inicios posteriores:

`.\iniciar-backend.ps1`

Las bases de datos H2 se inicializan automáticamente y conservan sus datos entre ejecuciones. Las claves JWT se generan al iniciar los servicios, por lo que es necesario volver a iniciar sesión después de reiniciarlos.

Para crear productos desde Android se utiliza el endpoint multipart con inventario inicial, fotografía opcional y clave de idempotencia. El endpoint JSON anterior se mantiene por compatibilidad, pero no inicializa automáticamente el inventario.

## Aplicación Android

https://github.com/sigridRiquelme/OYL-Frutos-Secos

La documentación técnica detallada se incluye en el documento README externo de la entrega académica.