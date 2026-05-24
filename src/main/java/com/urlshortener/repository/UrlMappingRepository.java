package com.urlshortener.repository;

import com.urlshortener.entity.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long>{
        //Find By ShortCode
        Optional<UrlMapping> findByShortCode(String shortCode);
        //Find Active url by ShortCode
        Optional<UrlMapping> findByShortCodeAndIsActiveTrue(String shortCode);
        //Check If ShortCode Exists
        boolean existsByShortCode(String shortCode);
        //Find All Urls by User
        List<UrlMapping> findByCreatedByOrderByCreatedAtDesc(String createdBy);
        //Find Expired Urls
        List<UrlMapping> findByExpiresAtBeforeAndIsActiveTrue(LocalDateTime dateTime);
        //Increment Click Count
        @Modifying
        @jakarta.transaction.Transactional
        @Query("UPDATE UrlMapping u " + "SET u.clickCount = u.clickCount + 1 " + "WHERE  u.shortCode = :shortCode")
        int incrementClickCount(@Param("shortCode") String shortCode);
        //Deactivate Expired Urls
        @Modifying
        @jakarta.transaction.Transactional
        @Query("UPDATE UrlMapping u " + "SET u.isActive = false " + "WHERE u.expiresAt < :now " + "AND u.isActive = true")
        int deactivateExpiredUrls(@Param("now") LocalDateTime now);
        //Count Recent Urls
        @Query("SELECT COUNT(u) From UrlMapping u " + "WHERE u.createdAt >= :since " + "AND u.isActive = true")
        long countActiveUrlsCreatedAfter(@Param("since") LocalDateTime since);
        //Top Clicked Urls
        @Query(value = "SELECT * FROM url_mappings " + "WHERE isActive = true " + "ORDER BY click_count DESC "+"LIMIT :limit" , nativeQuery = true)
        List<UrlMapping> findTopByClickCount(@Param("limit") int limit) ;

}