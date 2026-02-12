package pfa.dev.authservice.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;


@Configuration
@EnableWebSecurity

public class SecurityConfig {
    private JwtAuthConverter jwtAuthConverter;

     @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
     private String uri;
    public SecurityConfig(JwtAuthConverter jwtAuthConverter) {
        this.jwtAuthConverter = jwtAuthConverter;
    }

    @Bean
      
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        return http

                .sessionManagement(sm->sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS ))
                .csrf(csrf-> csrf.disable())
                .authorizeHttpRequests(ar -> ar
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/info").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                        .decoder(jwtDecoder())
                        .jwtAuthenticationConverter(jwtAuthConverter)
                )
        )
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder(){
        return JwtDecoders.fromOidcIssuerLocation(uri);
    }

}
