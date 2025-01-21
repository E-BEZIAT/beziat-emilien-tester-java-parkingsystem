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

import static com.mysql.cj.conf.PropertyKey.logger;
import static org.apache.commons.lang.BooleanUtils.isFalse;
import static org.apache.commons.lang.BooleanUtils.isTrue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

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
        Mockito.lenient().when(inputReaderUtil.readSelection()).thenReturn(1); //2 solutions, ajouter lenient avant when ou lancer mokito avec la commande @RunWith(MockitoJUnitRunner.Silent.class)
        Mockito.lenient().when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    private static void tearDown(){

    }

    @Test
    public void testParkingACar(){
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);
        Ticket ticket = new Ticket();
        ticket.setVehicleRegNumber("ABCDEF");
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR,false));
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
        ticket.setOutTime(null);
        ticket.setPrice(0);

        parkingService.processIncomingVehicle();
        boolean isSaved = ticketDAO.saveTicket(ticket);
        Ticket retrievedTicket = ticketDAO.getTicket(ticket.getVehicleRegNumber());

        assertTrue(isSaved);
        assertNotNull(parkingSpot);
        assertNotNull(retrievedTicket);
        assertNotNull(ticket);
        assertEquals(null, retrievedTicket.getOutTime());
        assertEquals(0, retrievedTicket.getPrice());
        assertEquals("ABCDEF", retrievedTicket.getVehicleRegNumber());
        assertEquals(ParkingType.CAR, retrievedTicket.getParkingSpot().getParkingType());

        //TODO: check that a ticket is actualy saved in DB and Parking table is updated with availability
    }

    @Test
    public void testParkingLotExit(){
        Ticket ticket = new Ticket();
        ticket.setVehicleRegNumber("ABCDEF");
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR,false));
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
        ticket.setOutTime(null);
        ticket.setPrice(0);
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        boolean isSaved = ticketDAO.saveTicket(ticket);

        parkingService.processExitingVehicle();
        ticketDAO.saveTicket(ticket);
        Ticket retrievedTicket = ticketDAO.getTicket(ticket.getVehicleRegNumber());

        assertTrue(isSaved);
        assertNotNull(retrievedTicket);
        assertNotNull(ticket);
        assertTrue(retrievedTicket.getPrice() > 0);
        assertNotNull(retrievedTicket.getOutTime());
        assertEquals(Fare.CAR_RATE_PER_HOUR, retrievedTicket.getPrice(), 0.01);
        assertEquals(1, parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR));
        assertEquals("ABCDEF", retrievedTicket.getVehicleRegNumber());



        //TODO: check that the fare generated and out time are populated correctly in the database
    }


    @Test
    public void testParkingLotExitRecurringUser() throws Exception{

        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        parkingService.processIncomingVehicle();

        Ticket ticket = new Ticket();
        ticket.setVehicleRegNumber("ABCDEF");
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR,false));
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
        ticket.setOutTime(new Date());
        ticket.setPrice(0);
        ticket.setRegularCustomer(true);
        ticketDAO.saveTicket(ticket);

        double expectedFare = Math.round(Fare.CAR_RATE_PER_HOUR * 0.95);
        boolean isRegularCustomer = ticketDAO.getNbTicket(ticket.getVehicleRegNumber()) >= 1;
        if (isRegularCustomer) {
            ticket.setPrice(expectedFare);
        }
        parkingService.processExitingVehicle();
        boolean isSaved = ticketDAO.saveTicket(ticket);
        Ticket retrievedTicket = ticketDAO.getTicket(ticket.getVehicleRegNumber());

        assertTrue(isRegularCustomer);
        assertNotNull(retrievedTicket);
        assertTrue(isSaved, "le ticket n'est pas sauvegardé correctement");
        assertTrue(ticketDAO.getNbTicket(ticket.getVehicleRegNumber()) >= 1);
    }
}
