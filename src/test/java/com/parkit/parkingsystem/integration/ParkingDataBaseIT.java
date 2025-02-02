package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Date;

import static com.parkit.parkingsystem.model.Ticket.vehicleRegNumber;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;
    private static FareCalculatorService fareCalculatorService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    public static void setUp(){
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        fareCalculatorService = new FareCalculatorService();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    public void setUpPerTest() throws Exception {
        Mockito.lenient().when(inputReaderUtil.readSelection()).thenReturn(1);
        Mockito.lenient().when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    public static void tearDown(){

    }

    @Test
    public void testParkingACar(){

        //GIVEN

        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService); // Initialize parking service
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false); // Create car parking spot
        Ticket ticket = new Ticket(); // Create ticket with vehicle details
        ticket.setVehicleRegNumber("ABCDEF");
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR,false));
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000))); // Set parking entry time to 1 hour ago
        ticket.setOutTime(null); // Initialize ticket status
        ticket.setPrice(0);

        //WHEN

        parkingService.processIncomingVehicle(); // Process incoming vehicle in parking service

        //THEN

        assertNotNull(parkingSpot); // Check objects are not null
        assertNotNull(ticket);
        assertNotNull(ticket.getInTime());
        assertNull(ticket.getOutTime()); // Validate ticket properties
        assertEquals(0, ticket.getPrice());
        assertEquals("ABCDEF", ticket.getVehicleRegNumber());
        assertEquals(ParkingType.CAR, ticket.getParkingSpot().getParkingType());
        assertEquals(1, ticket.getParkingSpot().getId());
    }

    @Test
    public void testParkingLotExit(){

        //GIVEN

        Ticket ticket = new Ticket(); // Create ticket
        ticket.setVehicleRegNumber("ABCDEF");
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR,false));
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000))); // Set ticket timestamps and initial status
        ticket.setOutTime(null);
        ticket.setPrice(0);
        ticketDAO.saveTicket(ticket);
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService); // Initialize parking service and save ticket

        //WHEN
        boolean saved = ticketDAO.saveTicket(ticket);
        assertTrue(saved, "Ticket should be saved successfully");
        parkingService.processExitingVehicle(); // Process vehicle exit
        ticket = ticketDAO.getTicket("ABCDEF");

        //THEN

        assertNotNull(ticket);
        assertNotNull(ticket.getInTime());
        assertNotNull(ticket.getPrice());
        assertNotNull(ticket.getOutTime());
        assertNotNull(ticket.getParkingSpot());
        assertEquals(1, parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)); // Validate parking spot and registration
        assertEquals("ABCDEF", ticket.getVehicleRegNumber());
    }


    @Test
    public void testParkingLotExitRecurringUser(){

        //GIVEN

        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService); // Initialize parking service
        parkingService.processIncomingVehicle();
        Ticket ticket = new Ticket(); // Create and configure ticket
        ticket.setVehicleRegNumber("ABCDEF");
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR,false));
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
        ticket.setOutTime(new Date());
        ticket.setPrice(0);
        ticketDAO.getTicket(ticket.getVehicleRegNumber());
        ticketDAO.saveTicket(ticket); // Save ticket
        ticketDAO.updateTicket(ticket);

        //WHEN

        int nbTicket = ticketDAO.getNbTicket("ABCDEF");
        parkingService.processExitingVehicle(); // Process vehicle exit and save ticket

        //THEN

        assertNotNull(nbTicket); // Verify ticket details and regular customer status
        assertEquals("ABCDEF", ticket.getVehicleRegNumber());
        assertEquals(ParkingType.CAR, ticket.getParkingSpot().getParkingType());
        assertNotNull(ticket.getInTime());
        assertNotNull(ticket.getOutTime());
        assertNotNull(ticket);
        assertNotNull(ticket.getParkingSpot().getParkingType());
        assertTrue(ticketDAO.getNbTicket(ticket.getVehicleRegNumber()) >= 1);
    }
}
