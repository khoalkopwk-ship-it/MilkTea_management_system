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

@Configuration(proxyBeanMethods = false)
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
        org.springframework.security.web.csrf.CookieCsrfTokenRepository csrfTokenRepository =
                org.springframework.security.web.csrf.CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokenRepository.setCookiePath("/");
        csrfTokenRepository.setHeaderName("X-CSRF-TOKEN");

        org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler requestHandler =
                new org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null);

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(requestHandler)
                        .ignoringRequestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/register",
                                "/api/v1/auth/verify-email",
                                "/api/v1/auth/resend-otp",
                                "/api/v1/auth/password-reset/**",
                                "/ws/**",
                                "/actuator/**"
                        )
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Settings, Vouchers and Preparation Recipes are strictly ADMIN only
                        .requestMatchers("/admin/settings", "/admin/settings/**").hasRole("ADMIN")
                        .requestMatchers("/admin/vouchers", "/admin/vouchers/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/vouchers", "/api/v1/admin/vouchers/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/settings", "/api/v1/admin/settings/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/preparation-recipes", "/api/v1/admin/preparation-recipes/**").hasRole("ADMIN")
                        // Admin pages & APIs: accessible to ADMIN and MANAGER (manager is scoped to their branch)
                        .requestMatchers("/admin", "/admin/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/cashier", "/cashier/**").hasRole("CASHIER")
                        .requestMatchers("/kitchen", "/kitchen/**").hasAnyRole("KITCHEN", "CHEF")
                        // 1. Static resources, pages & websocket
                        .requestMatchers(
                                "/",
                                "/menu",
                                "/cart",
                                "/orders",
                                "/profile",
                                "/products/**",
                                "/favorites",
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
                        .requestMatchers(
                                "/api/v1/admin/categories",
                                "/api/v1/admin/categories/**",
                                "/api/v1/admin/branches",
                                "/api/v1/admin/branches/**",
                                "/api/v1/admin/refunds/*/decision"
                        ).hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/api/v1/cashier/**").hasRole("CASHIER")
                        .requestMatchers("/api/v1/kitchen/**").hasAnyRole("KITCHEN", "CHEF")
                        .requestMatchers("/api/v1/inventory/**").hasAnyRole("ADMIN", "KITCHEN", "CASHIER", "MANAGER")
                        .requestMatchers("/api/v1/orders/counter").hasAnyRole("CASHIER", "ADMIN", "MANAGER")
                        // 3. Public, Customer & Guest routes
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/api/v1/public/**",
                                "/api/v1/branches",
                                "/api/v1/branches/**",
                                "/api/v1/catalog/**",
                                "/api/v1/tables/**",
                                "/api/v1/table-sessions/**",
                                "/api/v1/orders/table",
                                "/api/v1/orders/**",
                                "/api/v1/customer/**",
                                "/api/v1/favorites/**",
                                "/api/v1/reviews/**",
                                "/api/v1/vouchers/**"
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
