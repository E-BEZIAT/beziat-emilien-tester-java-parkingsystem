package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.sql.SQLException;
import java.util.Date;

public class ParkingService {

    private static final Logger logger = LogManager.getLogger("ParkingService");

    private static FareCalculatorService fareCalculatorService = new FareCalculatorService();

    private InputReaderUtil inputReaderUtil;
    private ParkingSpotDAO parkingSpotDAO;
    private  TicketDAO ticketDAO;

    public ParkingService(InputReaderUtil inputReaderUtil, ParkingSpotDAO parkingSpotDAO, TicketDAO ticketDAO){
        this.inputReaderUtil = inputReaderUtil;
        this.parkingSpotDAO = parkingSpotDAO;
        this.ticketDAO = ticketDAO;
    }

    public void processIncomingVehicle() {
        try {
            ParkingSpot parkingSpot = getNextParkingNumberIfAvailable();
            if(parkingSpot != null && parkingSpot.getId() > 0) {
                String vehicleRegNumber = getVehichleRegNumber();
                int nbTicket = ticketDAO.getNbTicket(vehicleRegNumber);
                boolean isRegularCustomer = nbTicket >= 1;
                if (isRegularCustomer) {
                    System.out.println("Heureux de vous revoir ! En tant qu’utilisateur régulier de notre parking, vous allez obtenir une remise de 5%");
                }
                parkingSpot.setAvailable(false);
                if(!parkingSpotDAO.updateParking(parkingSpot)) {
                    throw new Exception("erreur lors de l'update du parkingspot");
                }

                Date inTime = new Date();
                Ticket ticket = new Ticket();
                ticket.setParkingSpot(parkingSpot);
                ticket.setVehicleRegNumber(vehicleRegNumber);
                ticket.setPrice(0);
                ticket.setInTime(inTime);
                ticket.setOutTime(null);
                ticket.setRegularCustomer(isRegularCustomer);
                System.out.println("Generated Ticket and saved in DB");
                System.out.println("Please park your vehicle in spot number:"+parkingSpot.getId());
                System.out.println("Recorded in-time for vehicle number:"+vehicleRegNumber+" is:"+inTime);

                if(!ticketDAO.saveTicket(ticket)) {
                    throw new Exception("erreur lors de la sauvegarde du ticket");
                }

                else {
                    System.out.println("not available parking spot");
                }
            }

        }catch(Exception e){
            logger.error("Unable to process incoming vehicle",e);
        }
    }

    public String getVehichleRegNumber() throws Exception {
        System.out.println("Please type the vehicle registration number and press enter key");
        return inputReaderUtil.readVehicleRegistrationNumber();
    }

    public ParkingSpot getNextParkingNumberIfAvailable() throws Exception {
        int parkingNumber = 0;
        ParkingSpot parkingSpot = null;
        ParkingType parkingType = getVehichleType();
        try{
            parkingNumber = parkingSpotDAO.getNextAvailableSlot(parkingType);
            if(parkingNumber > 0){
                parkingSpot = new ParkingSpot(parkingNumber,parkingType, true);
            }else{
                throw new Exception("Error fetching parking number from DB. Parking slots might be full");
            }
        }catch(IllegalArgumentException ie){
            logger.error("Error parsing user input for type of vehicle", ie);
            throw ie;
        }catch(Exception e){
            logger.error("Error fetching next available parking slot", e);
            throw e;
        }
        return parkingSpot;
    }

    public ParkingType getVehichleType(){
        System.out.println("Please select vehicle type from menu");
        System.out.println("1 CAR");
        System.out.println("2 BIKE");
        int input = inputReaderUtil.readSelection();
        switch(input){
            case 1: {
                return ParkingType.CAR;
            }
            case 2: {
                return ParkingType.BIKE;
            }
            default: {
                System.out.println("Incorrect input provided");
                throw new IllegalArgumentException("Entered input is invalid");
            }
        }
    }

    public void processExitingVehicle() {
        try{
            String vehicleRegNumber = getVehichleRegNumber();
            System.out.println("Exiting vehicle with register number "+vehicleRegNumber);
            Ticket ticket = ticketDAO.getTicket(vehicleRegNumber);
            if (ticket == null) {
                System.out.println("No ticket found for vehicle number: " + vehicleRegNumber);
                return;
            }
            Date outTime = new Date();
            ticket.setOutTime(outTime);
            long durationInMillis = ticket.getOutTime().getTime() - ticket.getInTime().getTime();
            double durationInMinutes = durationInMillis / (1000.0 * 60.0);
            double calculatedFare = durationInMinutes * fareCalculatorService.calculateFare(ticket);
            int nbTickets = ticketDAO.getNbTicket(vehicleRegNumber);
            boolean isRecurringUser = nbTickets > 1;
            if(isRecurringUser){
                calculatedFare *= 0.95;
            }
            ticket.setPrice(calculatedFare);
            if(ticketDAO.updateTicket(ticket)) {
                ParkingSpot parkingSpot = ticket.getParkingSpot();
                parkingSpot.setAvailable(true);
                parkingSpotDAO.updateParking(parkingSpot);
                System.out.println("Thank you for your trust, please pay the parking fare:" + ticket.getPrice());
            }else{
                System.out.println("Unable to update ticket information. Error occurred");
            }
        }catch (SQLException e) {
            logger.error("Database error while processing exiting vehicle", e);
        }
        catch(Exception e){
            logger.error("Unable to process exiting vehicle",e);
        }
    }
}
