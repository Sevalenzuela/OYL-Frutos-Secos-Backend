package cl.oylfrutossecos.clientes.service;

import cl.oylfrutossecos.clientes.dto.ClienteResponse;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class JwtService {

    private static final long DURACION_SEGUNDOS = 1800;

    private final JwtEncoder jwtEncoder;

    public JwtService(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    // Generar un token firmado.
    public String generarToken(ClienteResponse cliente) {

        Instant ahora = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()

                // Identificar al emisor.
                .issuer("oyl-clientes")

                // Identificar al cliente.
                .subject(cliente.id().toString())

                // Fecha de creacion.
                .issuedAt(ahora)

                // Vencimiento en 30 minutos.
                .expiresAt(
                        ahora.plusSeconds(DURACION_SEGUNDOS)
                )

                // Informacion de autorizacion.
                .claim("rol", cliente.rol().name())

                .build();

        JwsHeader encabezado = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(
                        encabezado,
                        claims
                )
        ).getTokenValue();
    }

    public long obtenerDuracionSegundos() {
        return DURACION_SEGUNDOS;
    }
}