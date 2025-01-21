package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

import static com.mysql.cj.conf.PropertyKey.logger;

public class FareCalculatorService {

    public double calculateFare(Ticket ticket) {
        if ((ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime()))) {
            throw new IllegalArgumentException("Out time provided is incorrect:" + ticket.getOutTime().toString());
        }
        double CAR_RATE_PER_MINUTES = Fare.CAR_RATE_PER_HOUR / 60;
        double BIKE_RATE_PER_MINUTES = Fare.BIKE_RATE_PER_HOUR / 60;
        long inTime = ticket.getInTime().getTime();
        System.out.println("inTime = " + inTime);
        long outTime = ticket.getOutTime().getTime();
        System.out.println("outTime = " + outTime);

        //TODO: Some tests are failing here. Need to check if this logic is correct
        double duration = (double) (outTime - inTime) / (1000 * 60); //duration in minute
        System.out.println("duration = " + duration);
        boolean discount = ticket.isRegularCustomer();
        System.out.println("Parking Type: " + ticket.getParkingSpot().getParkingType());
        switch (ticket.getParkingSpot().getParkingType()) {
            /** logger.info("Create DB connection");
             try {
             Class.forName("com.mysql.cj.jdbc.Driver");
             return DriverManager.getConnection(
             "jdbc:mysql://localhost:3306/prod?useSSL=false&serverTimezone=Europe/Paris&characterEncoding=UTF-8","root","rootroot");
             } catch (ClassNotFoundException | SQLException e) {
             throw new RuntimeException(e);
             }
             **/

            case CAR: {
                if (duration <= 30) {
                    ticket.setPrice(0);
                }
                else {
                    if (ticket.isRegularCustomer()) {
                        ticket.setPrice((duration - 30) * (CAR_RATE_PER_MINUTES * 0.95));
                    } else {
                        ticket.setPrice((duration - 30) * CAR_RATE_PER_MINUTES);
                    }
                }
                break;

            }
            case BIKE: {
                if (duration <= 30) {
                    ticket.setPrice(0);
                }
                else {
                    if (ticket.isRegularCustomer()) {
                        ticket.setPrice((duration - 30) * (BIKE_RATE_PER_MINUTES * 0.95));
                    } else {
                        ticket.setPrice((duration - 30) * BIKE_RATE_PER_MINUTES);
                    }
                }
                    break;


            }
            default:
                throw new IllegalArgumentException("Unkown Parking Type: " + ticket.getParkingSpot().getParkingType());
        }

        return CAR_RATE_PER_MINUTES;
    }
}