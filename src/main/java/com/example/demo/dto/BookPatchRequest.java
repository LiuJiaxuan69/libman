package com.example.demo.dto;

import java.math.BigDecimal;
import lombok.Data;

/**
 * Patch update DTO: any subset of fields may be provided (nulls ignored).
 */
@Data
public class BookPatchRequest {
    private String bookName;
    private String author;
    private BigDecimal price;
    private String publish;
    private String description;
    private String categoryIds;
    private String tags;
    private Integer status;
    private String coverUrl; // rarely used here; cover normally updated via /{id}/cover
}
