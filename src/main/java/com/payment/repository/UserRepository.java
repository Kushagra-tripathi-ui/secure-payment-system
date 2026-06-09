package com.payment.repository;

import com.payment.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * USER REPOSITORY - Database access for User entity
 * 
 * CONCEPT: Spring Data JPA Repository
 * 
 * JpaRepository provides common CRUD operations:
 * - save(user): Insert or update
 * - findById(id): Get by primary key
 * - delete(user): Delete record
 * - findAll(): Get all records
 * 
 * We add custom methods for business logic:
 * - findByEmail: Search by email
 * - Spring automatically generates SQL from method name!
 * 
 * @Repository: Tells Spring this is a data access component
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Find user by email
     * Spring automatically generates: SELECT * FROM users WHERE email = ?
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Check if email exists
     * Prevents duplicate registrations
     */
    boolean existsByEmail(String email);
}
