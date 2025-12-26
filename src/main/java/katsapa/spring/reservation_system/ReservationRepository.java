package katsapa.spring.reservation_system;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface ReservationRepository extends JpaRepository<ReservationEntity, Long> {
    @Transactional
    @Modifying
    @Query(
            "update ReservationEntity r set r.userId = :userid, r.roomId = :roomId," +
                    " r.startDate = :srartDate, r.endDate = :endDate, r.status = :status where r.id = :id"
    )
    int updateAllFields(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("roomId") Long roomId,
            @Param("startDate")LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") ReservationStatus status
    );
}
