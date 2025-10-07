package com.example.cats;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.BeanRegistrar;
import org.springframework.beans.factory.BeanRegistry;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.core.dialect.JdbcPostgresDialect;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.resilience.annotation.Retryable;
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
 * todo Resilience Methods
 */

@SpringBootApplication
@ImportHttpServices(CatFacts.class)
@Import(DogBeanRegistrar.class)
public class CatsApplication {

    static void main(String[] args) {
        SpringApplication.run(CatsApplication.class, args);
    }
}

class UnknownHostException extends Exception {
}

class RiskyClient {

    private final AtomicInteger counter = new AtomicInteger();

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

    @GetMapping(value = "/dogs", version = "1.1")
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

        this.riskyClient.connect();

        this.repository.findAll().forEach(IO::println);

        this.catFacts.facts().facts().forEach(IO::println);
    }
}


class DogBeanRegistrar implements BeanRegistrar {

    @Override
    public void register(@NonNull BeanRegistry registry, @NonNull Environment env) {
        registry.registerBean(Runner.class);
        registry.registerBean(RiskyClient.class);
        registry.registerBean(JdbcPostgresDialect.class, c -> c
                .supplier(_ -> JdbcPostgresDialect.INSTANCE));
    }
}

interface DogRepository
        extends ListCrudRepository<@NonNull Dog, @NonNull Integer> {
}

record Dog(@Id int id, String description, String name) {
}
