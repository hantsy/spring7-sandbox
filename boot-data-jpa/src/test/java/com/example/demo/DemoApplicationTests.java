package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.support.WindowIterator;

import java.math.BigDecimal;
import java.util.List;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Slf4j
class DemoApplicationTests {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void setup() {
        productRepository.deleteAllInBatch();
    }

    @Test
    public void testCreateOrder() {
        var customerId = new CustomerId();
        var customer = customerRepository.save(Customer.of(customerId, "Foo", "bar", "foobar@example.com"));
        var apple = productRepository.save(Product.of(null, "Apple", BigDecimal.ONE));
        var orange = productRepository.save(Product.of(null, "Orange", BigDecimal.TEN));

        var order = orderRepository.save(
                Order.of(null,
                        List.of(
                                new OrderItem(apple.name, 10, apple),
                                new OrderItem(orange.name, 5, orange)
                        ),
                        OrderStatus.PENDING,
                        customerId,
                        null
                )
        );

        log.debug("saved order: {}", order);
    }

    @Test
    public void testScrollAPI() {
        productRepository.saveAllAndFlush(
                List.of(
                        Product.of(null, "Apple", BigDecimal.ONE),
                        Product.of(null, "Orange", BigDecimal.TEN),
                        Product.of(null, "WaterMelon", BigDecimal.TEN),
                        Product.of(null, "Melon", BigDecimal.TEN),
                        Product.of(null, "Tomato", new BigDecimal("4.5"))
                )
        );

        var products = productRepository.findFirst10ByNameContains("Melon", ScrollPosition.offset(0));

        do {
            for (Product product : products) {
                log.debug("found product: {}", product);
            }

            products = productRepository.findFirst10ByNameContains("Apple", products.positionAt(products.size() - 1));

        } while (!products.isEmpty() && products.hasNext());

        var productWindowIterator = WindowIterator.of(position -> productRepository.findFirst10ByNameContains("Apple", position))
                .startingAt(ScrollPosition.offset());

        while (productWindowIterator.hasNext()) {
            var product = productWindowIterator.next();
            log.debug("product windows iterator: {}", product);
        }

        var byExamples = productRepository.findBy(
                Example.of(
                        Product.of(null, "Melon", null),
                        ExampleMatcher
                                .matching()
                                .withIgnoreNullValues()
                                .withMatcher("name", ExampleMatcher.GenericPropertyMatcher::contains)
                ),
                q -> q.limit(10).scroll(ScrollPosition.offset())
        );
        do {
            for (Product product : byExamples) {
                log.debug("productByExample windows iterator: {}", product);
            }

        } while (!byExamples.isEmpty() && byExamples.hasNext());

    }


}
