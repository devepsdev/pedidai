package com.pedidai.api.services;

import com.pedidai.api.entities.Order;

public interface NotificationService {

    void sendOrderNotification(Order order);
}
