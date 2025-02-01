package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {
    /**
     * Calculate the price with the duration time
     * @param ticket
     * @return
     */

    public double calculateFare(Ticket ticket) {
        if ((ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime()))) {
            throw new IllegalArgumentException("Out time provided is incorrect:" + ticket.getOutTime().toString());
        }
        double CAR_RATE_PER_MINUTES = Fare.CAR_RATE_PER_HOUR / 60;
        double BIKE_RATE_PER_MINUTES = Fare.BIKE_RATE_PER_HOUR / 60;
        long inTime = ticket.getInTime().getTime();
        long outTime = ticket.getOutTime().getTime();

        double duration = (double) (outTime - inTime) / (1000 * 60); //duration in minute
        System.out.println("duration = " + duration);
        boolean discount = ticket.isRegularCustomer();
        System.out.println("Parking Type: " + ticket.getParkingSpot().getParkingType());

                if (duration <= 30) {
                    ticket.setPrice(0);
                    return 0.0;
                }
                else {
                    switch (ticket.getParkingSpot().getParkingType()) {
                        case CAR: {
                            if (ticket.isRegularCustomer()) {
                                ticket.setPrice(duration * CAR_RATE_PER_MINUTES * 0.95);
                            } else {
                                ticket.setPrice(duration * CAR_RATE_PER_MINUTES);
                            }
                            break;
                        }
                    case BIKE: {
                        if (ticket.isRegularCustomer()) {
                            ticket.setPrice((duration) * (BIKE_RATE_PER_MINUTES * 0.95));
                        } else {
                            ticket.setPrice((duration) * BIKE_RATE_PER_MINUTES);
                        }
                        break;
                    }
                        default:
                            throw new IllegalArgumentException("Unkown Parking Type: " + ticket.getParkingSpot().getParkingType());
                     }
                }
        return CAR_RATE_PER_MINUTES;
    }
}