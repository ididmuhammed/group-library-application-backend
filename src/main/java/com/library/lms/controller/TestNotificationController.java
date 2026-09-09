package com.library.lms.controller;


import com.library.lms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TEMPORARY — for manually testing the automated-notifications feature
 * without waiting for the 08:00 cron to fire.
 *
 * DELETE THIS FILE before shipping. It has no @PreAuthorize, so as written
 * it's reachable by any authenticated user (the global rule in
 * SecurityConfig is anyRequest().authenticated()) — fine for a local test,
 * not for anything else.
 */
@RestController
@RequiredArgsConstructor
public class TestNotificationController {

    private final NotificationService notificationService;

    @GetMapping("/api/test/run-notifications")
    public ResponseEntity<String> runNotifications() {
        int reminders = notificationService.sendDueDateReminders();
        int overdue = notificationService.sendOverdueNotices();
        String result = "Sent " + reminders + " due-date reminder(s), " + overdue + " overdue notice(s).";
        return ResponseEntity.ok(result);
    }
}
