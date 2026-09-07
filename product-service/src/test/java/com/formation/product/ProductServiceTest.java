package com.formation.product;

import com.formation.product.dto.ProductRequest;
import com.formation.product.dto.ProductResponse;
import com.formation.product.exception.ProductNotFoundException;
import com.formation.product.model.Product;
import com.formation.product.repository.ProductRepository;
import com.formation.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void findById_produitInexistant_leveProductNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(99L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_sauvegardeEtRetourneLeProduitCree() {
        ProductRequest request = new ProductRequest("Souris", "Souris optique", new BigDecimal("19.90"), 25);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(2L);
            return p;
        });

        ProductResponse result = productService.create(request);

        assertThat(result.id()).isEqualTo(2L);
        assertThat(result.name()).isEqualTo("Souris");
    }

    @Test
    void findAll_retourneTousLesProduits() {
        when(productRepository.findAll()).thenReturn(
                java.util.List.of(new Product("A", "desc A", new BigDecimal("1.00"), 1))
        );

        assertThat(productService.findAll()).hasSize(1);
    }

    @Test
    void update_produitInexistant_leveProductNotFoundException() {
        ProductRequest request = new ProductRequest("A", "desc", new BigDecimal("1.00"), 1);
        when(productRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(7L, request))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void delete_produitInexistant_leveProductNotFoundException() {
        when(productRepository.existsById(7L)).thenReturn(false);

        assertThatThrownBy(() -> productService.delete(7L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void update_produitExistant_modifieEtEnregistre() {
        Product existing = new Product("Souris", "Souris optique", new BigDecimal("19.90"), 25);
        existing.setId(1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductRequest request = new ProductRequest("Souris Pro", "Souris optique sans fil", new BigDecimal("24.90"), 30);

        ProductResponse result = productService.update(1L, request);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Souris Pro");
        assertThat(result.description()).isEqualTo("Souris optique sans fil");
        assertThat(result.price()).isEqualByComparingTo("24.90");
        assertThat(result.quantity()).isEqualTo(30);
    }

    @Test
    void delete_produitExistant_supprimeLeProduit() {
        when(productRepository.existsById(1L)).thenReturn(true);

        productService.delete(1L);

        verify(productRepository).deleteById(1L);
    }

    @Test
    void delete_produitInexistant_neSupprimePas() {
        when(productRepository.existsById(7L)).thenReturn(false);

        assertThatThrownBy(() -> productService.delete(7L))
                .isInstanceOf(ProductNotFoundException.class);

        verify(productRepository, never()).deleteById(any(Long.class));
    }
}
