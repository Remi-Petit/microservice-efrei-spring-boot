package com.formation.booking.client;

import com.formation.booking.dto.FitnessClassDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Contrat Feign vers class-service : lecture d'un cours + reservation /
 * liberation de places (endpoints d'ecriture re-verifiant la regle metier).
 */
@FeignClient(name = "class-service", fallbackFactory = ClassClientFallbackFactory.class)
public interface ClassClient {

    @GetMapping("/api/classes/{id}")
    FitnessClassDto getById(@PathVariable("id") Long id);

    @PatchMapping("/api/classes/{id}/increment")
    FitnessClassDto increment(@PathVariable("id") Long id, @RequestParam("spots") int spots);

    @PatchMapping("/api/classes/{id}/decrement")
    FitnessClassDto decrement(@PathVariable("id") Long id, @RequestParam("spots") int spots);
}
