package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

    private static ParkingService parkingService;

    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static ParkingSpotDAO parkingSpotDAO;
    @Mock
    private static TicketDAO ticketDAO;

    @BeforeEach
    public void setUpPerTest() {
        try {
            lenient().when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
            ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);
            Ticket ticket = new Ticket();
            ticket.setInTime(new Date(System.currentTimeMillis() - (60*60*1000)));
            ticket.setParkingSpot(parkingSpot);
            ticket.setVehicleRegNumber("ABCDEF");
            lenient().when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
            lenient().when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
            lenient().when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        } catch (Exception e) {
            e.printStackTrace();
            throw  new RuntimeException("Failed to set up test mock objects");
        }
    }

    @Test
    public void processExitingVehicleTest(){

        //GIVEN

        Ticket ticket = new Ticket(); // Create ticket with vehicle details
        ticket.setVehicleRegNumber("ABCDEF");
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR,false));
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000))); // Set ticket timestamps and initial status
        ticket.setOutTime(null);
        ticket.setPrice(0);

        //WHEN

        when(ticketDAO.getTicket("ABCDEF")).thenReturn(ticket); // Mock ticket services
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
        when(ticketDAO.getNbTicket("ABCDEF")).thenReturn(5);

        parkingService.processExitingVehicle(); // Process vehicle exit

        //THEN

        verify(ticketDAO, times(1)).updateTicket(any(Ticket.class)); // Verify ticket update
        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketDAO).updateTicket(ticketCaptor.capture());
        Ticket updatedTicket = ticketCaptor.getValue();
        assertNotNull(updatedTicket.getOutTime(), "heure de sortie");
        assertTrue(updatedTicket.getPrice() > 0, "prix du ticket");
        verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class)); // Verify parking spot update
        ArgumentCaptor<ParkingSpot> parkingSpotCaptor = ArgumentCaptor.forClass(ParkingSpot.class);
        verify(parkingSpotDAO).updateParking(parkingSpotCaptor.capture());
        ParkingSpot updatedParkingSpot = parkingSpotCaptor.getValue();
        assertTrue(updatedParkingSpot.isAvailable(), "place de parking libre");
        verify(ticketDAO, times(1)).getNbTicket("ABCDEF"); // Check ticket count and apply discount
        if(ticketDAO.getNbTicket("ABCDEF") > 1){
            double delta = 0.001;
            assertEquals((0.95 * Fare.CAR_RATE_PER_HOUR), ticket.getPrice(), delta);
        }
    }

    @Test
    public void processIncomingVehicleTest() throws Exception {

        //GIVEN

        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false); // Create parking spot and ticket
        Ticket ticket = new Ticket();
        ticket.setVehicleRegNumber("ABCDEF");
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR,false));
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000))); // Set ticket timestamps and initial status
        ticket.setOutTime(null);
        ticket.setPrice(0);

        //WHEN

        when(inputReaderUtil.readSelection()).thenReturn(1); // Mock input selections and services
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);
        when(ticketDAO.saveTicket(any(Ticket.class))).thenReturn(true);
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
        when(ticketDAO.getNbTicket("ABCDEF")).thenReturn(0);
        parkingService.processIncomingVehicle(); // Process incoming vehicle

        //THEN

        assertNotNull(parkingSpot); // Verify parking spot
        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class); // Verify ticket save
        verify(ticketDAO, times(1)).saveTicket(ticketCaptor.capture());
        Ticket savedTicket = ticketCaptor.getValue();
        assertNotNull(savedTicket.getInTime(), "heure d'entrée'"); // Check ticket details
        assertEquals("ABCDEF", savedTicket.getVehicleRegNumber(), "Numéro d'immatriculation incorrect");
        assertEquals(0, savedTicket.getPrice(), "Le prix initial devrait être 0");
        assertNull(savedTicket.getOutTime(), "L'heure de sortie devrait être null");
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class)); // Verify parking spot update
        ArgumentCaptor<ParkingSpot> parkingSpotCaptor = ArgumentCaptor.forClass(ParkingSpot.class);
        verify(parkingSpotDAO).updateParking(parkingSpotCaptor.capture());
        verify(ticketDAO, Mockito.times(1)).getNbTicket("ABCDEF"); // Check ticket count and apply potential discount
        if(ticketDAO.getNbTicket("ABCDEF") > 1){
            double delta = 0.001;
            assertEquals((0.95 * Fare.CAR_RATE_PER_HOUR), ticket.getPrice(), delta);
        }
    }

    @Test
    public void processIncomingVehicleTestError() throws Exception {

        //WHEN

        when(inputReaderUtil.readSelection()).thenReturn(1); // Mock input selections with error
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenThrow(new RuntimeException("Invalid input provided"));
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);
        parkingService =  new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO); // Initialize parking service and process incoming vehicle
        parkingService.processIncomingVehicle();

        //THEN

        verify(parkingSpotDAO, times(0)).updateParking(any(ParkingSpot.class)); // Verify no updates occurred due to error
        verify(ticketDAO, Mockito.times(0)).saveTicket(any(Ticket.class));
    }

    @Test
    public void processExitingVehicleTestUnableUpdate() throws Exception {

        //GIVEN

        Ticket ticket = new Ticket(); // Create ticket with vehicle details
        ticket.setVehicleRegNumber("ABCDEF");
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR,false));
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000))); // Set ticket timestamps and initial status
        ticket.setOutTime(new Date());
        ticket.setPrice(0);

        //WHEN

        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF"); // Mock ticket services for exit scenario
        when(ticketDAO.getTicket("ABCDEF")).thenReturn(ticket);
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);
        lenient().when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
        parkingService.processExitingVehicle(); // Process vehicle exit

        //THEN

        verify(ticketDAO, times(1)).updateTicket(ticket); // Verify ticket update attempt
        assertFalse(ticketDAO.updateTicket(ticket), "Expected ticket update to fail.");
        verify(parkingSpotDAO, never()).updateParking(any(ParkingSpot.class)); // Verify no parking spot updates
    }

    @Test
    public void testGetNextParkingNumberIfAvailableCAR() throws Exception {

        //WHEN

        when(inputReaderUtil.readSelection()).thenReturn(1); // Mock input selection and parking spot availability
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);
        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable(); // Get next available parking spot

        //THEN

        assertNotNull(parkingSpot); // Verify parking spot details
        assertEquals(ParkingType.CAR, parkingSpot.getParkingType());
        assertEquals(parkingSpot.getId(), 1);
        verify(parkingSpotDAO, Mockito.times(1)).getNextAvailableSlot(any(ParkingType.class));
    }

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberNotFound() throws Exception {

        //WHEN

        when(inputReaderUtil.readSelection()).thenReturn(1); // Mock input selection and no available parking spots
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(0);
        Exception exception = assertThrows(Exception.class, () -> parkingService.getNextParkingNumberIfAvailable()); // Verify exception is thrown when no parking spot is available

        //THEN

        assertEquals("Error fetching parking number from DB. Parking slots might be full", exception.getMessage()); // Verify exception message and parking spot lookup
        verify(parkingSpotDAO, Mockito.times(1)).getNextAvailableSlot(any(ParkingType.class));
    }

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument() throws Exception {

        //WHEN

        when(inputReaderUtil.readSelection()).thenReturn(3); // Mock invalid input selection
        assertThrows(Exception.class, () -> parkingService.getNextParkingNumberIfAvailable()); // Verify exception is thrown for invalid selection

        //THEN

        verify(parkingSpotDAO, never()).getNextAvailableSlot(any(ParkingType.class)); // Verify no parking spot lookup occurs for invalid selection
    }

}
