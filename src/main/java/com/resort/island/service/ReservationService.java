package com.resort.island.service;

import com.resort.island.model.Availability;
import com.resort.island.model.Reservation;
import com.resort.island.repository.ReservationRepository;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class ReservationService {

    @Value("${total-rooms}")
    private Integer totalRooms;
    private Set<Integer> allRooms = new HashSet<>();
    private ConcurrentHashMap<Integer, Lock> locks;

    @Autowired
    ReservationRepository repository;

    @PostConstruct
    private void init() {
        if(totalRooms >= 1) {
            allRooms = Arrays.stream(IntStream.range(1, totalRooms + 1).toArray()).boxed().collect(Collectors.toSet());
            locks = new ConcurrentHashMap(totalRooms);
            for(Integer i: allRooms) {
                locks.put(i, new ReentrantLock(true));
            }
        }
    }

    public String createReservation(Reservation newReservation) throws Exception {
        validateReservation(newReservation);
        Set<Integer> availableRooms = validateAvailabilityAndGetAvailableRooms(newReservation, false, false);
        Optional<Reservation> result = obtainLockAndExecuteReservationAction(availableRooms, newReservation, false, (reservation) -> repository.save(reservation));
        if (result.isPresent()) {
            return result.get().getId();
        }
        throw new Exception("Unfortunately we were not able to create the reservation");
    }

    public Reservation updateReservation(String id, Reservation newReservation) {
        validateReservation(newReservation);
        Optional<Reservation> oldReservation = repository.findById(id);
        if (!oldReservation.isPresent()) {
            throw new NoSuchElementException(String.format("Reservation with id %s does NOT exist.", newReservation.getId()));
        }
        return oldReservation.map( r -> {
            boolean overlapsOldReservation = isOverlapsOldReservation(newReservation, r);
            boolean isWithinPreviousDates = isWithinOldReservationDates(newReservation, r);
            r.setEmail(newReservation.getEmail());
            r.setFirstName(newReservation.getFirstName());
            r.setLastName(newReservation.getLastName());
            r.setArrivalDate(newReservation.getArrivalDate());
            r.setDepartureDate(newReservation.getDepartureDate());
            if (isWithinPreviousDates) {
                return repository.save(r);
            }
            Set<Integer> availableRooms = validateAvailabilityAndGetAvailableRooms(r, true, overlapsOldReservation);
            System.out.println(availableRooms);
            Optional<Reservation> result = obtainLockAndExecuteReservationAction(availableRooms, r, true, (reservation) -> repository.save(reservation));
            return result.get();
        }).get();
    }

    private boolean isWithinOldReservationDates(Reservation newReservation, Reservation r) {
        return newReservation.getArrival().isEqual(r.getArrival()) && newReservation.getDeparture().isBefore(r.getDeparture())
                || newReservation.getDeparture().isEqual(r.getDeparture()) && newReservation.getArrival().isAfter(r.getArrival())
                || newReservation.getArrival().isEqual(newReservation.getDeparture()) && newReservation.getArrival().isAfter(r.getArrival()) && newReservation.getDeparture().isBefore(r.getDeparture());
    }

    private boolean isOverlapsOldReservation(Reservation newReservation, Reservation r) {
        return !(newReservation.getArrival().isBefore(r.getArrival()) &&
                newReservation.getArrival().isAfter(r.getDeparture())) || !(newReservation.getDeparture().isBefore(r.getArrival()) &&
                newReservation.getDeparture().isAfter(r.getDeparture()));
    }

    public void deleteReservation(String id) {
        repository.deleteById(id);
    }

    public List<Reservation> getReservationsInRange(String startDate, String endDate) {
        return repository.findAllByArrivalDateBetween(LocalDate.parse(startDate).minusDays(1).toString(),endDate);
    }

    private void validateReservation(Reservation r) {
        if (r.getDeparture().isBefore(r.getArrival())) {
            throw new InvalidPropertyException(Reservation.class, "departureDate", "Departure date must be same day or later than arrival date");
        }
        if (!r.getArrival().plusDays(3).isAfter(r.getDeparture())) {
            throw new InvalidPropertyException(Reservation.class, "departureDate", "The maximum allowed stay is of 3 days.");
        }
    }

    private Set<Integer> validateAvailabilityAndGetAvailableRooms(Reservation reservation, boolean isUpdate, boolean overlapsOldReservation) {
        LocalDate startDate = reservation.getArrival().minusDays(2);
        LocalDate endDate = reservation.getDeparture();
        List<Reservation> overlappingReservations = getReservationsInRange(startDate.toString(), endDate.toString() )
                .stream()
                .filter(r -> r.getDeparture().isAfter(reservation.getArrival().minusDays(1)))
                .collect(Collectors.toList());
        Set<Integer> availableRooms = new HashSet<>(allRooms);
        if (overlappingReservations.isEmpty()) {
            return availableRooms;
        }

        if (!isUpdate || !overlapsOldReservation) {
            if (overlappingReservations.size() == totalRooms) {
                throw new IllegalStateException("Unfortunately no rooms are available at the moment to make a reservation for the selected dates.");
            }
        } else {
            if (overlappingReservations.size() == totalRooms + 1) {
                throw new IllegalStateException("Unfortunately no rooms are available at the moment to change the reservation for the selected dates.");
            }
           Reservation self = overlappingReservations.stream().filter(r -> r.getId().equals(reservation.getId())).findFirst().get();
           overlappingReservations.remove(self);
        }

        Set<Integer> reservedRooms = overlappingReservations.stream().map(Reservation::getRoomNumber).collect(Collectors.toSet());
        availableRooms.removeAll(reservedRooms);
        return availableRooms;
    }

    private boolean validateRoomAvailability(LocalDate arrivalDate, LocalDate departureDate, Integer room, final String reservationIdToSkip) {
        LocalDate startDate = arrivalDate.minusDays(2);
        LocalDate endDate = departureDate;
        List<Reservation> overlappingReservations = getReservationsInRange(startDate.toString(), endDate.toString() )
                .stream()
                .filter(r -> r.getDeparture().isAfter(arrivalDate.minusDays(1)))
                .filter(r -> reservationIdToSkip != null && !r.getId().equals(reservationIdToSkip))
                .collect(Collectors.toList());
        if (overlappingReservations.isEmpty()) {
            return true;
        }
        return !overlappingReservations.stream().map(r -> r.getRoomNumber()).collect(Collectors.toSet()).contains(room);
    }

    private Optional<Reservation> obtainLockAndExecuteReservationAction (Set<Integer> availableRooms, Reservation reservation, boolean isUpdate, Function<Reservation,Reservation> action) {
        final String reservationIdToSkip = isUpdate ? reservation.getId() : null;
        for(Integer roomNumber: availableRooms) {
            Lock lock = locks.get(roomNumber);
            if (lock.tryLock()) {
                try {
                    if (validateRoomAvailability(reservation.getArrival(), reservation.getDeparture(), roomNumber, reservationIdToSkip)) {
                        reservation.setRoomNumber(roomNumber);
                        return Optional.ofNullable(action.apply(reservation));
                    }
                } finally {
                    lock.unlock();
                }
            }
        }
        return Optional.empty();
    }

    public Reservation getReservationById(String id) {
        Optional<Reservation> r = repository.findById(id);
        if (r.isPresent()) {
            return r.get();
        }
        throw new NoSuchElementException(String.format("Reservation with id %s does NOT exist.", id));

    }

    public List<Availability> getAvailabilitiesInRange(String startDate, String endDate) {
        List<Availability> result = new ArrayList<>();
        endDate = endDate == null || endDate.compareTo(startDate) < 0 ? LocalDate.parse(startDate).plusDays(31).toString() : LocalDate.parse(endDate).plusDays(1).toString();
        startDate = startDate == null ? LocalDate.now().minusDays(2).toString() : LocalDate.parse(startDate).minusDays(2).toString();
        List<Reservation> currentReservations = getReservationsInRange(startDate, endDate);
        LocalDate s = LocalDate.parse(startDate).plusDays(2);
        LocalDate e = LocalDate.parse(endDate).minusDays(1);
        do {
            String d = s.toString();
            LocalDate dDate = LocalDate.parse(d);
            List<Reservation> overlappingReservations = currentReservations.stream()
                    .filter(r-> dDate.isEqual(r.getDeparture()) || dDate.isEqual(r.getArrival()) || dDate.isAfter(r.getArrival()) && dDate.isBefore(r.getDeparture()))
                    .collect(Collectors.toList());
            Set<Integer> availableRooms = new HashSet<>(allRooms);
            Set<Integer> reservedRooms = overlappingReservations.stream().map(Reservation::getRoomNumber).collect(Collectors.toSet());
            availableRooms.removeAll(reservedRooms);
            result.add(new Availability(d, availableRooms.size()));
            s = s.plusDays(1);
        } while (!s.isAfter(e));

        return result;
    }
}
