package com.urlshortener.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
//Table
@Entity
@Table(name = "url_mappings",indexes = { 
    @Index(name = "idx_short_code",columnList = "short_code", unique = true),
    @Index(name = "idx_created_at",columnList = "created_at"),
    @Index(name = "idx_expires_at",columnList = "expires_at")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlMapping implements Serializable {
    public static final long serialVersionUID = 1L;

    //Primary Key
    @Id
    @Column(name = "id", nullable = false , updatable = false )
    private long id;
    //Short Code
    @Column(name = "short_code", nullable = true , unique = true, length = 10 , updatable = false)
    private String shortCode;
    //Original Url
    @Column(name = "original_url", nullable = false, columnDefinition = "TEXT")
    private String originalUrl;
    //Custom Alias
    @Column(name = "custom_alias" , unique = true , length = 50)
    private String customAlias;
    //Click Counter
    @Builder.Default
    @Column(name = "click_count" , nullable = false)
    private Long clickCount = 0L;
    //Active Flag
    @Builder.Default
    @Column(name = "is_active" , nullable = false)
    private Boolean isActive = true;
    //Expiry
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    //Audit Timestamps
    //Created
    @CreationTimestamp
    @Column(name = "created_at",nullable = false, updatable = false)
    private LocalDateTime createdAt;
    //Update
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updated_at;
    //Creator
    @Column(name = "created_by",length = 100)
    private String createdBy;
    //Helper Methods 
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
    public void incrementClickCount(){
        this.clickCount = (this.clickCount == null ? 0L : this.clickCount) + 1L;
    }
}
