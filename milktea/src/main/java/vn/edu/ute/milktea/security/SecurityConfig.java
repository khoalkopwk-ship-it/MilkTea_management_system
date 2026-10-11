package vn.edu.ute.milktea.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable) // CSRF handled via token headers/check for API or CookieCsrfTokenRepository
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Staff pages require an authenticated staff JWT.
                        .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                        .requestMatchers("/cashier", "/cashier/**").hasAnyRole("ADMIN", "CASHIER")
                        .requestMatchers("/kitchen", "/kitchen/**").hasAnyRole("ADMIN", "KITCHEN")
                        // 1. Static resources, pages & websocket
                        .requestMatchers(
                                "/",
                                "/menu",
                                "/cart",
                                "/orders",
                                "/order-history",
                                "/profile",
                                "/login",
                                "/error",
                                "/static/**",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/ws/**",
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()
                        // 2. Role-based staff routes
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/cashier/**").hasAnyRole("ADMIN", "CASHIER")
                        .requestMatchers("/api/v1/kitchen/**").hasAnyRole("ADMIN", "KITCHEN")
                        .requestMatchers("/api/v1/inventory/**").hasAnyRole("ADMIN", "KITCHEN", "CASHIER")
                        // 3. Public, Customer & Guest routes
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/api/v1/public/**",
                                "/api/v1/catalog/**",
                                "/api/v1/tables/**",
                                "/api/v1/table-sessions/**",
                                "/api/v1/orders/**",
                                "/api/v1/customer/**"
                        ).permitAll()
                        // Everything else authenticated
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            String uri = request.getRequestURI();
                            boolean staffPage = uri.equals("/admin") || uri.startsWith("/admin/")
                                    || uri.equals("/cashier") || uri.startsWith("/cashier/")
                                    || uri.equals("/kitchen") || uri.startsWith("/kitchen/");

                            if (staffPage) {
                                response.sendRedirect("/login?redirect=" + uri);
                                return;
                            }
                            response.sendError(403);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    String uri = request.getRequestURI();
                    boolean staffPage = uri.equals("/admin") || uri.startsWith("/admin/")
                            || uri.equals("/cashier") || uri.startsWith("/cashier/")
                            || uri.equals("/kitchen") || uri.startsWith("/kitchen/");
                    boolean anonymous = authentication == null || authentication instanceof AnonymousAuthenticationToken;

                    if (staffPage && anonymous) {
                        response.sendRedirect("/login?redirect=" + uri);
                        return;
                    }
                    response.sendError(403);
                }))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.base-url:http://localhost:8080}") String baseUrl,
            @Value("${app.websocket.allowed-origin:http://localhost:8080}") String allowedOrigin) {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = new ArrayList<>();
        if (baseUrl != null && !baseUrl.isBlank()) origins.add(baseUrl);
        if (allowedOrigin != null && !allowedOrigin.isBlank() && !origins.contains(allowedOrigin)) origins.add(allowedOrigin);
        if (!origins.contains("http://localhost:8080")) origins.add("http://localhost:8080");
        if (!origins.contains("http://127.0.0.1:8080")) origins.add("http://127.0.0.1:8080");

        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
