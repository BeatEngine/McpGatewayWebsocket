package org.beatengine.mcpgateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    public SecurityConfiguration(AuthenticationProvider authenticationProvider) {
        this.authenticationProvider = authenticationProvider;
    }

    private static final String[] WHITE_LIST_URL = {
            "/", // redirect to /index.html is configured.
            "/index.html",
            "/auth/**",
            "/css/**",
            "/js/**",
            "/img/**",
            "/api/**",
            "/favicon.ico",
            "/ws/**", /* WEBSOCKET API */
            "/springwolf/**", /* springwolf websocket api documentation */
            "/webjars/**", /* springwolf websocket api documentation */
            "/api/v1/test/**",
            "/v3/api-docs/**", /* OpenApi //todo only for debug  */
            "/swagger-ui/**" /* //todo only for debug  */
    };

    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(new Customizer<CsrfConfigurer<HttpSecurity>>() {
                    @Override
                    public void customize(CsrfConfigurer<HttpSecurity> httpSecurityCsrfConfigurer) {
                        httpSecurityCsrfConfigurer.disable();
                    }
                })
                .authorizeHttpRequests(req ->
                        req
                                .requestMatchers(WHITE_LIST_URL)
                                .permitAll().dispatcherTypeMatchers().permitAll()
                                .anyRequest()
                                .fullyAuthenticated()
                )
                .csrf(AbstractHttpConfigurer::disable).headers(headers -> headers.contentTypeOptions(
                        HeadersConfigurer.ContentTypeOptionsConfig::disable // Springwolf used For JavaScript imports with different content-type
                ))
                /* Session management only for websocket in org.beatengine.mcpgateway.websocket.CookieHandshakeInterceptor */
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
        ;

        return http.build();
    }
}
