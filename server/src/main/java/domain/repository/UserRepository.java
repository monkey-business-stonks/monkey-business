package domain.repository;

import domain.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    // Authenticate user
    Optional<User> findByUsername(String username);
    
    // Check email uniqueness
    Optional<User> findByEmail(String email);
    
    // Check if username already exists
    boolean existsByUsername(String username);
    
    // Check if email already exists
    boolean existsByEmail(String email);
}
