package org.example.userService.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.userService.entity.User;
import org.example.userService.repository.UserRepository;
import org.example.userService.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepo;
    @Autowired private JwtUtil jwtUtil;

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    public boolean getUserIsActive(String token){
        UUID userId = UUID.fromString(String.valueOf(jwtUtil.getUserId(token)));

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        return user.isActive();
    }
}
