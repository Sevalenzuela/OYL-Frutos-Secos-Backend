package cl.oylfrutossecos.productos.config;

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

    // 1. OBTENER CLAVE JWT COMPARTIDA

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

    // 2. VALIDAR AUTENTICIDAD DEL TOKEN

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

    // 3. CONVERTIR ROLES DEL TOKEN

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

    // 4. DEFINIR PERMISOS DE LOS ENDPOINTS

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

                        // CONSULTAR PRODUCTOS

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/productos",
                                "/api/productos/*",
                                "/api/productos/*/foto/*"
                        ).permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/productos/*/alta/*")
                        .hasRole("ADMINISTRADOR")

                        // CREAR PRODUCTOS

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/productos"
                        ).hasRole("ADMINISTRADOR")

                        // ACTUALIZAR PRODUCTOS

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/productos/*"
                        ).hasRole("ADMINISTRADOR")

                        // CAMBIAR ESTADO

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/productos/*/estado"
                        ).hasRole("ADMINISTRADOR")

                        // ELIMINAR PRODUCTOS

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/productos/*"
                        ).hasRole("ADMINISTRADOR")

                        // BLOQUEAR OTRAS SOLICITUDES

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