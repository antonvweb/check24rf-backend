package org.example.userService.controller;

import jakarta.validation.Valid;
import org.example.common.repository.UserRepository;
import org.example.userService.dto.ChangeAltDataRequest;
import org.example.userService.dto.ChangeAltDataResponse;
import org.example.userService.dto.UserResponse;
import org.example.userService.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/change-data")
    public ResponseEntity<ChangeAltDataResponse> changeData(
            @Valid @RequestBody ChangeAltDataRequest request,
            @RequestHeader("Authorization") String authHeader) {

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ChangeAltDataResponse(false, "Отсутствует токен авторизации"));
            }

            String token = authHeader.replace("Bearer ", "");

            boolean isChanged = userService.changeAltData(request.getType(), request.getData(), token);

            if (isChanged) {
                return ResponseEntity.ok(new ChangeAltDataResponse(
                        true,
                        "Данные успешно изменены",
                        request.getType(),
                        request.getData()
                ));
            } else {
                return ResponseEntity.badRequest()
                        .body(new ChangeAltDataResponse(false, "Не удалось изменить данные"));
            }

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ChangeAltDataResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ChangeAltDataResponse(false, "Внутренняя ошибка сервера"));
        }
    }
}
