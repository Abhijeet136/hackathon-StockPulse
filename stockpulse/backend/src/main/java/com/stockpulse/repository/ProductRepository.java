package com.stockpulse.repository;

import com.stockpulse.domain.Category;
import com.stockpulse.domain.Product;
import com.stockpulse.domain.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySku(String sku);

    /** Average velocity of the product's category peers, excluding the product itself. */
    @Query("select avg(p.demandVelocity) from Product p where p.category = :category and p.id <> :excludeId")
    Optional<Double> averageVelocityOfPeers(@Param("category") Category category, @Param("excludeId") Long excludeId);

    @Query("""
            select p from Product p
            where (:status is null or p.status = :status)
              and (:category is null or p.category = :category)
            order by p.sku
            """)
    List<Product> search(@Param("status") ProductStatus status, @Param("category") Category category);
}
