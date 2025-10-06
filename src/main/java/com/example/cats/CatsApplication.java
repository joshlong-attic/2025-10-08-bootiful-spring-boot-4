package com.example.cats;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.BeanRegistrar;
import org.springframework.beans.factory.BeanRegistry;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.core.dialect.JdbcDialect;
import org.springframework.data.jdbc.core.dialect.JdbcPostgresDialect;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collection;
import java.util.Map;

@SpringBootApplication
@Import(DogBeanRegistrar.class)
public class CatsApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatsApplication.class, args);
    }

    @Bean
    JdbcPostgresDialect myJdbcPostgresDialect (){
        return JdbcPostgresDialect.INSTANCE;
    }


}


@Controller
@ResponseBody
class DogsController {

    private final DogRepository repository;

    DogsController(@NonNull DogRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/dogs")
    Collection<Map<String, Object>> dogs() {
        return this.repository
                .findAll()
                .stream()
                .map(d -> Map.of("id", d.id(), "name", (Object) d.name()))
                .toList();
    }
}

record Runner(DogRepository repository) implements ApplicationRunner {

    @Override
    public void run(@NonNull ApplicationArguments args) throws Exception {
        this.repository.findAll().forEach(System.out::println);
    }
}


class DogBeanRegistrar implements BeanRegistrar {

    @Override
    public void register(@NonNull BeanRegistry registry, @NonNull Environment env) {
        registry.registerBean(Runner.class);
    }
}

interface DogRepository
        extends ListCrudRepository<@NonNull Dog, @NonNull Integer> {
}

record Dog(@Id int id, String description, String name) {
}
