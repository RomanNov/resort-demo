package com.resort.island.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resort.island.model.Availability;
import com.resort.island.model.Reservation;
import com.resort.island.repository.ReservationRepository;
import com.resort.island.service.ReservationService;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReservationServiceTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReservationRepository repository;

    private List<Reservation> reservations;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws JsonProcessingException {
        reservations = objectMapper.readValue("[\n" +
                "    {\n" +
                "        \"email\": \"u1@u1\",\n" +
                "        \"firstName\": \"u1\",\n" +
                "        \"lastName\": \"u1\",\n" +
                "        \"arrivalDate\": \"2020-10-01\",\n" +
                "        \"departureDate\": \"2020-10-02\",\n" +
                "        \"roomNumber\": 1\n" +
                "    },\n" +
                "    {\n" +
                "        \"email\": \"u1@u1\",\n" +
                "        \"firstName\": \"u1\",\n" +
                "        \"lastName\": \"u1\",\n" +
                "        \"arrivalDate\": \"2020-10-01\",\n" +
                "        \"departureDate\": \"2020-10-03\",\n" +
                "        \"roomNumber\": 2\n" +
                "    },\n" +
                "    {\n" +
                "        \"email\": \"u1@u1\",\n" +
                "        \"firstName\": \"u1\",\n" +
                "        \"lastName\": \"u1\",\n" +
                "        \"arrivalDate\": \"2020-10-05\",\n" +
                "        \"departureDate\": \"2020-10-06\",\n" +
                "        \"roomNumber\": 1\n" +
                "    }\n" +
                "]",
                new TypeReference<List<Reservation>>(){});

        repository.deleteAll();
        reservations.forEach(r -> repository.save(r));
    }

    @Test
    void getReservationsInRange() {
        List<Reservation> result = reservationService.getReservationsInRange("2020-10-01", "2020-10-10");
        Assert.assertEquals(reservations.size(), result.size());
        Assert.assertEquals(reservations.get(0).getId(), result.get(0).getId());
        Assert.assertEquals(reservations.get(1).getId(), result.get(1).getId());
        Assert.assertEquals(reservations.get(2).getId(), result.get(2).getId());
    }

    @Test
    void getAvailabilitiesInRange() {
        List<Availability> result = reservationService.getAvailabilitiesInRange("2020-10-01", "2020-10-10");
        Assert.assertEquals(10, result.size());

        Assert.assertEquals("2020-10-01", result.get(0).getDate());
        Assert.assertEquals(0, result.get(0).getNumberAvailableRooms().intValue());

        Assert.assertEquals("2020-10-02", result.get(1).getDate());
        Assert.assertEquals(0, result.get(1).getNumberAvailableRooms().intValue());

        Assert.assertEquals("2020-10-03", result.get(2).getDate());
        Assert.assertEquals(1, result.get(2).getNumberAvailableRooms().intValue());

        Assert.assertEquals("2020-10-04", result.get(3).getDate());
        Assert.assertEquals(2, result.get(3).getNumberAvailableRooms().intValue());

        Assert.assertEquals("2020-10-05", result.get(4).getDate());
        Assert.assertEquals(1, result.get(4).getNumberAvailableRooms().intValue());

        Assert.assertEquals("2020-10-06", result.get(5).getDate());
        Assert.assertEquals(1, result.get(5).getNumberAvailableRooms().intValue());

        Assert.assertEquals("2020-10-07", result.get(6).getDate());
        Assert.assertEquals(2, result.get(6).getNumberAvailableRooms().intValue());

        Assert.assertEquals("2020-10-08", result.get(7).getDate());
        Assert.assertEquals(2, result.get(7).getNumberAvailableRooms().intValue());
    }

    @Test
    void getReservationById() {
        Reservation result = reservationService.getReservationById(reservations.get(0).getId());
        Assert.assertEquals(reservations.get(0).getId(), result.getId());
    }

    @Test
    void createReservation() throws Exception {
        Reservation reservation = new Reservation();
        reservation.setEmail("u2@u2");
        reservation.setFirstName("u2");
        reservation.setLastName("u2");
        reservation.setArrivalDate("2020-10-04");
        reservation.setDepartureDate("2020-10-06");
        String result = reservationService.createReservation(reservation);
        Assert.assertNotNull(result);
    }

    @Test
    void createReservationShouldFailWhenNoRoomsAreAvailable() throws Exception {
        Reservation reservation = new Reservation();
        reservation.setEmail("u2@u2");
        reservation.setFirstName("u2");
        reservation.setLastName("u2");
        reservation.setArrivalDate("2020-10-01");
        reservation.setDepartureDate("2020-10-02");
        Assertions.assertThrows(IllegalStateException.class, () -> reservationService.createReservation(reservation))
                .getMessage().equals("Unfortunately no rooms are available at the moment to make a reservation for the selected dates.");
    }

    @Test
    void updateReservation() {
        Reservation r = reservations.get(0);
        Assert.assertEquals("2020-10-02", r.getDepartureDate());
        r.setDepartureDate("2020-10-03");
        Reservation result = reservationService.updateReservation(r.getId(), r);
        Assert.assertEquals("2020-10-03", result.getDepartureDate());
    }

    @Test
    void cancelReservation() {
        Reservation reservation = reservations.get(0);
        Assert.assertEquals(reservation.getId(), reservationService.getReservationById(reservation.getId()).getId());
        reservationService.deleteReservation(reservation.getId());
        Assertions.assertThrows(NoSuchElementException.class, () -> reservationService.getReservationById(reservation.getId()));
    }
}