package cl.oylfrutossecos.clientes.config;

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

    // 1. CLAVE SECRETA JWT
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

    // 2. GENERAR TOKENS
    @Bean
    public JwtEncoder jwtEncoder(SecretKey claveJwt) {

        return NimbusJwtEncoder
                .withSecretKey(claveJwt)
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    // 3. VALIDAR TOKENS
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

    // 4. CONVERTIR EL ROL JWT EN UN PERMISO
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

    // 5. REGLAS DE SEGURIDAD
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

                        // ACCESO PUBLICO
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/registro",
                                "/api/auth/login"
                        ).permitAll()

                        // USUARIOS AUTENTICADOS
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/auth/sesion",
                                "/api/clientes/me"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/clientes/me"
                        ).authenticated()

                        // ACCESO EXCLUSIVO DEL ADMINISTRADOR
                        .requestMatchers(
                                "/api/admin/**"
                        ).hasRole("ADMINISTRADOR")

                        // DENEGAR OTRAS RUTAS
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