
package org.example.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "usersdev")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    @Column(name = "phone_number_alt")
    private String phoneNumberAlt;

    @Column(name = "email")
    private String email;

    @Column(name = "email_alt")
    private String emailAlt;

    @Column(name = "telegram_chat_id")
    private String telegramChatId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_active")
    private boolean isActive;

    public void setPhoneNumber(String phoneNumber){
        this.phoneNumber = phoneNumber;
    }

    public String getPhoneNumber(){
        return this.phoneNumber;
    }

    public void setPhoneNumberAlt(String phoneNumberAlt){
        this.phoneNumberAlt = phoneNumberAlt;
    }

    public String getPhoneNumberAlt(){
        return this.phoneNumberAlt;
    }

    public void setEmail(String email){
        this.email = email;
    }

    public String getEmail(){
        return this.email;
    }

    public void setEmailAlt(String emailAlt){
        this.emailAlt = emailAlt;
    }

    public String getEmailAlt(){
        return this.emailAlt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getTelegramChatId() {
        return telegramChatId;
    }

    public UUID getId() {
        return id;
    }

    public void setTelegramChatId(String telegramChatId) {
        this.telegramChatId = telegramChatId;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}

