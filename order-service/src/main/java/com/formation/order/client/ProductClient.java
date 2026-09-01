package com.formation.order.client;

import com.formation.order.dto.ProductDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * "product-service" est le nom logique enregistre dans Eureka (comme lb://).
 * Feign + Spring Cloud LoadBalancer resolvent cette adresse a chaque appel.
 */
@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ProductDto getProductById(@PathVariable("id") Long id);
}
