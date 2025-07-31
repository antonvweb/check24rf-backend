package org.example.userService.dto;

public class UserResponse {
    public String phoneNumber;

    public String phoneNumberAlt;

    public String email;

    public String emailAlt;

    public String telegramChatId;

    public String createdAt;

    public boolean isActive;

    public String getPhoneNumber(){
        return this.phoneNumber;
    }

    public String getPhoneNumberAlt(){
        return this.phoneNumberAlt;
    }

    public String getEmail(){
        return this.email;
    }

    public String getEmailAlt(){
        return this.emailAlt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getTelegramChatId() {
        return telegramChatId;
    }

    public boolean isActive() {
        return isActive;
    }
}
