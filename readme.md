# README

* java 25; java scripts! 
* jbang.dev
* spring boot 4 release time
* support schedule
* new jakarta ee 11 baseline 
* java 17 remains the same
* jspecify is everywhere. 
* well need to build a db. spring data jdbc. build a custom findByName method( we'll come back to this later!)
* let's try running all these. ill create a bean. how do i register it. I could use java config, but ill use `BeanRegistrar` instead.
* let's build a client to get cat facts
* i could use the RestClient or RestTemplate. good news, Spring Boot's modularized. so I can bring in spring-boot-starter-restclients.
* we want to call another service. might fail. let's use resilience methods. well show retryable but there's also ConcurrencyLimit
* let's build an api to export information about the dogs in the shelter.
* we have many conflicting versions so well use API versioning and a request parameter 
* nice! looking good. time to think about production. I'll need security. show the new spring security customizers and MFA support.
* i want to scale this so ill use virtual threads. works even better with java 24+ !! nothing new here, but worth noting. 
* i want ot compile as a graalvm image. but this is a snapshot so i need to register a hint. why? the new jackson 3 support! 
```java
hints.reflection().registerType(tools.jackson.databind.jsontype.NamedType.class);
hints.resources().registerPattern("static/resources/**");
```
* notice how the new registerType method takes a class.  
* now i want to run this in native mode.
* the aot repositories r awesome, too! but they'll need a JdbcDialect instance to work in native mode. hopefully this gets fixed in time for prodution

```java
registry.registerBean(JdbcPostgresDialect.class, c -> c.supplier(_ -> JdbcPostgresDialect.INSTANCE));
```
* now i think we're ready lets build! 