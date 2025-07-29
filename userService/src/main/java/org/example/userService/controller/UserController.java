package org.example.userService.controller;

import org.example.userService.dto.UserResponse;
import org.example.userService.repository.UserRepository;
import org.example.userService.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired private UserService userService;
    @Autowired private UserRepository userRepo;

    @GetMapping("/is-active")
    public ResponseEntity<?> getUserIsActive(@RequestHeader("Authorization") String authHeader){
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authHeader.replace("Bearer ", "");

        return ResponseEntity.ok(Map.of(
                "isActive", userService.getUserIsActive(token)
        ));
    }

    @GetMapping("/user")
    public ResponseEntity<UserResponse> getUser(@RequestHeader("Authorization") String authHeader){
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authHeader.replace("Bearer ", "");

        return ResponseEntity.ok(userService.getUser(token));
    }
}
