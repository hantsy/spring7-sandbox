package com.example.demo;

import org.springframework.data.annotation.Id;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.math.BigDecimal;

@Table("products")
public record Product(@PrimaryKey() String id, String name, BigDecimal price) {
}
