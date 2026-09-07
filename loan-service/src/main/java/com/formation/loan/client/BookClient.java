package com.formation.loan.client;

import com.formation.loan.dto.BookDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Contrat Feign vers book-service. Contrairement au projet guide (order-service ne
 * faisait que LIRE via GET), ici on appelle aussi des endpoints d'ECRITURE :
 * un emprunt decrémente le stock, un retour le réincrémente.
 *
 * "book-service" est le nom logique enregistre dans Eureka (lb://book-service).
 */
@FeignClient(name = "book-service")
public interface BookClient {

    // ---- LECTURE ----
    @GetMapping("/api/books/{id}")
    BookDto getBookById(@PathVariable("id") Long id);

    // ---- ECRITURE (appels internes de book-service) ----
    @PatchMapping("/api/books/{id}/decrement-stock")
    BookDto decrementStock(@PathVariable("id") Long id);

    @PatchMapping("/api/books/{id}/increment-stock")
    BookDto incrementStock(@PathVariable("id") Long id);
}

