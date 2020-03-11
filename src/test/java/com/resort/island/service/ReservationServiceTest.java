package com.resort.island.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resort.island.model.Availability;
import com.resort.island.model.Reservation;
import com.resort.island.repository.ReservationRepository;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.util.*;

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
                "        \"arrivalDate\": \"2020-04-01\",\n" +
                "        \"departureDate\": \"2020-04-02\",\n" +
                "        \"roomNumber\": 1\n" +
                "    },\n" +
                "    {\n" +
                "        \"email\": \"u1@u1\",\n" +
                "        \"firstName\": \"u1\",\n" +
                "        \"lastName\": \"u1\",\n" +
                "        \"arrivalDate\": \"2020-04-01\",\n" +
                "        \"departureDate\": \"2020-04-03\",\n" +
                "        \"roomNumber\": 2\n" +
                "    },\n" +
                "    {\n" +
                "        \"email\": \"u1@u1\",\n" +
                "        \"firstName\": \"u1\",\n" +
                "        \"lastName\": \"u1\",\n" +
                "        \"arrivalDate\": \"2020-04-05\",\n" +
                "        \"departureDate\": \"2020-04-06\",\n" +
                "        \"roomNumber\": 1\n" +
                "    }\n" +
                "]",
                new TypeReference<List<Reservation>>(){});

        repository.deleteAll();
        reservations.forEach(r -> repository.save(r));
    }

    @Test
    void getReservationsInRange() {
        List<Reservation> result = reservationService.getReservationsInRange("2020-04-01", "2020-04-10");
        Assert.assertEquals(reservations.size(), result.size());
        Assert.assertEquals(reservations.get(0).getId(), result.get(0).getId());
        Assert.assertEquals(reservations.get(1).getId(), result.get(1).getId());
        Assert.assertEquals(reservations.get(2).getId(), result.get(2).getId());
    }

    @Test
    void getAvailabilitiesInRange() {
        List<Availability> result = reservationService.getAvailabilitiesInRange("2020-04-01", "2020-04-10");
        Assert.assertEquals(10, result.size());

        Assert.assertEquals("2020-04-01", result.get(0).getDate());
        Assert.assertEquals(0, result.get(0).getNumberAvailableRooms().intValue());

        Assert.assertEquals("2020-04-02", result.get(1).getDate());
        Assert.assertEquals(0, result.get(1).getNumberAvailableRooms().intValue());

        Assert.assertEquals("2020-04-03", result.get(2).getDate());
        Assert.assertEquals(1, result.get(2).getNumberAvailableRooms().intValue());

        Assert.assertEquals("2020-04-04", result.get(3).getDate());
        Assert.assertEquals(2, result.get(3).getNumberAvailableRooms().intValue());

        Assert.assertEquals("2020-04-05", result.get(4).getDate());
        Assert.assertEquals(1, result.get(4).getNumberAvailableRooms().intValue());

        Assert.assertEquals("2020-04-06", result.get(5).getDate());
        Assert.assertEquals(1, result.get(5).getNumberAvailableRooms().intValue());

        Assert.assertEquals("2020-04-07", result.get(6).getDate());
        Assert.assertEquals(2, result.get(6).getNumberAvailableRooms().intValue());

        Assert.assertEquals("2020-04-08", result.get(7).getDate());
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
        reservation.setArrivalDate(LocalDate.now().plusDays(1).toString());
        reservation.setDepartureDate(LocalDate.now().plusDays(2).toString());
        String result = reservationService.createReservation(reservation);
        Assert.assertNotNull(result);
    }

    @Test
    void createConcurrentReservations() throws Exception {
        repository.deleteAll();
        int N = 10;
        String[] result = new String[N];
        Thread[] thread = new Thread[N];
        for(int i = 0; i < N; i++) {
            int j = i;
            thread[j] = new Thread(() -> {
                try {
                    Reservation reservation = new Reservation();
                    reservation.setEmail("u2@u2");
                    reservation.setFirstName("u2");
                    reservation.setLastName("u2");
                    reservation.setArrivalDate(LocalDate.now().plusDays(1).toString());
                    reservation.setDepartureDate(LocalDate.now().plusDays(2).toString());
                    result[j] = reservationService.createReservation(reservation);
                } catch (Exception e) {
                    result[j] = e.getMessage();
                }
                });
            thread[j].start();
        }
        for(int i = N-1; i >= 0; i--) {
            thread[i].join();
        }
        List<String> resultList = new ArrayList<>(Arrays.asList(result));
        resultList.removeIf(s -> s.equals("Unfortunately we were not able to create the reservation."));
        resultList.removeIf(s -> s.equals("Unfortunately no rooms are available at the moment to make a reservation for the selected dates."));
        Assert.assertEquals(2 ,resultList.size());
        Assert.assertTrue(repository.findById(resultList.get(0)).get().getId() != repository.findById(resultList.get(1)).get().getId());
        Assert.assertTrue(repository.findById(resultList.get(0)).get().getRoomNumber() != repository.findById(resultList.get(1)).get().getRoomNumber());
    }

    @Test
    void createReservationShouldFailWhenNoRoomsAreAvailable() throws Exception {
        Reservation reservation = new Reservation();
        reservation.setEmail("u2@u2");
        reservation.setFirstName("u2");
        reservation.setLastName("u2");
        reservation.setArrivalDate("2020-04-01");
        reservation.setDepartureDate("2020-04-02");
        Assertions.assertThrows(IllegalStateException.class, () -> reservationService.createReservation(reservation))
                .getMessage().equals("Unfortunately no rooms are available at the moment to make a reservation for the selected dates.");
    }

    @Test
    void createReservationShouldFailWhenReserveMoreThan30DaysInAdvance() throws Exception {
        Reservation reservation = new Reservation();
        reservation.setEmail("u2@u2");
        reservation.setFirstName("u2");
        reservation.setLastName("u2");
        reservation.setArrivalDate("2020-05-01");
        reservation.setDepartureDate("2020-05-02");
        Assertions.assertThrows(InvalidPropertyException.class, () -> reservationService.createReservation(reservation))
                .getMessage().equals("A reservation can be made at most 30 days in advance.");
    }

    @Test
    void createReservationShouldFailWhenReserveForToday() throws Exception {
        Reservation reservation = new Reservation();
        reservation.setEmail("u2@u2");
        reservation.setFirstName("u2");
        reservation.setLastName("u2");
        reservation.setArrivalDate(LocalDate.now().toString());
        reservation.setDepartureDate(LocalDate.now().plusDays(1).toString());
        Assertions.assertThrows(InvalidPropertyException.class, () -> reservationService.createReservation(reservation))
                .getMessage().equals("A reservation should be made at least 1 day in advance.");
    }

    @Test
    void updateReservation() {
        Reservation r = reservations.get(0);
        Assert.assertEquals("2020-04-02", r.getDepartureDate());
        r.setDepartureDate("2020-04-03");
        Reservation result = reservationService.updateReservation(r.getId(), r);
        Assert.assertEquals("2020-04-03", result.getDepartureDate());
    }

    @Test
    void cancelReservation() {
        Reservation reservation = reservations.get(0);
        Assert.assertEquals(reservation.getId(), reservationService.getReservationById(reservation.getId()).getId());
        reservationService.deleteReservation(reservation.getId());
        Assertions.assertThrows(NoSuchElementException.class, () -> reservationService.getReservationById(reservation.getId()));
    }
}