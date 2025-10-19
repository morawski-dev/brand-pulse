package com.morawski.dev.backend.config;

import lombok.extern.slf4j.Slf4j;
import com.morawski.dev.backend.util.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Value("${spring.web.cors.allowed-origins}")
	private String[] allowedOrigins;

	@Value("${spring.web.cors.allowed-methods}")
	private String allowedMethods;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		log.info("Configuring Security Filter Chain");

		http
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			)
			.authorizeHttpRequests(auth -> auth
				// Public endpoints - Health and documentation
				.requestMatchers("/actuator/**").permitAll()
				.requestMatchers("/api-docs/**").permitAll()
				.requestMatchers("/swagger-ui/**").permitAll()
				.requestMatchers("/swagger-ui.html").permitAll()
				// Public endpoints - Authentication (API Plan Section 12.2)
				.requestMatchers("/api/auth/register").permitAll()
				.requestMatchers("/api/auth/login").permitAll()
				.requestMatchers("/api/auth/forgot-password").permitAll()
				.requestMatchers("/api/auth/reset-password").permitAll()
				.requestMatchers("/api/health/**").permitAll()
				// All other endpoints require authentication
				.anyRequest().authenticated()
			);

		return http.build();
	}

	/**
	 * Password encoder bean using BCrypt with 10 rounds.
	 * API Plan Section 3.1: "Hash password using BCrypt (minimum 10 rounds)"
	 *
	 * @return BCryptPasswordEncoder
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		log.info("Configuring BCrypt password encoder with strength: {}", Constants.BCRYPT_STRENGTH);
		return new BCryptPasswordEncoder(Constants.BCRYPT_STRENGTH);
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
		configuration.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);

		return source;
	}
}
