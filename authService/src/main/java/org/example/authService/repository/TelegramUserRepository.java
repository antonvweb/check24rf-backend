package org.example.authService.repository;

import org.example.authService.entity.TelegramUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с пользователями Telegram
 */
@Repository
public interface TelegramUserRepository extends JpaRepository<TelegramUser, Long> {

    Optional<TelegramUser> findByChatId(Long chatId);

    List<TelegramUser> findByIsActiveTrue();

    boolean existsByChatId(Long chatId);
}
