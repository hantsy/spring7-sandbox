package com.example.demo;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "customers")
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@Data
public class Customer {
    @EmbeddedId
    private CustomerId id;
    private String firstName;
    private String lastName;
    private String email;
}
