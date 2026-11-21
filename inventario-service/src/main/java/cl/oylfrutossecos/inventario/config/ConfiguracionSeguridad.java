package cl.oylfrutossecos.inventario.config;

import jakarta.servlet.DispatcherType;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import org.springframework.http.HttpMethod;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;

@Configuration
public class ConfiguracionSeguridad {

    // 1. CONFIGURAR CLAVE JWT

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

    // 2. VALIDAR TOKENS JWT

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

    // 3. CONVERTIR ROLES JWT

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

    // 4. SEGURIDAD EXCLUSIVA PARA RESERVAS

    @Bean
    @Order(1)
    public SecurityFilterChain seguridadInterna(
            HttpSecurity http,

            @Value("${oyl.inventario.secret}")
            String secretoInterno

    ) throws Exception {

        if (
                Base64.getDecoder()
                        .decode(secretoInterno).length < 32
        ) {

            throw new IllegalArgumentException(
                    "La clave interna debe tener al menos 32 bytes"
            );
        }

        byte[] claveEsperada =
                secretoInterno.getBytes(
                        StandardCharsets.UTF_8
                );

        http

                .securityMatcher(
                        "/api/inventario/reservas",
                        "/api/inventario/reservas/**"
                )

                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(sesion ->
                        sesion.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authenticationManager(authentication -> {

                    String usuario =
                            authentication.getName();

                    byte[] claveRecibida =

                            String.valueOf(
                                    authentication.getCredentials()
                            ).getBytes(
                                    StandardCharsets.UTF_8
                            );

                    boolean claveValida =
                            MessageDigest.isEqual(
                                    claveEsperada,
                                    claveRecibida
                            );

                    if (
                            !"pedidos-service".equals(usuario)
                                    || !claveValida
                    ) {

                        throw new BadCredentialsException(
                                "Credencial interna invalida"
                        );
                    }

                    return UsernamePasswordAuthenticationToken
                            .authenticated(
                                    usuario,
                                    null,
                                    List.of(
                                            new SimpleGrantedAuthority(
                                                    "ROLE_SERVICIO"
                                            )
                                    )
                            );
                })

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/inventario/reservas"
                        ).hasRole("SERVICIO")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/inventario/reservas/*"
                        ).hasRole("SERVICIO")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/inventario/reservas/*/confirmar",
                                "/api/inventario/reservas/*/cancelar"
                        ).hasRole("SERVICIO")

                        .anyRequest().denyAll()
                )

                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    // 5. SEGURIDAD PARA USUARIOS Y ADMINISTRADORES

    @Bean
    @Order(2)
    public SecurityFilterChain seguridadUsuarios(
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

                        // CONSULTAR EXISTENCIAS

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/inventario",
                                "/api/inventario/*"
                        ).permitAll()

                        .requestMatchers(HttpMethod.PUT, "/api/inventario/*/inicial")
                        .hasRole("ADMINISTRADOR")

                        // AJUSTAR EXISTENCIAS

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/inventario/*/ajustes"
                        ).hasRole("ADMINISTRADOR")

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