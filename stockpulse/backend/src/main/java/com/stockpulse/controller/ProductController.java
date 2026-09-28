package com.stockpulse.controller;

import com.stockpulse.domain.Category;
import com.stockpulse.domain.ProductStatus;
import com.stockpulse.dto.*;
import com.stockpulse.service.ProductService;
import com.stockpulse.service.SuggestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService products;
    private final SuggestionService suggestions;

    public ProductController(ProductService products, SuggestionService suggestions) {
        this.products = products;
        this.suggestions = suggestions;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody CreateProductRequest request) {
        return ProductResponse.from(products.create(request));
    }

    @GetMapping
    public List<ProductResponse> list(@RequestParam(required = false) ProductStatus status,
                                      @RequestParam(required = false) Category category) {
        return products.list(status, category).stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return ProductResponse.from(products.get(id));
    }

    @PatchMapping("/{id}/stock")
    public ProductResponse updateStock(@PathVariable Long id, @Valid @RequestBody StockUpdateRequest request) {
        return ProductResponse.from(products.updateStock(id, request.stockLevel()));
    }

    @PostMapping("/{id}/orders")
    public ProductResponse placeOrder(@PathVariable Long id, @Valid @RequestBody(required = false) OrderRequest request) {
        int quantity = request == null ? 1 : request.quantityOrDefault();
        return ProductResponse.from(products.placeOrder(id, quantity));
    }

    @PostMapping("/{id}/suggest-pricing")
    @ResponseStatus(HttpStatus.CREATED)
    public PricingSuggestionResponse suggestPricing(@PathVariable Long id) {
        return PricingSuggestionResponse.from(suggestions.requestPricing(id));
    }

    @PostMapping("/{id}/suggest-reorder")
    @ResponseStatus(HttpStatus.CREATED)
    public ReorderSuggestionResponse suggestReorder(@PathVariable Long id) {
        return ReorderSuggestionResponse.from(suggestions.requestReorder(id));
    }
}
