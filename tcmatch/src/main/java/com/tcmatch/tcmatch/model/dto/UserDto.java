package com.tcmatch.tcmatch.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long chatId;
    private String userName;
    private String firstName;
    private String lastName;
    private Integer messageId;

    // 🔥 КОНСТРУКТОР ДЛЯ УДОБСТВА
    public UserDto(Long chatId, String userName, String firstName, String lastName) {
        this.chatId = chatId;
        this.userName = userName;
        this.firstName = firstName;
        this.lastName = lastName;
        this.messageId = null;
    }

    // 🔥 МЕТОД ДЛЯ ФОРМАТИРОВАНИЯ ИМЕНИ
    public String getDisplayName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (userName != null) {
            return "@" + userName;
        } else {
            return "Пользователь";
        }
    }
}