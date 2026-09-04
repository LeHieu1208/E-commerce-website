package com.project.thuongmaidientu.Service;

import com.project.thuongmaidientu.Model.Product;
import com.project.thuongmaidientu.Repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductSearchService {
    private final ProductRepository productRepository;
    private final Map<String, List<Product>> cache = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, List<Product>> eldest) {
            return size() > 20;
        }
    };

    public ProductSearchService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        String normalized = keyword.trim().toLowerCase();
        if (cache.containsKey(normalized)) {
            return cache.get(normalized);
        }

        List<Product> matches = new ArrayList<>();
        for (Product product : productRepository.findAll()) {
            if (matches(product, normalized)) {
                matches.add(product);
            }
        }

        cache.put(normalized, matches);
        return matches;
    }

    private boolean matches(Product product, String keyword) {
        if (product == null) {
            return false;
        }
        String name = product.getName() == null ? "" : product.getName().toLowerCase();
        String description = product.getDescription() == null ? "" : product.getDescription().toLowerCase();
        String categoryName = product.getCategory() != null && product.getCategory().getName() != null
                ? product.getCategory().getName().toLowerCase()
                : "";
        return name.contains(keyword) || description.contains(keyword) || categoryName.contains(keyword);
    }
}
