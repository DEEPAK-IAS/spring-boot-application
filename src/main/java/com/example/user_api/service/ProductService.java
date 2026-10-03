package com.example.user_api.service;

import com.example.user_api.dto.ProductDTO;
import com.example.user_api.entity.Product;
import com.example.user_api.entity.ProductImage;
import com.example.user_api.repository.ProductRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Cacheable(value = "productList")
    public List<Product> getAllProducts() {
        System.out.println("Fetching all products from the database");
        return productRepository.findAll();
    }

    @Cacheable(value = "product", key = "#id")
    public Product getProductById(Long id) {
        System.out.println("Fetching product ID " + id + " from the Database...");
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    @CacheEvict(value = "productList", allEntries = true)
    public Product createProduct(ProductDTO dto) {

        logger.info("Attempting to create a new product with name: {}", dto.getName()); 

        Product product = new Product();
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStockQuantity(dto.getStockQuantity());

        // Loop through the provided URLs and map them to ProductImage entities
        if (dto.getImageUrls() != null) {
            for (String url : dto.getImageUrls()) {
                ProductImage productImage = new ProductImage(url, product);
                product.getImages().add(productImage);
            }
        }

        Product savedProduct = productRepository.save(product);
        logger.info("Successfully created product with ID: {}", savedProduct.getId());

        return savedProduct;
    }

    @CacheEvict(value = "productList", allEntries = true)
    @CachePut(value = "product", key = "#id")
    public Product updateProduct(Long id, ProductDTO dto) {
        Product product = getProductById(id);
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStockQuantity(dto.getStockQuantity());
        return productRepository.save(product);
    }

    @CacheEvict(value = {"product", "productList"}, allEntries = true)
    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        productRepository.delete(product);
    }
}