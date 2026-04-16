package com.example.demo;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@Data
@EntityListeners(AuditingEntityListener.class) // apply it globally in orm.xml
public class Customer {
    @EmbeddedId
    private CustomerId id;
    private String firstName;
    private String lastName;
    private String email;

    @CreatedDate
    private LocalDateTime createdAt;
    @CreatedBy
    private String createdBy;

    @LastModifiedDate
    private LocalDateTime updatedAt;
    @LastModifiedBy
    private String updatedBy;

    public static Customer of(CustomerId customerId, String firstName, String lastName, String email) {
        return new Customer(customerId, firstName, lastName, email, null, null, null, null);
    }
}
