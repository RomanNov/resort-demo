package com.resort.island.controller;

import com.resort.island.model.Availability;
import com.resort.island.model.Reservation;
import com.resort.island.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;


    @GetMapping("/reservations")
    public List<Reservation> getReservationsInRange(@RequestParam (required = false)  String startDate, @RequestParam (required = false) String endDate) {
        return reservationService.getReservationsInRange(startDate, endDate);
    }

    @GetMapping("/availabilities")
    public List<Availability> getAvailabilitiesInRange(@RequestParam (required = false)  String startDate, @RequestParam (required = false) String endDate) {
        return reservationService.getAvailabilitiesInRange(startDate, endDate);
    }

    @GetMapping("/reservations/{id}")
    public Reservation getReservation(@PathVariable String id) throws Exception {
        return reservationService.getReservationById(id);
    }

    @PostMapping("/reservations")
    public String createReservation(@RequestBody Reservation reservation) throws Exception {
       return reservationService.createReservation(reservation);
    }

    @PutMapping("/reservations/{id}")
    public String updateReservation(@RequestBody Reservation reservation, @PathVariable String id) {
        reservationService.updateReservation(id, reservation);
        return id;
    }

    @DeleteMapping("reservations/{id}")
    public void cancelReservation(@PathVariable String id) {
        reservationService.deleteReservation(id);
    }
}
