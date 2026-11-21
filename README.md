# OYL Frutos Secos Backend

## Instalación limpia en Windows

1. Instala un JDK 21 y abre PowerShell en la raíz del repositorio. Deja libres los puertos 8081 a 8084.
2. En el primer arranque, ejecuta `.\iniciar-backend.ps1 -CrearAdmin`. Si el script no encuentra el JDK, agrega `-JavaHome` con la ruta de instalación.
3. Ingresa la contraseña de `admin_oyl` cuando PowerShell la solicite. Usa al menos 12 caracteres y como máximo 72 bytes en UTF-8. La entrada queda oculta y el script entrega la contraseña a Clientes mediante una variable de entorno del proceso. Clientes guarda su hash en H2.
4. En los siguientes arranques, ejecuta `.\iniciar-backend.ps1`. Si activas `-CrearAdmin` otra vez, Clientes conserva la contraseña del administrador existente. Si el nombre de usuario o el correo reservado está ocupado por otra cuenta, revisa la ventana de Clientes.

Las bases H2 se crean en las carpetas de los servicios y están excluidas de Git. Para una instalación independiente, usa otro checkout limpio y detén los servicios del checkout anterior antes de iniciarla. Cada arranque genera una nueva clave JWT, por lo que las sesiones anteriores requieren volver a iniciar sesión.

## Pruebas

Con Java 21 configurado, ejecuta `.\mvnw.cmd test` desde cada carpeta de servicio: `productos-service`, `inventario-service`, `clientes-service` y `pedidos-service`.
