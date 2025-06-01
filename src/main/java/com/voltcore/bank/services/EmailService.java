package com.voltcore.bank.services;

import com.voltcore.bank.entities.Transaction;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Service class for sending email notifications.
 */
@Service
public class EmailService {
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends a transaction notification email to the account holder.
     */
    public void sendTransactionEmail(Transaction transaction) {
        String email = transaction.getAccount().getEmail();
        if (email == null || email.isEmpty()) {
            return; // Skip if no email is provided
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Transaction Notification");
        message.setText(
                "Dear " + transaction.getAccount().getAccountHolderName() + ",\n\n" +
                        "A transaction has been processed on your account:\n" +
                        "Type: " + transaction.getTransactionType() + "\n" +
                        "Amount: $" + transaction.getAmount() + "\n" +
                        "Payment Method: " + (transaction.getPaymentMethod() != null ? transaction.getPaymentMethod() : "N/A") + "\n" +
                        "Date: " + transaction.getTransactionDate() + "\n" +
                        "Description: " + transaction.getDescription() + "\n\n" +
                        "Thank you for banking with us!"
        );
        mailSender.send(message);
    }
}