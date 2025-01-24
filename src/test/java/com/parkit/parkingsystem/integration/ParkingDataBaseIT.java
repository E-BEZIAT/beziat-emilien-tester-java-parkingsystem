package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
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
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    private static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    private void setUpPerTest() throws Exception {
        Mockito.lenient().when(inputReaderUtil.readSelection()).thenReturn(1);
        Mockito.lenient().when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    private static void tearDown(){

    }

    @Test
    public void testParkingACar(){

        //GIVEN

        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO); // Initialize parking service
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false); // Create car parking spot
        Ticket ticket = new Ticket(); // Create ticket with vehicle details
        ticket.setVehicleRegNumber("ABCDEF");
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR,false));
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000))); // Set parking entry time to 1 hour ago
        ticket.setOutTime(null); // Initialize ticket status
        ticket.setPrice(0);

        //WHEN

        parkingService.processIncomingVehicle(); // Process incoming vehicle in parking service
        boolean isSaved = ticketDAO.saveTicket(ticket); // Save ticket to database
        Ticket retrievedTicket = ticketDAO.getTicket(ticket.getVehicleRegNumber()); // Retrieve ticket using vehicle registration number

        //THEN

        assertTrue(isSaved); // Verify ticket was successfully saved
        assertNotNull(parkingSpot); // Check objects are not null
        assertNotNull(retrievedTicket);
        assertNotNull(ticket);
        assertEquals(null, retrievedTicket.getOutTime()); // Validate ticket properties
        assertEquals(0, retrievedTicket.getPrice());
        assertEquals("ABCDEF", retrievedTicket.getVehicleRegNumber());
        assertEquals(ParkingType.CAR, retrievedTicket.getParkingSpot().getParkingType());
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
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO); // Initialize parking service and save ticket
        boolean isSaved = ticketDAO.saveTicket(ticket);

        //WHEN

        parkingService.processExitingVehicle(); // Process vehicle exit
        ticketDAO.saveTicket(ticket); // Update ticket in database
        Ticket retrievedTicket = ticketDAO.getTicket(ticket.getVehicleRegNumber()); // Retrieve updated ticket

        //THEN

        assertTrue(isSaved); // Verify ticket saved and ticket details
        assertNotNull(retrievedTicket);
        assertNotNull(ticket);
        assertTrue(retrievedTicket.getPrice() > 0); // Check ticket pricing and timestamp
        assertNotNull(retrievedTicket.getOutTime());
        assertEquals(Fare.CAR_RATE_PER_HOUR, retrievedTicket.getPrice(), 0.01);
        assertEquals(1, parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)); // Validate parking spot and registration
        assertEquals("ABCDEF", retrievedTicket.getVehicleRegNumber());
    }


    @Test
    public void testParkingLotExitRecurringUser() throws Exception{

        //GIVEN

        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO); // Initialize parking service
        parkingService.processIncomingVehicle();
        Ticket ticket = new Ticket(); // Create and configure ticket
        ticket.setVehicleRegNumber("ABCDEF");
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR,false));
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
        ticket.setOutTime(new Date());
        ticket.setPrice(0);
        ticket.setRegularCustomer(true);
        ticketDAO.saveTicket(ticket); // Save ticket

        //WHEN

        double expectedFare = Math.round(Fare.CAR_RATE_PER_HOUR * 0.95); // Calculate discounted fare for regular customers
        boolean isRegularCustomer = ticketDAO.getNbTicket(ticket.getVehicleRegNumber()) >= 1;
        if (isRegularCustomer) {
            ticket.setPrice(expectedFare);
        }
        parkingService.processExitingVehicle(); // Process vehicle exit and save ticket
        boolean isSaved = ticketDAO.saveTicket(ticket);
        Ticket retrievedTicket = ticketDAO.getTicket(ticket.getVehicleRegNumber());

        //THEN

        assertTrue(isRegularCustomer); // Verify ticket details and regular customer status
        assertNotNull(retrievedTicket);
        assertTrue(isSaved, "le ticket n'est pas sauvegardé correctement");
        assertTrue(ticketDAO.getNbTicket(ticket.getVehicleRegNumber()) >= 1);
    }
}
