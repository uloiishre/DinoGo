package com.dinogo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.dinogo.security.JwtAuthenticationFilter;

@Configuration
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;

        public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .cors(Customizer.withDefaults())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .httpBasic(httpBasic -> httpBasic.disable())
                                .formLogin(formLogin -> formLogin.disable())
                                .authorizeHttpRequests(authorize -> authorize
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                .requestMatchers("/api/auth/**").permitAll()
                                                .requestMatchers(HttpMethod.GET,
                                                                "/api/products/**",
                                                                "/api/categories/**",
                                                                "/api/subcategories/**",
                                                                "/api/brands/**",
                                                                "/api/search/**").permitAll()
                                                .requestMatchers("/api/seller/**").hasRole("SELLER")
                                                .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("SELLER")
                                                .requestMatchers(HttpMethod.PATCH, "/api/products/**").hasRole("SELLER")
                                                .requestMatchers(HttpMethod.PATCH, "/api/orders/*/status").hasRole("SELLER")
                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/orders/*/shipment")
                                                .hasRole("SELLER")
                                                .requestMatchers(
                                                                HttpMethod.PATCH,
                                                                "/api/orders/*/shipment/status")
                                                .hasRole("SELLER")
                                                .requestMatchers(
                                                                HttpMethod.PATCH,
                                                                "/api/orders/*/shipment/tracking-info")
                                                .hasRole("SELLER")
                                                .requestMatchers(
                                                                "/api/cart/**",
                                                                "/api/favorites/**",
                                                                "/api/checkout/**",
                                                                "/api/orders/**",
                                                                // 收件地址包含個資，只允許已登入會員存取。
                                                                "/api/addresses/**",
                                                                "/api/member/**")
                                                .authenticated()
                                                .anyRequest().permitAll())
                                .exceptionHandling(exceptionHandling -> exceptionHandling
                                                .authenticationEntryPoint((request, response, exception) -> response
                                                                .sendError(401,
                                                                                "Authentication is required"))
                                                .accessDeniedHandler((request, response, exception) -> response
                                                                .sendError(403, "Insufficient role")))
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
