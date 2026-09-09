package com.library.lms.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmailService {

    private static final String FROM_ADDRESS = "tabithamugide31@gmail.com";
    private static final String LOGIN_URL = "http://localhost:5173/login";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy");

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

            helper.setFrom(FROM_ADDRESS);
            helper.setTo(email);

            helper.setSubject(
                    "Library Management System - Your Account Has Been Created"
            );

            String rolesText = roles.stream()
                    .map(this::formatRole)
                    .collect(Collectors.joining(", "));

            String loginUrl = LOGIN_URL;

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

    /**
     * Reminder sent the day before a loan is due back.
     */
    public boolean sendDueDateReminderEmail(String email, String fullName, String bookTitle, LocalDate dueDate) {
        String html = buildNotificationHtml(
                "#1f4e79",
                "Due Date Reminder",
                "Hi " + escape(fullName) + ",",
                "This is a friendly reminder that your loan is due back tomorrow. "
                        + "Please return it on time to avoid a late fine.",
                "\"" + escape(bookTitle) + "\" is due on <strong>" + dueDate.format(DATE_FORMAT) + "</strong>."
        );
        String text = "Hi " + fullName + ",\n\n"
                + "This is a friendly reminder that your loan is due back tomorrow.\n\n"
                + "\"" + bookTitle + "\" is due on " + dueDate.format(DATE_FORMAT) + ".\n\n"
                + "Please return it on time to avoid a late fine.\n\n"
                + "- Library Management System";
        return send(email, "\"" + bookTitle + "\" is due back tomorrow", text, html);
    }

    /**
     * Overdue notice sent for a loan that's already past its due date.
     * Safe to call repeatedly (e.g. once per day) — the caller is
     * responsible for throttling.
     */
    public boolean sendOverdueNoticeEmail(String email, String fullName, String bookTitle,
                                          LocalDate dueDate, long daysOverdue) {
        String dayWord = daysOverdue == 1 ? "day" : "days";
        String html = buildNotificationHtml(
                "#b45309",
                "Overdue Notice",
                "Hi " + escape(fullName) + ",",
                "Our records show the loan below is now overdue. Fines accrue daily until it's returned, "
                        + "so please bring it back when you get a chance.",
                "\"" + escape(bookTitle) + "\" was due on <strong>" + dueDate.format(DATE_FORMAT)
                        + "</strong> &mdash; " + daysOverdue + " " + dayWord + " overdue."
        );
        String text = "Hi " + fullName + ",\n\n"
                + "Our records show the loan below is now overdue.\n\n"
                + "\"" + bookTitle + "\" was due on " + dueDate.format(DATE_FORMAT)
                + " (" + daysOverdue + " " + dayWord + " overdue).\n\n"
                + "Fines accrue daily until it's returned, so please bring it back when you get a chance.\n\n"
                + "- Library Management System";
        return send(email, "\"" + bookTitle + "\" is now overdue", text, html);
    }

    /**
     * Sent when a reserved copy has been set aside and is ready for pickup.
     */
    public boolean sendReservationAvailableEmail(String email, String fullName, String bookTitle,
                                                 LocalDate pickupDeadline) {
        String html = buildNotificationHtml(
                "#15803d",
                "Reservation Ready for Pickup",
                "Hi " + escape(fullName) + ",",
                "Good news — a copy of your reserved book is now available. "
                        + "We're holding it for you, but only until the pickup deadline below.",
                "\"" + escape(bookTitle) + "\" is ready for pickup. Please collect it by "
                        + "<strong>" + pickupDeadline.format(DATE_FORMAT) + "</strong>, "
                        + "or the hold will be released to the next person in line."
        );
        String text = "Hi " + fullName + ",\n\n"
                + "Good news - a copy of your reserved book is now available.\n\n"
                + "\"" + bookTitle + "\" is ready for pickup. Please collect it by "
                + pickupDeadline.format(DATE_FORMAT) + ", or the hold will be released to the next person in line.\n\n"
                + "- Library Management System";
        return send(email, "\"" + bookTitle + "\" is ready for pickup", text, html);
    }

    /**
     * @return true if the message was handed off to the mail server successfully,
     *         false if sending failed (the failure is logged with full detail either way).
     */
    private boolean send(String toEmail, String subject, String plainText, String html) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            // multipart=true + setText(plain, html) produces a proper
            // text/plain + text/html alternative message instead of an
            // HTML-only one — spam filters trust multipart mail more, and
            // it gives clients without HTML rendering something readable.
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(FROM_ADDRESS);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(plainText, html);
            javaMailSender.send(message);
            return true;
        } catch (Exception e) {
            // Notification emails must never take down the batch/scheduled job
            // that triggered them, so we log and swallow rather than rethrow —
            // but the caller still gets a false return so it can decide whether
            // to mark the record as notified (it shouldn't, if this failed).
            log.error("Failed to send notification email '{}' to {}", subject, toEmail, e);
            return false;
        }
    }

    /**
     * Small shared template used by all automated notification emails
     * (due-date reminders, overdue notices, reservation pickups). Kept as a
     * simple inline builder rather than a file-based template since the
     * content is short and fully dynamic.
     */
    private String buildNotificationHtml(String accentColor, String heading, String greeting,
                                         String intro, String detailHtml) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:0;background-color:#f4f6f8;font-family:Arial,Helvetica,sans-serif;color:#333333;">
                <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="background-color:#f4f6f8;padding:40px 15px;">
                <tr><td align="center">
                <table width="600" cellpadding="0" cellspacing="0" border="0" style="max-width:600px;width:100%%;background-color:#ffffff;border-radius:10px;overflow:hidden;box-shadow:0 2px 10px rgba(0,0,0,0.08);">
                <tr><td style="background-color:%s;padding:30px;text-align:center;">
                <h1 style="margin:0;color:#ffffff;font-size:24px;font-weight:600;">Library Management System</h1>
                <p style="margin:8px 0 0;color:#f0f0f0;font-size:14px;">%s</p>
                </td></tr>
                <tr><td style="padding:35px 40px;">
                <p style="font-size:15px;line-height:1.7;margin:0 0 15px;">%s</p>
                <p style="font-size:15px;line-height:1.7;margin:0 0 20px;">%s</p>
                <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="background-color:#f8fafc;border:1px solid #e2e8f0;border-left:4px solid %s;border-radius:8px;margin-bottom:10px;">
                <tr><td style="padding:16px 20px;font-size:15px;line-height:1.6;">%s</td></tr>
                </table>
                <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="margin-top:25px;">
                <tr><td align="center">
                <a href="%s" style="display:inline-block;padding:12px 26px;background-color:%s;color:#ffffff;text-decoration:none;border-radius:6px;font-size:15px;font-weight:bold;">Open Library System</a>
                </td></tr>
                </table>
                </td></tr>
                <tr><td style="background-color:#f8fafc;border-top:1px solid #e5e7eb;padding:20px 30px;text-align:center;">
                <p style="margin:0;font-size:13px;color:#64748b;">This is an automated message from the Library Management System. Please do not reply directly to this email.</p>
                </td></tr>
                </table>
                </td></tr>
                </table>
                </body>
                </html>
                """.formatted(accentColor, heading, greeting, intro, accentColor, detailHtml, LOGIN_URL, accentColor);
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