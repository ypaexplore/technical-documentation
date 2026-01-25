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

    /**
     * DATA SEEDER: Prepares the H2 database with test data for the demo.
     */
    @Bean
    CommandLineRunner seedData(AuthorRepository authorRepository, ProductRepository productRepository) {
        return args -> {
            // Seed for N+1 Demo
            for (int i = 1; i <= 5; i++) {
                Author author = new Author("Author " + i);
                author.getBooks().add(new Book("Book A" + i, author));
                author.getBooks().add(new Book("Book B" + i, author));
                authorRepository.save(author);
            }

            // Seed for Locking Demo
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

    /**
     * PITFALL: Lazy initialization is standard. 
     * If accessed outside @Transactional, it throws LazyInitializationException.
     * If accessed in a loop, it causes N+1 Problem.
     */
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

    /**
     * FEATURE: Optimistic Locking.
     * Prevents "Lost Updates" without blocking the DB.
     * Hibernate checks this value before every UPDATE.
     */
    @Version
    private Long version;

    public Product(String name, Integer stock) {
        this.name = name;
        this.stock = stock;
    }
}

// ==========================================
// 2. REPOSITORIES (The Solutions)
// ==========================================

@Repository
interface AuthorRepository extends JpaRepository<Author, Long> {

    // ISSUE: Standard find all - leads to N+1 when iterating books
    List<Author> findAll();

    /**
     * SOLUTION A: Entity Graph.
     * Directs Hibernate to fetch the 'books' collection in the INITIAL query using a JOIN.
     */
    @EntityGraph(attributePaths = {"books"})
    @Query("SELECT a FROM Author a")
    List<Author> findAllWithBooksGraph();

    /**
     * SOLUTION B: Join Fetch (JPQL).
     * Explicitly joins the tables to avoid multiple select statements.
     */
    @Query("SELECT a FROM Author a LEFT JOIN FETCH a.books")
    List<Author> findAllWithBooksJoinFetch();
}

@Repository
interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * FEATURE: Pessimistic Locking.
     * Issues "SELECT ... FOR UPDATE". 
     * Useful for high-contention rows like Bank Balances or Inventory Stock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    Optional<Product> findByIdWithPessimisticLock(Long id);
}

// ==========================================
// 3. SERVICE LAYER (Business Logic & Transactions)
// ==========================================

@Service
@RequiredArgsConstructor
class PerformanceService {
    private final AuthorRepository authorRepository;
    private final ProductRepository productRepository;

    /**
     * DEMO: The N+1 Disaster.
     * Watch the console: You will see 1 query for Authors + 5 queries for Books.
     */
    @Transactional(readOnly = true)
    public void demoNPlusOneProblem() {
        List<Author> authors = authorRepository.findAll(); 
        for (Author author : authors) {
            // This line triggers a separate SQL query for each author!
            System.out.println("Author: " + author.getName() + " has " + author.getBooks().size() + " books.");
        }
    }

    /**
     * DEMO: The Solution.
     * Watch the console: You will see exactly ONE query with a LEFT OUTER JOIN.
     */
    @Transactional(readOnly = true)
    public void demoEntityGraphSolution() {
        List<Author> authors = authorRepository.findAllWithBooksGraph();
        for (Author author : authors) {
            System.out.println("Optimized Author: " + author.getName() + " has " + author.getBooks().size() + " books.");
        }
    }

    /**
     * DEMO: Transaction Timeout.
     * If the DB or logic takes > 2 seconds, the transaction will rollback automatically.
     */
    @Transactional(timeout = 2)
    public void demoTimeoutFeature(Long id) throws InterruptedException {
        Product p = productRepository.findById(id).orElseThrow();
        Thread.sleep(3000); // Simulate heavy work exceeding 2s
        p.setStock(p.getStock() - 1);
    }

    /**
     * DEMO: Optimistic Locking Failure.
     * If version is changed by another thread, this will throw OptimisticLockingFailureException.
     */
    @Transactional
    public void demoOptimisticLock(Long id, int newStock) {
        Product p = productRepository.findById(id).orElseThrow();
        p.setStock(newStock);
        // Hibernate executes: UPDATE product SET stock = ?, version = version + 1 WHERE id = ? AND version = ?
    }
}
