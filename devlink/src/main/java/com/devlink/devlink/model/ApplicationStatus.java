package com.devlink.devlink.model;

public enum ApplicationStatus {
    PENDING,      // Ожидает рассмотрения
    ACCEPTED,     // Принят заказчиком
    REJECTED,     // Отклонен заказчиком
    WITHDRAWN     // Отозван исполнителем
}
