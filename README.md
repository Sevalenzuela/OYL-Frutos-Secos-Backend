# OYL Frutos Secos Backend

## Instalación limpia en Windows

1. Instala un JDK 21 y abre PowerShell en la raíz del repositorio. Deja libres los puertos 8081 a 8084.
2. En el primer arranque, ejecuta `.\iniciar-backend.ps1 -CrearAdmin`. Si el script no encuentra el JDK, agrega `-JavaHome` con la ruta de instalación.
3. Ingresa la contraseña de `admin_oyl` cuando PowerShell la solicite. Usa al menos 12 caracteres y como máximo 72 bytes en UTF-8. La entrada queda oculta y el script entrega la contraseña a Clientes mediante una variable de entorno del proceso. Clientes guarda su hash en H2.
4. En los siguientes arranques, ejecuta `.\iniciar-backend.ps1`. Si activas `-CrearAdmin` otra vez, Clientes conserva la contraseña del administrador existente. Si el nombre de usuario o el correo reservado está ocupado por otra cuenta, revisa la ventana de Clientes.

Las bases H2 se crean en las carpetas de los servicios y están excluidas de Git. Para una instalación independiente, usa otro checkout limpio y detén los servicios del checkout anterior antes de iniciarla. Cada arranque genera una nueva clave JWT, por lo que las sesiones anteriores requieren volver a iniciar sesión.

## Pruebas

Con Java 21 configurado, ejecuta `.\mvnw.cmd test` desde cada carpeta de servicio: `productos-service`, `inventario-service`, `clientes-service` y `pedidos-service`.

## Alta de productos con fotografía

Android crea con `POST /api/productos` multipart autenticado como administrador.
Envía las partes `datos` (JSON), `stock` (entero mayor o igual a cero) y `foto`
(opcional), más una cabecera `Idempotency-Key` UUID. La edición multipart
usa `PUT /api/productos/{id}` con `datos` y una foto opcional. Las rutas
JSON de la API siguen disponibles para sus consumidores anteriores.

Productos conserva el alta pendiente hasta que Inventario verifica el producto
y registra el stock inicial. Si falla la comunicación, reintenta desde el
mismo formulario con los mismos datos. La misma clave identifica la primera
solicitud guardada. Inventario conserva cualquier ajuste o reserva al repetir
la inicialización. Los registros pendientes quedan fuera del catálogo.

Las imágenes JPEG y PNG se validan y recodifican antes de guardarlas en la
base de Productos, con un máximo de 5 MiB y 4 megapíxeles. Cada foto se
sirve por una ruta relativa al servicio de Productos. Incluye la base de
Productos al respaldar la instalación para conservar las imágenes.

Para ejecutar los servicios en el mismo equipo, usa `iniciar-backend.ps1`.
Si Productos e Inventario se ejecutan en equipos distintos, configura
`OYL_INVENTARIO_URL` en Productos y `OYL_PRODUCTOS_URL` en Inventario
con las direcciones que cada servicio puede alcanzar.

Para revisar la recuperación ante fallos y la inicialización idempotente:

```powershell
.\productos-service\mvnw.cmd -f productos-service/pom.xml test
.\inventario-service\mvnw.cmd -f inventario-service/pom.xml test
```
