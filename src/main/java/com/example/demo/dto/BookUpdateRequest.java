package com.example.demo.dto;

import java.math.BigDecimal;
import lombok.Data;

/**
 * Full update DTO: all mutable fields required (except optional description/tags/categoryIds/coverUrl).
 * Controller will map to BookInfo and perform ownership check.
 */
@Data
public class BookUpdateRequest {
    private String bookName;      // required
    private String author;        // required
    private BigDecimal price;     // required
    private String publish;       // required
    private String description;   // optional
    private String categoryIds;   // JSON array string, optional
    private String tags;          // JSON array string, optional
    private Integer status;       // optional (may allow owner to set NORMAL/FORBIDDEN)
    private String coverUrl;      // optional (usually updated via cover endpoint)
}
