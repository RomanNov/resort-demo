package com.resort.island.repository;

import com.resort.island.model.Reservation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ReservationRepository extends MongoRepository<Reservation, String> {
    List<Reservation> findAllByArrivalDateBetween(String startDate, String endDate);
}
