package com.example.userreactiveexample.controllers;

import com.example.userreactiveexample.models.User;
import com.example.userreactiveexample.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.http.MediaType;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@AutoConfigureWebTestClient
@Slf4j
class UserControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private UserRepository userRepository;


    @Autowired
    private DatabaseClient databaseClient;


    private List<User> initData(){
        return Arrays.asList(new User(null,"Utsav Sharma",30,10000),
                new User(null,"Anshul Agarwal",5,100000),
                new User(null,"Jaskaran Singh",40,10000000));
    }


    @BeforeEach
    public  void setup(){
        List<String> statements = Arrays.asList("DROP TABLE IF EXISTS users ;",
                "CREATE TABLE users ( id INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, name VARCHAR(100) NOT NULL, age integer,salary decimal);");

        statements.forEach(it -> databaseClient.sql(it)
                .fetch()
                .rowsUpdated()
                .block());

        userRepository.deleteAll()
                .thenMany(Flux.fromIterable(initData()))
                .flatMap(userRepository::save)
                .doOnNext(user ->{
                    System.out.println("User added: " + user);
                })
                .blockLast();
    }

    @Test
    void createUser(){
        User user = new User(null,"Balram Rathore",45,5555555);
        webTestClient.post().uri("/users").contentType(MediaType.valueOf(MediaType.APPLICATION_JSON_VALUE))
                .body(Mono.just(user),User.class)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.name").isEqualTo("Balram Rathore");
    }

    @Test
    void getAllUsersValidateResponse(){
        Flux<User> userFlux = webTestClient.get().uri("/users").exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
                .returnResult(User.class)
                .getResponseBody();
        StepVerifier.create(userFlux.log("Values received"))
                .expectNextCount(3)
                .verifyComplete();

    }

    @Test
    void getAllUsersValidateCount(){
        webTestClient.get().uri("/users").exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
                .expectBodyList(User.class)
                .hasSize(3)
                .consumeWith(user ->{
                    List<User> users = user.getResponseBody();
                    assert users != null;
                    users.forEach(u ->{
                        assertNotNull(u.getId());
                    });
                });
    }

    @Test
    void getUserById(){
        webTestClient.get().uri("/users".concat("/{userId}"),"1")
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name","Suman Das");
    }
    
    @Test
    void getUserById_NotFound(){
        webTestClient.get().uri("/users".concat("/{userId}"),"6")
                .exchange().expectStatus().isNotFound();
    }

    @Test
    void deleteUser(){
        webTestClient.delete().uri("/users".concat("/{userId}"),"1")
                .accept(MediaType.valueOf(MediaType.APPLICATION_JSON_VALUE))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Void.class);
    }
    @Test
    void updateUser(){
        double newsalary = 12345;
        User user = new User(null,"Kanta Ram",31,newsalary);
        webTestClient.put().uri("/users".concat("/{userId}"),"1")
                .contentType(MediaType.valueOf(MediaType.APPLICATION_JSON_VALUE))
                .accept(MediaType.valueOf(MediaType.APPLICATION_JSON_VALUE))
                .body(Mono.just(user),User.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.salary").isEqualTo(newsalary);
    }
    @Test
    void updateUser_notFound(){
        double newsalary = 12345;
        User user = new User(null,"Kanta Ram",31,newsalary);
        webTestClient.put().uri("/users".concat("/{userId}"),"6")
                .contentType(MediaType.valueOf(MediaType.APPLICATION_JSON_VALUE))
                .accept(MediaType.valueOf(MediaType.APPLICATION_JSON_VALUE))
                .body(Mono.just(user),User.class)
                .exchange()
                .expectStatus().isBadRequest();
    }
}
