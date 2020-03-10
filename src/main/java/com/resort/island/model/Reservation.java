package com.resort.island.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Document(collection ="reservations")
public class Reservation {
    @Id
    private String id;
    @NonNull
    private String email;
    @NonNull
    private String firstName;
    @NonNull
    private String lastName;
    @NonNull
    private String arrivalDate;
    @NonNull
    private String departureDate;
    private Integer roomNumber;
    @JsonIgnore
    private transient LocalDate arrival;
    @JsonIgnore
    private transient LocalDate departure;

    public LocalDate getArrival() {
        return LocalDate.parse(arrivalDate);
    }
    public LocalDate getDeparture() {
        return LocalDate.parse(departureDate);
    }
}
