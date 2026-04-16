package com.example.demo;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumeratedValue;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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

    public static Order of(CustomerId customerId, List<OrderItem> items) {
        var order = new Order();
        order.setCustomerId(customerId);
        order.setItems(items);
        return order;
    }
}

enum OrderStatus {
    PENDING(0),
    COMPLETED(1),
    DELIVERED(2),
    CANCELED(-1);

    @EnumeratedValue
    final int code;

    OrderStatus(int code) {
        this.code = code;
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