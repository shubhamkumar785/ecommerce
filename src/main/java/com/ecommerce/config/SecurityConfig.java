package com.ecommerce.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;

@Configuration
@EnableWebSecurity
@Order(1)
public class SecurityConfig {

	private static final String AUTH_COOKIE = "SHOPPING_CART_TOKEN";

	@Autowired
	private CustomAuthenticationSuccessHandler authenticationSuccessHandler;

	@Autowired
	@Lazy
	private AuthFailureHandlerImpl authenticationFailureHandler;

	@Autowired
	private CustomWebAuthenticationDetailsSource customWebAuthenticationDetailsSource;

	@Autowired
	private JwtAuthenticationFilter jwtAuthenticationFilter;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public UserDetailsService userDetailsService() {
		return new UserDetailsServiceImpl();
	}

	@Bean
	public DaoAuthenticationProvider authenticationProvider() {
		CustomAuthenticationProvider authenticationProvider = new CustomAuthenticationProvider();
		authenticationProvider.setUserDetailsService(userDetailsService());
		authenticationProvider.setPasswordEncoder(passwordEncoder());
		return authenticationProvider;
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
			throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		System.out.println("DEBUG: SecurityConfig filterChain loaded");
		AccessDeniedHandler accessDeniedHandler = (request, response, accessDeniedException) -> response
				.sendError(HttpServletResponse.SC_FORBIDDEN, "Unauthorized");

		http.csrf(csrf -> csrf.disable()).cors(cors -> cors.disable())
				.authenticationProvider(authenticationProvider())
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.authorizeHttpRequests(req -> req
						.requestMatchers(new AntPathRequestMatcher("/"), new AntPathRequestMatcher("/signin"), new AntPathRequestMatcher("/login"), new AntPathRequestMatcher("/register"), new AntPathRequestMatcher("/saveUser"), new AntPathRequestMatcher("/products"),
								new AntPathRequestMatcher("/product/**"), new AntPathRequestMatcher("/search"), new AntPathRequestMatcher("/forgot-password"), new AntPathRequestMatcher("/reset-password"), new AntPathRequestMatcher("/auth/google"),
								new AntPathRequestMatcher("/api/auth/login"), new AntPathRequestMatcher("/api/auth/signup"), new AntPathRequestMatcher("/api/otp/**"), new AntPathRequestMatcher("/api/products"), new AntPathRequestMatcher("/become-seller"), new AntPathRequestMatcher("/css/**"), new AntPathRequestMatcher("/js/**"), new AntPathRequestMatcher("/img/**"), new AntPathRequestMatcher("/static/**"), new AntPathRequestMatcher("/error"))
						.permitAll()
						.requestMatchers("/user/**").hasRole("USER")
						.requestMatchers("/seller/**").hasRole("SELLER")
						.requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
						.anyRequest().authenticated())
				.exceptionHandling(ex -> ex.accessDeniedHandler(accessDeniedHandler))
				.formLogin(form -> form.loginPage("/signin")
						.loginProcessingUrl("/login")
						.authenticationDetailsSource(customWebAuthenticationDetailsSource)
						// .defaultSuccessUrl("/")
						.failureHandler(authenticationFailureHandler)
						.successHandler(authenticationSuccessHandler)
						.permitAll())
				.logout(logout -> logout
						.logoutSuccessHandler((request, response, authentication) -> {
							ResponseCookie authCookie = ResponseCookie.from(AUTH_COOKIE, "")
									.httpOnly(true)
									.secure(request.isSecure())
									.sameSite("Lax")
									.path("/")
									.maxAge(0)
									.build();
							response.addHeader(HttpHeaders.SET_COOKIE, authCookie.toString());
							response.sendRedirect("/");
						})
						.permitAll());

		return http.build();
	}

}
