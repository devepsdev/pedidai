package com.pedidai.api.services;

public interface EmailService {

    void sendPasswordResetEmail(String to, String token, String userName);

    void sendEmailVerification(String to, String token, String userName);

    void sendCompanyAdminVerification(String to, String token, String userName, String companyName);

    void sendOrderNotification(String to, String supplierName, String companyName,
                               String companyAddress, String companyPhone,
                               String orderName, String orderDetails,
                               String notes);
}