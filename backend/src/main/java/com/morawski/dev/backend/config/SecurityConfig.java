package com.morawski.dev.backend.config;

import com.morawski.dev.backend.security.CustomUserDetailsService;
import com.morawski.dev.backend.security.JwtAuthenticationEntryPoint;
import com.morawski.dev.backend.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.morawski.dev.backend.util.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final CustomUserDetailsService userDetailsService;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	@Value("${spring.web.cors.allowed-origins}")
	private String[] allowedOrigins;

	@Value("${spring.web.cors.allowed-methods}")
	private String allowedMethods;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		log.info("Configuring Security Filter Chain with JWT authentication");

		http
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			)
			// Configure exception handling for authentication errors
			.exceptionHandling(exception -> exception
				.authenticationEntryPoint(jwtAuthenticationEntryPoint)
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
			)
			// Set custom authentication provider
			.authenticationProvider(authenticationProvider())
			// Add JWT filter before UsernamePasswordAuthenticationFilter
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

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

	/**
	 * Authentication provider bean.
	 * Configures how Spring Security authenticates users.
	 *
	 * Uses:
	 * - CustomUserDetailsService to load user from database
	 * - PasswordEncoder to validate credentials
	 *
	 * API Plan Section 3.2: User Login
	 * - Validates credentials against hashed password
	 *
	 * @return DaoAuthenticationProvider
	 */
	@Bean
	public AuthenticationProvider authenticationProvider() {
		log.info("Configuring DAO authentication provider");
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
		authProvider.setUserDetailsService(userDetailsService);
		authProvider.setPasswordEncoder(passwordEncoder());
		return authProvider;
	}

	/**
	 * Authentication manager bean.
	 * Used by authentication endpoints (login) to authenticate user credentials.
	 *
	 * API Plan Section 3.2: POST /api/auth/login
	 * - Authenticates user credentials
	 * - Returns JWT token on success
	 *
	 * @param config Authentication configuration
	 * @return AuthenticationManager
	 * @throws Exception if configuration fails
	 */
	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		log.info("Configuring authentication manager");
		return config.getAuthenticationManager();
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
