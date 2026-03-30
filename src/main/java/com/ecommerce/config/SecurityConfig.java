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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
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
		AuthenticationEntryPoint forbiddenEntryPoint = new HttpStatusEntryPoint(HttpStatus.FORBIDDEN);
		AccessDeniedHandler accessDeniedHandler = (request, response, accessDeniedException) -> response
				.sendError(HttpServletResponse.SC_FORBIDDEN, "Unauthorized");

		http.csrf(csrf -> csrf.disable()).cors(cors -> cors.disable())
				.authenticationProvider(authenticationProvider())
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.authorizeHttpRequests(req -> req
						.requestMatchers("/", "/signin", "/login", "/register", "/saveUser", "/products",
								"/product/**", "/search", "/forgot-password", "/reset-password", "/auth/google",
								"/api/auth/login", "/api/auth/signup", "/css/**", "/js/**", "/img/**", "/static/**", "/error")
						.permitAll()
						.requestMatchers("/api/admin/**").hasRole("ADMIN")
						.requestMatchers("/user/**").hasRole("USER")
						.requestMatchers("/admin/**").hasRole("ADMIN")
						.requestMatchers("/seller/**").hasRole("SELLER")
						.anyRequest().authenticated())
				.exceptionHandling(ex -> ex
						.defaultAuthenticationEntryPointFor(forbiddenEntryPoint,
								new AntPathRequestMatcher("/admin/**"))
						.defaultAuthenticationEntryPointFor(forbiddenEntryPoint,
								new AntPathRequestMatcher("/api/admin/**"))
						.accessDeniedHandler(accessDeniedHandler))
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
							response.sendRedirect("/signin?logout");
						})
						.permitAll());

		return http.build();
	}

}
