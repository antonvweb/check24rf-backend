package org.example.mcoService.repository;

import org.example.mcoService.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByPhone(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);
}