package com.tcmatch.tcmatch.model.enums;

public enum UserState {
    NONE,
    WAITING_GITHUB_URL,      // 🔥 Просто флаг, что ждем GitHub URL
    WAITING_SUPPORT_MESSAGE, // Можем добавить для поддержки
    // ... другие состояния
}