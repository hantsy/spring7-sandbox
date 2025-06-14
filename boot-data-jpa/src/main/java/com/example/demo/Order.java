package com.example.demo;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "orders")
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@Data
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    @ElementCollection(fetch = FetchType.EAGER)
    List<OrderItem> items;

    OrderStatus status = OrderStatus.PENDING;

    @Embedded
    @AttributeOverrides(value = {@AttributeOverride(name = "id", column = @Column(name = "customer_id"))})
    CustomerId customerId;

    @CreatedDate
    Instant createdAt;
}

enum OrderStatus {
    PENDING(0),
    COMPLETED(1),
    DELIVERED(2),
    CANCELED(-1);

    @EnumeratedValue
    final int intValue;

    OrderStatus(int intValue) {
        this.intValue = intValue;
    }
}

@Embeddable
record OrderItem(String name, int quantity, @ManyToOne Product product) {
    OrderItem {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        if (product == null) {
            throw new IllegalArgumentException("Product must not be null");
        }
    }
}