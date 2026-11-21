package cl.oylfrutossecos.pedidos.config;

import jakarta.servlet.DispatcherType;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.util.Base64;

@Configuration
public class ConfiguracionSeguridad {

    @Bean
    public SecretKey claveJwt(
            @Value("${oyl.jwt.secret}") String secreto
    ) {

        byte[] bytes = Base64.getDecoder()
                .decode(secreto);

        if (bytes.length < 32) {

            throw new IllegalArgumentException(
                    "La clave JWT debe tener al menos 32 bytes"
            );
        }

        return new SecretKeySpec(
                bytes,
                "HmacSHA256"
        );
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey claveJwt) {

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(claveJwt)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        decoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(
                        "oyl-clientes"
                )
        );

        return decoder;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter permisos =
                new JwtGrantedAuthoritiesConverter();

        permisos.setAuthoritiesClaimName("rol");
        permisos.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(
                permisos
        );

        return converter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter converter
    ) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(sesion ->
                        sesion.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        .dispatcherTypeMatchers(
                                DispatcherType.ERROR
                        ).permitAll()

                        // REGISTRAR COMPRA

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/pedidos"
                        ).hasRole("CLIENTE")

                        // HISTORIAL PROPIO

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/pedidos/mis-pedidos"
                        ).hasRole("CLIENTE")

                        // LISTADO ADMINISTRATIVO

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/pedidos"
                        ).hasRole("ADMINISTRADOR")

                        // CAMBIAR ESTADOS

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/pedidos/*/estado"
                        ).hasRole("ADMINISTRADOR")

                        // CONSULTAR PEDIDO INDIVIDUAL

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/pedidos/*"
                        ).hasAnyRole(
                                "CLIENTE",
                                "ADMINISTRADOR"
                        )

                        .anyRequest().denyAll()
                )

                .oauth2ResourceServer(oauth ->
                        oauth.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(
                                        converter
                                )
                        )
                );

        return http.build();
    }
}