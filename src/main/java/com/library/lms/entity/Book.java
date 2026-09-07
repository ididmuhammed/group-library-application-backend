package com.library.lms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "books")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 150)
    private String author;

    @Column(unique = true, length = 20)
    private String isbn;

    @Column(length = 100)
    private String category;

    @Builder.Default
    @Column(nullable = false)
    private Integer totalCopies = 1;

    @Builder.Default
    @Column(nullable = false)
    private Integer availableCopies = 1;

    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal bookCopyPrice = BigDecimal.ZERO;
}
