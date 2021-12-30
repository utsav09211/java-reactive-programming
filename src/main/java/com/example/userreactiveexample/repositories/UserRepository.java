package com.example.userreactiveexample.repositories;

import com.example.userreactiveexample.models.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface UserRepository extends ReactiveCrudRepository<User,Integer> {

    Flux<User> findUsersByAge(int age);
}
