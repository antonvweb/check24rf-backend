package org.example.dto;

import org.example.entity.Role;

public class RegisterRequest {
    private String login;
    private String password;
    private Role role;

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public Role getRole() {
        return role;
    }
}

