package com.library.lms.service;

import com.library.lms.entity.BorrowRecord;
import com.library.lms.repository.BorrowRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Drives the library's automated email notifications:
 *  - a reminder the day before a loan is due back
 *  - a daily overdue notice for every day a loan stays overdue
 *  - releasing/reassigning reservation holds whose pickup window has lapsed
 *
 * All three run together once a day. Each item is emailed independently so
 * one failed send (bad address, mail server hiccup) never blocks the rest
 * of the batch — see EmailService, which logs and swallows send failures.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final BorrowRecordRepository borrowRecordRepository;
    private final ReservationService reservationService;
    private final EmailService emailService;

    /**
     * Runs once a day at 08:00 server time. Change the cron expression (or
     * externalize it via a @Value on app.notifications.cron) if you need a
     * different schedule.
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void runDailyNotifications() {
        log.info("Running daily library notification scan...");
        int reminders = sendDueDateReminders();
        int overdue = sendOverdueNotices();
        int expired = reservationService.expireStalePickups();
        log.info("Notification scan complete: {} due-date reminders, {} overdue notices, {} reservations expired",
                reminders, overdue, expired);
    }

    /**
     * Emails everyone whose loan is due back tomorrow and hasn't already
     * been reminded.
     */
    @Transactional
    public int sendDueDateReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<BorrowRecord> dueSoon = borrowRecordRepository
                .findByStatusAndDueDateAndDueReminderSentFalse(BorrowRecord.BorrowStatus.BORROWED, tomorrow);

        int sent = 0;
        for (BorrowRecord record : dueSoon) {
            try {
                boolean success = emailService.sendDueDateReminderEmail(
                        record.getBorrower().getEmail(),
                        record.getBorrower().getFullName(),
                        record.getBook().getTitle(),
                        record.getDueDate());
                if (success) {
                    record.setDueReminderSent(true);
                    borrowRecordRepository.save(record);
                    sent++;
                }
            } catch (Exception e) {
                log.error("Failed to send due-date reminder for borrow record {}", record.getId(), e);
            }
        }
        return sent;
    }

    /**
     * Emails everyone with a currently overdue loan, at most once per day
     * per loan.
     */
    @Transactional
    public int sendOverdueNotices() {
        LocalDate today = LocalDate.now();
        List<BorrowRecord> overdue = borrowRecordRepository
                .findByStatusAndDueDateBefore(BorrowRecord.BorrowStatus.BORROWED, today);

        int sent = 0;
        for (BorrowRecord record : overdue) {
            if (today.equals(record.getLastOverdueNotifiedDate())) {
                continue; // already notified today
            }
            try {
                long daysOverdue = ChronoUnit.DAYS.between(record.getDueDate(), today);
                boolean success = emailService.sendOverdueNoticeEmail(
                        record.getBorrower().getEmail(),
                        record.getBorrower().getFullName(),
                        record.getBook().getTitle(),
                        record.getDueDate(),
                        daysOverdue);
                if (success) {
                    record.setLastOverdueNotifiedDate(today);
                    borrowRecordRepository.save(record);
                    sent++;
                }
            } catch (Exception e) {
                log.error("Failed to send overdue notice for borrow record {}", record.getId(), e);
            }
        }
        return sent;
    }
}