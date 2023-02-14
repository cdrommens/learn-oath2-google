package be.rommens.learningoath2;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 *
 */
@Configuration
public class SecurityConfiguration {

    private final OAuth2ResourceServerProperties properties;
    private final String clientId;
    private final UserRepository userRepository;

    public SecurityConfiguration(OAuth2ResourceServerProperties properties,
            @Value("${spring.security.oauth2.resourceserver.jwt.client-id}") String clientId,
            UserRepository userRepository) {
        this.properties = properties;
        this.clientId = clientId;
        this.userRepository = userRepository;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/open").permitAll()
                        .requestMatchers("/admin/**").hasAuthority("SCOPE_ADMINN")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt);
        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder nimbusJwtDecoder = NimbusJwtDecoder.withJwkSetUri(this.properties.getJwt().getJwkSetUri())
                .jwsAlgorithms(this::jwsAlgorithms).build();
        String issuerUri = this.properties.getJwt().getIssuerUri();
        //OAuth2TokenValidator<Jwt> audienceValidator = audienceValidator();
        //OAuth2TokenValidator<Jwt> userValidator = userValidator();
        OAuth2TokenValidator<Jwt> googleTokenValidator = new GoogleTokenValidator(clientId, userRepository);
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        //OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator, userValidator);
        OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(withIssuer, googleTokenValidator);

        nimbusJwtDecoder.setJwtValidator(withAudience);
        return nimbusJwtDecoder;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtDatabaseGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtDatabaseGrantedAuthoritiesConverter(userRepository);

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }

    private void jwsAlgorithms(Set<SignatureAlgorithm> signatureAlgorithms) {
        for (String algorithm : this.properties.getJwt().getJwsAlgorithms()) {
            signatureAlgorithms.add(SignatureAlgorithm.from(algorithm));
        }
    }

    static class GoogleTokenValidator implements OAuth2TokenValidator<Jwt> {
        private final String clientId;
        private final UserRepository userRepository;

        GoogleTokenValidator(String clientId, UserRepository userRepository) {
            this.clientId = clientId;
            this.userRepository = userRepository;
        }

        @Override
        public OAuth2TokenValidatorResult validate(Jwt token) {
            OAuth2Error wrongToken = new OAuth2Error("401", "Wrong token", null);
            OAuth2Error wrongUserid = new OAuth2Error("401", "Wrong userid", null);

            NetHttpTransport transport = new NetHttpTransport();
            JsonFactory factory = new GsonFactory();
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, factory)
                    .setAudience(List.of(clientId))
                    .build();
            try {
                GoogleIdToken idToken = verifier.verify(token.getTokenValue());
                if (idToken == null) {
                    return OAuth2TokenValidatorResult.failure(wrongToken);
                }
                return userRepository.getUser(token.getClaim("sub"))
                        .map(user -> OAuth2TokenValidatorResult.success())
                        .orElse(OAuth2TokenValidatorResult.failure(wrongUserid));
            } catch (GeneralSecurityException | IOException e) {
                return OAuth2TokenValidatorResult.failure(wrongToken);
            }
        }
    }

    /**
    OAuth2TokenValidator<Jwt> audienceValidator() {
        return new AudienceValidator(clientId);
    }

    OAuth2TokenValidator<Jwt> userValidator() {
        return new UserValidator(userRepository);
    }

    static class AudienceValidator implements OAuth2TokenValidator<Jwt> {

        private final String clientId;
        OAuth2Error error = new OAuth2Error("401", "Wrong ClientId", null);

        AudienceValidator(String clientId) {
            this.clientId = clientId;
        }

        @Override
        public OAuth2TokenValidatorResult validate(Jwt jwt) {
            if (jwt.getAudience().contains(this.clientId)) {
                return OAuth2TokenValidatorResult.success();
            } else {
                return OAuth2TokenValidatorResult.failure(error);
            }
        }
    }

    static class UserValidator implements OAuth2TokenValidator<Jwt> {

        private final UserRepository userRepository;
        OAuth2Error error = new OAuth2Error("401", "Wrong userid", null);

        UserValidator(UserRepository userRepository) {
            this.userRepository = userRepository;
        }


        @Override
        public OAuth2TokenValidatorResult validate(Jwt jwt) {
            return userRepository.getUser(jwt.getClaim("sub"))
                    .map(user -> OAuth2TokenValidatorResult.success())
                    .orElse(OAuth2TokenValidatorResult.failure(error));
        }
    }
    */

    static class JwtDatabaseGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

        private static final String DEFAULT_AUTHORITY_PREFIX = "SCOPE_";
        private final UserRepository userRepository;

        JwtDatabaseGrantedAuthoritiesConverter(UserRepository userRepository) {
            this.userRepository = userRepository;
        }

        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            User user = userRepository.getUser(jwt.getClaim("sub"))
                    .orElseThrow(() -> new IllegalStateException("User can't be not found at this point."));

            Collection<GrantedAuthority> grantedAuthorities = new ArrayList<>();
            for (String authority : user.authorities()) {
                grantedAuthorities.add(new SimpleGrantedAuthority(DEFAULT_AUTHORITY_PREFIX + authority));
            }
            return grantedAuthorities;
        }
    }

}
