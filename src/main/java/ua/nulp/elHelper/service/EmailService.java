package ua.nulp.elHelper.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final static Logger LOGGER = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;

    @Async
    public void send(String to, String subject, String emailContent) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            helper.setText(emailContent, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom("denis.starchenko.26@gmail.com");

            mailSender.send(mimeMessage);
            LOGGER.info("Email sent to {}", to);
        } catch (MessagingException e) {
            LOGGER.error("failed to send email", e);
        }
    }
}
