
param(
    [string]$JavaHome = "",
    [switch]$CrearAdmin
)

$ErrorActionPreference = "Stop"

Clear-Host

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "     OYL FRUTOS SECOS - BACKEND" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$raiz = $PSScriptRoot

$servicios = @(
    @{ Nombre = "Productos"; Carpeta = "productos-service"; Puerto = 8081 }
    @{ Nombre = "Inventario"; Carpeta = "inventario-service"; Puerto = 8082 }
    @{ Nombre = "Clientes"; Carpeta = "clientes-service"; Puerto = 8083 }
    @{ Nombre = "Pedidos"; Carpeta = "pedidos-service"; Puerto = 8084 }
)

# 1. COMPROBAR PUERTOS

Write-Host "`nComprobando puertos..." -ForegroundColor Cyan

foreach ($servicio in $servicios) {

    $ocupado = Get-NetTCPConnection `
        -LocalPort $servicio.Puerto `
        -State Listen `
        -ErrorAction SilentlyContinue

    if ($ocupado) {
        Write-Host "[ERROR] Puerto $($servicio.Puerto) ocupado." -ForegroundColor Red
        Write-Host "Deten los servicios anteriores antes de continuar."
        return
    }
}

# 2. LOCALIZAR JAVA 21

Write-Host "`nBuscando Java 21..." -ForegroundColor Cyan

$candidatos = @($JavaHome, $env:JAVA_HOME)

$rutasJava = @(
    "$env:USERPROFILE\.jdks"
    "$env:ProgramFiles\Java"
    "$env:ProgramFiles\Eclipse Adoptium"
    "$env:ProgramFiles\Microsoft"
)

foreach ($ruta in $rutasJava) {

    if (Test-Path $ruta) {

        $candidatos += Get-ChildItem `
            -Path $ruta `
            -Directory |
            Select-Object -ExpandProperty FullName
    }
}

$jdk21 = $null

foreach ($ruta in $candidatos) {

    if ([string]::IsNullOrWhiteSpace($ruta)) {
        continue
    }

    $release = Join-Path $ruta "release"
    $javac = Join-Path $ruta "bin\javac.exe"

    if ((Test-Path $release) -and (Test-Path $javac)) {

        if (Select-String -Path $release -Pattern '^JAVA_VERSION="21(?:[.+-]|")' -Quiet) {

            $jdk21 = $ruta
            break
        }
    }
}

if (-not $jdk21) {

    Write-Host "[ERROR] No se encontro Java 21." -ForegroundColor Red
    Write-Host "Indica su carpeta mediante el parametro -JavaHome."
    return
}

$env:JAVA_HOME = $jdk21
$env:Path = "$jdk21\bin;$env:Path"

Write-Host "[OK] Java 21 configurado." -ForegroundColor Green
Write-Host $env:JAVA_HOME

# 3. COMPROBAR LOS PROYECTOS

foreach ($servicio in $servicios) {

    $carpeta = Join-Path $raiz $servicio.Carpeta
    $maven = Join-Path $carpeta "mvnw.cmd"

    if (-not (Test-Path $maven)) {

        Write-Host "[ERROR] No se encontro $maven" -ForegroundColor Red
        return
    }
}

# 4. GENERAR CLAVE JWT COMPARTIDA

Write-Host "`nConfigurando seguridad JWT..." -ForegroundColor Cyan

$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()

$rng.GetBytes($bytes)

$env:OYL_JWT_SECRET = [Convert]::ToBase64String($bytes)

$rng.Dispose()

$env:OYL_ADMIN_BOOTSTRAP = "false"

Remove-Item Env:OYL_ADMIN_PASSWORD -ErrorAction SilentlyContinue

# En una instalacion nueva, solicitar la contrasena sin mostrarla
# ni escribirla en un archivo. Solo se entrega al proceso Clientes.
$claveAdminSegura = $null

if ($CrearAdmin) {

    $claveAdminSegura = Read-Host "Contrasena inicial de admin_oyl (minimo 12 caracteres)" -AsSecureString
    $puntero = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($claveAdminSegura)

    try {

        $texto = [System.Runtime.InteropServices.Marshal]::PtrToStringBSTR($puntero)

        if (
            [string]::IsNullOrWhiteSpace($texto) -or
            $texto.Length -lt 12 -or
            [System.Text.Encoding]::UTF8.GetByteCount($texto) -gt 72
        ) {
            throw "La contrasena administrativa debe tener al menos 12 caracteres y como maximo 72 bytes UTF-8."
        }

    } finally {

        [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($puntero)
        Remove-Variable texto -ErrorAction SilentlyContinue
    }
}

Write-Host "[OK] Clave JWT compartida generada." -ForegroundColor Green

# GENERAR CREDENCIAL INTERNA DE INVENTARIO

$bytesInternos = New-Object byte[] 32

$rngInterno = [System.Security.Cryptography.RandomNumberGenerator]::Create()

try {

    $rngInterno.GetBytes($bytesInternos)

    $secretoInventario = [Convert]::ToBase64String($bytesInternos)

} finally {

    $rngInterno.Dispose()
}

Write-Host "[OK] Credencial interna de Inventario generada." -ForegroundColor Green

# 5. FUNCION PARA COMPROBAR CONEXIONES

function Test-Puerto {

    param([int]$Puerto)

    $conexion = New-Object System.Net.Sockets.TcpClient

    try {

        $tarea = $conexion.ConnectAsync("127.0.0.1", $Puerto)

        return ($tarea.Wait(500) -and $conexion.Connected)

    } catch {

        return $false

    } finally {

        $conexion.Dispose()
    }
}

# 6. INICIAR MICROSERVICIOS

foreach ($servicio in $servicios) {

    Write-Host "`nIniciando $($servicio.Nombre)..." -ForegroundColor Yellow

    $carpeta = Join-Path $raiz $servicio.Carpeta

    $comando = '$Host.UI.RawUI.WindowTitle = "OYL - {0}"; .\mvnw.cmd spring-boot:run' -f $servicio.Nombre

if ($servicio.Nombre -in @("Inventario", "Pedidos")) {

    $env:OYL_INVENTARIO_SECRET = $secretoInventario

} else {

    Remove-Item Env:OYL_INVENTARIO_SECRET -ErrorAction SilentlyContinue
}
    # Entregar la contrasena administrativa solo a Clientes, si se solicita.
    if ($CrearAdmin -and $servicio.Nombre -eq "Clientes") {

        $puntero = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($claveAdminSegura)

        try {
            $env:OYL_ADMIN_PASSWORD = [System.Runtime.InteropServices.Marshal]::PtrToStringBSTR($puntero)
            $env:OYL_ADMIN_BOOTSTRAP = "true"
        } finally {
            [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($puntero)
        }

    } else {

        $env:OYL_ADMIN_BOOTSTRAP = "false"
        Remove-Item Env:OYL_ADMIN_PASSWORD -ErrorAction SilentlyContinue
    }

    try {

        Start-Process powershell.exe `
            -WorkingDirectory $carpeta `
            -ArgumentList "-NoProfile", "-NoExit", "-Command", $comando

    } finally {

        # Evitar que los siguientes servicios hereden la contrasena.
        Remove-Item Env:OYL_ADMIN_PASSWORD -ErrorAction SilentlyContinue
        $env:OYL_ADMIN_BOOTSTRAP = "false"

        if ($CrearAdmin -and $servicio.Nombre -eq "Clientes") {
            $claveAdminSegura.Dispose()
            $claveAdminSegura = $null
        }
    }

    $limite = (Get-Date).AddSeconds(180)

    $conectado = $false

    while ((Get-Date) -lt $limite) {

        if (Test-Puerto -Puerto $servicio.Puerto) {

            $conectado = $true
            break
        }

        Start-Sleep -Seconds 2
    }

    if (-not $conectado) {

        Write-Host "[ERROR] $($servicio.Nombre) no pudo iniciar." -ForegroundColor Red
        Write-Host "Revisa la terminal del microservicio."
        Write-Host "Los servicios anteriores pueden seguir ejecutandose."

        return
    }

    Write-Host "[OK] $($servicio.Nombre) accesible en puerto $($servicio.Puerto)." -ForegroundColor Green
}

# 7. MOSTRAR RESUMEN FINAL

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "       BACKEND OYL INICIADO" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan

foreach ($servicio in $servicios) {

    if (Test-Puerto -Puerto $servicio.Puerto) {

        Write-Host "[OK] $($servicio.Nombre) - Puerto $($servicio.Puerto)" -ForegroundColor Green

    } else {

        Write-Host "[ERROR] $($servicio.Nombre)" -ForegroundColor Red
    }
}

Write-Host "`nPuedes realizar las pruebas desde esta terminal."
Write-Host "No cierres las ventanas de los microservicios."
