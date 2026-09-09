package com.library.lms.repository;

import com.library.lms.entity.Book;
import com.library.lms.entity.Reservation;
import com.library.lms.entity.Reservation.ReservationStatus;
import com.library.lms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByUser(User user);

    List<Reservation> findByStatus(ReservationStatus status);

    List<Reservation> findByStatusAndPickupDeadlineBefore(ReservationStatus status, LocalDate date);

    Optional<Reservation> findFirstByBookAndStatusOrderByReservedDateAsc(Book book, ReservationStatus status);

    boolean existsByBookAndUserAndStatusIn(Book book, User user, List<ReservationStatus> statuses);

    void deleteByBook(Book book);
}
