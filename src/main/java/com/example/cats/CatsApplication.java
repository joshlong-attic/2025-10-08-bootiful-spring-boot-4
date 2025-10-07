package com.example.cats;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.factory.BeanRegistrar;
import org.springframework.beans.factory.BeanRegistry;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.env.Environment;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.core.dialect.JdbcPostgresDialect;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authorization.EnableGlobalMultiFactorAuthentication;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthorities;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.registry.ImportHttpServices;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * todo JmsClient
 */
@EnableGlobalMultiFactorAuthentication(authorities = {
        GrantedAuthorities.FACTOR_OTT_AUTHORITY,
//        GrantedAuthorities.FACTOR_WEBAUTHN_AUTHORITY,
        GrantedAuthorities.FACTOR_PASSWORD_AUTHORITY
})
@SpringBootApplication
@EnableResilientMethods
@ImportRuntimeHints(CatsApplication.MyHints.class)
@ImportHttpServices(CatFacts.class)
@Import(CatsApplication.MyBeanRegistrar.class)
public class CatsApplication {

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    InMemoryUserDetailsManager inMemoryUserDetailsManager(PasswordEncoder passwordEncoder) {
        return new InMemoryUserDetailsManager(
                User
                        .withUsername("josh")
                        .password(passwordEncoder.encode("pw"))
                        .roles("USER")
                        .build()
        );
    }

    @Bean
    Customizer<HttpSecurity> httpSecurityCustomizer() {
        return httpSecurity ->
                httpSecurity
                        .oneTimeTokenLogin(ott -> ott
                                .tokenGenerationSuccessHandler((_, response, oneTimeToken) -> {
                                    response.getWriter().println("you've got console mail!");
                                    response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE);
                                    IO.println("please go to http://localhost:8080/login/ott?token=" + oneTimeToken.getTokenValue());
                                }))
                        .webAuthn(r -> r
                                .allowedOrigins("http://localhost:8080")
                                .rpName("bootiful")
                                .rpId("localhost")
                        );
    }

    static class MyBeanRegistrar implements BeanRegistrar {

        @Override
        public void register(@NonNull BeanRegistry registry, @NonNull Environment env) {
            registry.registerBean(Runner.class);
            registry.registerBean(RiskyClient.class);
            registry.registerBean(JdbcPostgresDialect.class, c -> c
                    .supplier(_ -> JdbcPostgresDialect.INSTANCE));

        }
    }

    /* bleargh. */
    static class MyHints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
            hints.reflection().registerType(tools.jackson.databind.jsontype.NamedType.class);
            hints.resources().registerPattern("static/resources/**");
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(CatsApplication.class, args);
    }
}

class UnknownHostException extends Exception {
}

class RiskyClient {

    private final AtomicInteger counter = new AtomicInteger();

    @ConcurrencyLimit(10)
    @Retryable(maxAttempts = 4, includes = UnknownHostException.class)
    void connect() throws UnknownHostException {
        if (this.counter.incrementAndGet() < 3) {
            IO.println("Failed to connect");
            throw new UnknownHostException();
        }

        IO.println("Connected");
    }
}

record CatFact(@JsonProperty("fact_number") int factNumber, String fact) {
}

record CatFactsResults(Collection<CatFact> facts) {
}

interface CatFacts {

    @GetExchange("https://www.catfacts.net/api/")
    CatFactsResults facts();
}

@Controller
@ResponseBody
class DogsController {

    private final DogRepository repository;

    DogsController(@NonNull DogRepository repository) {
        this.repository = repository;
    }

    @GetMapping(value = "/dogs" ,version = "1.1")
    Collection<Dog> dogs() {
        return this.repository.findAll();
    }

    @GetMapping(value = "/dogs", version = "1.0")
    Collection<Map<String, Object>> dogsClassic() {
        return this.repository
                .findAll()
                .stream()
                .map(d -> Map.of("id", d.id(), "name", (Object) d.name()))
                .toList();
    }
}

record Runner(DogRepository repository, RiskyClient riskyClient, CatFacts catFacts)
        implements ApplicationRunner {

    @Override
    public void run(@NonNull ApplicationArguments args) throws Exception {
        this.repository.findAll().forEach(IO::println);
        this.repository.findByName("Prancer").forEach(IO::println);
        this.catFacts.facts().facts().forEach(IO::println);
        this.riskyClient.connect();
    }
}


interface DogRepository
        extends ListCrudRepository<@NonNull Dog, @NonNull Integer> {

    @Query("SELECT * FROM DOG WHERE NAME = :name")
    Collection<Dog> findByName(@NonNull String name);
}

record Dog(@Id int id, String description, String name) {
}
