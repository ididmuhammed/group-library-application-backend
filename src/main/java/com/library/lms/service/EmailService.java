package com.library.lms.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class EmailService {

    private final JavaMailSender javaMailSender;

    public void sendUserCreatedEmail(
            String email,
            String fullName,
            String username,
            String password,
            Set<String> roles
    ) {

        try {
            MimeMessage message = javaMailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    false,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom("tabithamugide31@gmail.com");
            helper.setTo(email);

            helper.setSubject(
                    "Library Management System - Your Account Has Been Created"
            );

            String rolesText = roles.stream()
                    .map(this::formatRole)
                    .collect(Collectors.joining(", "));

            String loginUrl = "http://localhost:5173/login";

            try (var inputStream = Objects.requireNonNull(
                    EmailService.class.getResourceAsStream(
                            "/templates/mail-notification.html"
                    )
            )) {

                String template = new String(
                        inputStream.readAllBytes(),
                        StandardCharsets.UTF_8
                );

                String html = template
                        .replace("{{fullName}}", escape(fullName))
                        .replace("{{username}}", escape(username))
                        .replace("{{email}}", escape(email))
                        .replace("{{password}}", escape(password))
                        .replace("{{roles}}", escape(rolesText))
                        .replace("{{loginUrl}}", loginUrl);

                helper.setText(html, true);
            }

            javaMailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to create and send account creation email",
                    e
            );
        }
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(
                value == null ? "" : value
        );
    }

    private String formatRole(String role) {
        return role
                .replace("ROLE_", "")
                .replace("_", " ");
    }
}