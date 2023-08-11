package com.project.vinylata.Service;

import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import java.util.Properties;

@Service
public class EmailService {

    private Session createSession(){
        // Create a session with the SMTP server
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");

        return Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("************@gmail.com", "powpafcbtoyrltkm");
            }
        });
    }

    public void sendOrderConfirmationEmail(String recipient, String orderDetails) throws MessagingException {

        Session session = createSession();
        // Create and configure the email message
        MimeMessage message = new MimeMessage(session);
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setSubject("Order Confirmation");
        helper.setTo(recipient);
        helper.setText("Dear customer,<br/><br/>Thank you for your purchase." +
                "<br/>Here are your order details:<br/>" + orderDetails, true);

        // Send the message using the SMTP server
        Transport.send(message);
    }

    public void sendOfficialOrderConfirmationEmail(String recipient, String orderDetails) throws MessagingException{
        Session session = createSession();
        // Create and configure the email message
        MimeMessage message = new MimeMessage(session);
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setSubject("Order Confirmation");
        helper.setTo(recipient);
        helper.setText("Dear customer,<br/>" +
                "<br/>Your order has been confirmed and will be delivered to you as soon as possible." +
                "<br/>Here are your order details:" +
                "<br/>" + orderDetails,true);

        // Send the message using the SMTP server
        Transport.send(message);
    }

    public void sendCanceledOrderConfirmationEmail(String recipient, String orderDetails) throws MessagingException{
        Session session = createSession();
        // Create and configure the email message
        MimeMessage message = new MimeMessage(session);
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setSubject("Order Cancelation");
        helper.setTo(recipient);
        helper.setText("Dear customer,<br/>" +
                "<br/>We are pity for canceling your order because of our policy!" +
                "<br/>Here are your order details:" +
                "<br/>" + orderDetails,true);

        // Send the message using the SMTP server
        Transport.send(message);
    }
}
