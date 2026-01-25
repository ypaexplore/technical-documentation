package com.techlead.jpa.demo;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ARCHETYPE: JPA PERFORMANCE & CONCURRENCY LAB
 * -------------------------------------------
 * This project is a living documentation for Technical Leaders to demonstrate
 * common JPA pitfalls and their modern solutions.
 */
@SpringBootApplication
public class JpaPerformanceArchetype {

    public static void main(String[] args) {
        SpringApplication.run(JpaPerformanceArchetype.class, args);
    }

    @Bean
    CommandLineRunner seedData(AuthorRepository authorRepository, ProductRepository productRepository) {
        return args -> {
            for (int i = 1; i <= 5; i++) {
                Author author = new Author("Author " + i);
                author.getBooks().add(new Book("Book A" + i, author));
                author.getBooks().add(new Book("Book B" + i, author));
                authorRepository.save(author);
            }
            productRepository.save(new Product("Laptop", 10));
            System.out.println(">>> DATABASE SEEDED FOR DEMO <<<");
        };
    }
}

// ==========================================
// 1. DOMAIN MODELS (Entities)
// ==========================================

@Entity
@Getter @Setter @NoArgsConstructor
class Author {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Book> books = new ArrayList<>();

    public Author(String name) { this.name = name; }
}

@Entity
@Getter @Setter @NoArgsConstructor
class Book {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    private Author author;

    public Book(String title, Author author) {
        this.title = title;
        this.author = author;
    }
}

@Entity
@Getter @Setter @NoArgsConstructor
class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private Integer stock;

    @Version
    private Long version;

    public Product(String name, Integer stock) {
        this.name = name;
        this.stock = stock;
    }
}

@Entity
@Getter @Setter @NoArgsConstructor
class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String message;

    public AuditLog(String message) { this.message = message; }
}

// ==========================================
// 2. REPOSITORIES
// ==========================================

@Repository
interface AuthorRepository extends JpaRepository<Author, Long> {}

@Repository
interface ProductRepository extends JpaRepository<Product, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithPessimisticLock(Long id);
}

@Repository
interface AuditLogRepository extends JpaRepository<AuditLog, Long> {}

// ==========================================
// 3. SERVICE LAYER (LABS)
// ==========================================

@Service
@RequiredArgsConstructor
class AuditService {
    private final AuditLogRepository auditLogRepository;

    /**
     * PROPAGATION: REQUIRES_NEW
     * This starts a BRAND NEW transaction, suspending the current one.
     * PRO: If the main transaction fails, this audit log STILL SAVES.
     * CON: Consumes 2 database connections at once. Risk of connection pool exhaustion.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String message) {
        auditLogRepository.save(new AuditLog(message));
    }
}

@Service
@RequiredArgsConstructor
class PerformanceService {
    private final AuthorRepository authorRepository;
    private final ProductRepository productRepository;
    private final AuditService auditService;

    /**
     * LAB 4: Transaction Propagation Demo
     * ----------------------------------
     * SCENARIO: We want to update stock, but we ALWAYS want an Audit Log,
     * even if the stock update fails (e.g., due to an exception).
     */
    @Transactional // Defaults to REQUIRED
    public void processOrderWithAudit(Long productId) {
        // 1. Log attempt in a NEW transaction
        auditService.logAction("Attempting to process product " + productId);

        // 2. Main Logic
        Product p = productRepository.findById(productId).orElseThrow();
        p.setStock(p.getStock() - 1);

        // 3. Simulate failure
        if (p.getStock() < 0) {
            throw new RuntimeException("Insufficient stock! Main TX will rollback.");
        }
        
        // RESULT: Even if RuntimeException is thrown, AuditLog remains in DB 
        // because it was saved in a separate REQUIRES_NEW transaction.
    }

    /**
     * LEAD TIP: The "Self-Invocation" Pitfall.
     * If you call a @Transactional method from within the same class, 
     * Spring's Proxy is bypassed, and NO TRANSACTION will be created.
     */
    public void badMethod() {
        // THIS WILL FAIL to create a transaction!
        demoNPlusOneProblem(); 
    }

    @Transactional(readOnly = true)
    public void demoNPlusOneProblem() {
        authorRepository.findAll().forEach(a -> System.out.println(a.getBooks().size()));
    }
}
