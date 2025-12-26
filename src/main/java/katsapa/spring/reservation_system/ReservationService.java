package katsapa.spring.reservation_system;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReservationService {

    private final ReservationRepository repository;
    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);
    ReservationService(ReservationRepository repository){
        this.repository = repository;
    }

    public Reservation getReservationByID(Long id)  {
        ReservationEntity reservationEntity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Not found reservation by id = " + id));
        return toDomainReservation(reservationEntity);
    }

    public List<Reservation> findAllReservations() {
        List<ReservationEntity> allReservations = repository.findAll();
        return allReservations.stream()
                .map(this::toDomainReservation)
                .toList();
    }

    public Reservation createReservation(Reservation reservationToCreate) {
        if(reservationToCreate.status() != null){
            throw new IllegalArgumentException("Status should by empty");
        }

        if(!reservationToCreate.endDate().isAfter(reservationToCreate.startDate())){
            throw new IllegalArgumentException("start date must be 1 day earlier then and date");
        }

        ReservationEntity entityToSave = new ReservationEntity(
                null,
                reservationToCreate.userId(),
                reservationToCreate.roomId(),
                reservationToCreate.startDate(),
                reservationToCreate.endDate(),
                ReservationStatus.PENDING
        );
        ReservationEntity savedEntity = repository.save(entityToSave);
        return toDomainReservation(savedEntity);
    }

    public Reservation updateReservation(
            Long id,
            Reservation reservationToUpdate
    ) {
        ReservationEntity reservationEntity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Not found reservation by id = " + id));

        if(reservationEntity.getStatus() != ReservationStatus.PENDING){
            throw new IllegalArgumentException("Cannot modify reservation with status = " + reservationEntity.getStatus());
        }
        ReservationEntity updateReservation = new ReservationEntity(
                reservationEntity.getId(),
                reservationToUpdate.userId(),
                reservationToUpdate.roomId(),
                reservationToUpdate.startDate(),
                reservationToUpdate.endDate(),
                ReservationStatus.PENDING
        );
        var reservation = repository.save(updateReservation);
        return toDomainReservation(reservation);
    }

    public void cancelReservation(
            Long id
    ) {
        var reservation = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Not found reservation by id = " + id));
        if(!reservation.getStatus().equals(ReservationStatus.PENDING)){
            throw new IllegalStateException("Cannot cancel approved or already cancelled reservation");
        }
        repository.deleteById(id);
        log.info("Successfully cancelled reservation: id={}", id);
    }

    public Reservation approveReservation(Long id) {
        ReservationEntity reservationEntity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Not found reservation by id = " + id));

        if(reservationEntity.getStatus().equals(ReservationStatus.PENDING)){
            throw new IllegalArgumentException("Can not approve reservation with status = " + reservationEntity.getStatus());
        }

        boolean conflict = isConflictReservation(reservationEntity);

        if(conflict){
            throw new IllegalArgumentException("Can not approve reservation because of conflict");
        }

        reservationEntity.setStatus(ReservationStatus.APPROVED);
        var updateReservation = repository.save(reservationEntity);
        return toDomainReservation(updateReservation);
    }

    private boolean isConflictReservation(
            ReservationEntity reservationEntity
    ){
        return repository.findAll()
                .stream()
                .filter(existing -> !reservationEntity.getId().equals(existing.getId()))
                .filter(existing -> reservationEntity.getRoomId().equals(existing.getRoomId()))
                .filter(existing -> existing.getStatus().equals(ReservationStatus.APPROVED))
                .anyMatch(existing -> reservationEntity.getStartDate().isBefore(existing.getEndDate())
                && existing.getStartDate().isBefore(reservationEntity.getEndDate()));
    }


    private Reservation toDomainReservation(
            ReservationEntity it
    ){
        return new Reservation(
                it.getId(),
                it.getUserId(),
                it.getRoomId(),
                it.getStartDate(),
                it.getEndDate(),
                it.getStatus()
        );
    }
}
