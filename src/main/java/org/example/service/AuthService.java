package org.example.service;

import org.example.dto.RegisterRequest;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.repository.UserRepository;
import org.example.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    public String login(String login, String password) {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("Неверный логин или пароль"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Неверный логин или пароль");
        }

        return jwtUtil.generateToken(user.getLogin(), user.getRole());
    }

    public String register(RegisterRequest request) {
        if (userRepository.findByLogin(request.getLogin()).isPresent()) {
            throw new RuntimeException("Пользователь уже существует");
        }

        User user = new User();
        user.setLogin(request.getLogin());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        userRepository.save(user);

        return jwtUtil.generateToken(user.getLogin(), user.getRole());
    }
}
