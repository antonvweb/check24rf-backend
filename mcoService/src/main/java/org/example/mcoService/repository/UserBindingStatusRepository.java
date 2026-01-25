package org.example.mcoService.repository;

import org.example.mcoService.entity.UserBindingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserBindingStatusRepository extends JpaRepository<UserBindingStatus, UUID> {

    Optional<UserBindingStatus> findByPhoneNumber(String phoneNumber);
}