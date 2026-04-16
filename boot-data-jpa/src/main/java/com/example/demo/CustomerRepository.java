package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, CustomerId> {

    @Query("select c from Customer c where c.firstName like :name or c.lastName like :name")
    List<Customer> findByName(String name);
}
