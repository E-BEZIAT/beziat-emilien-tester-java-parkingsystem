package com.parkit.parkingsystem.dao;

import com.parkit.parkingsystem.config.DataBaseConfig;
import com.parkit.parkingsystem.constants.DBConstants;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;

import static com.parkit.parkingsystem.constants.DBConstants.GET_NB_TICKET;

public class TicketDAO {

    private static final Logger logger = LogManager.getLogger("TicketDAO");

    public DataBaseConfig dataBaseConfig = new DataBaseConfig();

    public boolean saveTicket(Ticket ticket){
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = dataBaseConfig.getConnection();
            ps = con.prepareStatement(DBConstants.SAVE_TICKET);
            //ID, PARKING_NUMBER, VEHICLE_REG_NUMBER, PRICE, IN_TIME, OUT_TIME)
            //ps.setInt(1,ticket.getId());
            ps.setInt(1, ticket.getParkingSpot().getId());
            ps.setString(2, ticket.getVehicleRegNumber());
            ps.setDouble(3, ticket.getPrice());
            ps.setTimestamp(4, new java.sql.Timestamp(ticket.getInTime().getTime()));
            ps.setTimestamp(5, (ticket.getOutTime() == null)?null: (new java.sql.Timestamp(ticket.getOutTime().getTime())) );
            ps.setBoolean(6,ticket.getRegularCustomer());
            return ps.executeUpdate() > 0;
        }catch (Exception ex){
            logger.error("Error saving ticket",ex);
            return false;
        }finally {
            dataBaseConfig.closeConnection(con);
            dataBaseConfig.closePreparedStatement(ps);
        }
    }

    public Ticket getTicket(String vehicleRegNumber) {
        Connection con = null;
        Ticket ticket = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = dataBaseConfig.getConnection();
            ps = con.prepareStatement(DBConstants.GET_TICKET);
            //ID, PARKING_NUMBER, VEHICLE_REG_NUMBER, PRICE, IN_TIME, OUT_TIME)
            ps.setString(1,vehicleRegNumber);
            rs = ps.executeQuery();
            if(rs.next()){
                ticket = new Ticket();
                ParkingSpot parkingSpot = new ParkingSpot(rs.getInt(2), ParkingType.valueOf(rs.getString(6)),false);
                ticket.setId(rs.getInt(1));
                ticket.setParkingSpot(parkingSpot);
                ticket.setVehicleRegNumber(vehicleRegNumber);
                ticket.setPrice(rs.getDouble(3));
                ticket.setInTime(rs.getTimestamp(4));
                ticket.setOutTime(rs.getTimestamp(5));
            }
        }catch (Exception ex){
            logger.error("Error retrieving ticket from DB",ex);
            return null;
        }finally {
            dataBaseConfig.closeConnection(con);
            dataBaseConfig.closeResultSet(rs);
            dataBaseConfig.closePreparedStatement(ps);
        }
        return ticket;
    }

    public boolean updateTicket(Ticket ticket) {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = dataBaseConfig.getConnection();
            logger.info("Database connection established successfully.");
            ps = con.prepareStatement(DBConstants.UPDATE_TICKET);
            ps.setDouble(1, ticket.getPrice());
            ps.setTimestamp(2, new java.sql.Timestamp(ticket.getOutTime().getTime()));
            ps.setInt(3, ticket.getId());
            logger.info("Executing updateTicket query with parameters: Price=" + ticket.getPrice() +
                    ", OutTime=" + ticket.getOutTime() +
                    ", VehicleRegNumber=" + ticket.getVehicleRegNumber());
            System.out.println("UPDATE ticket SET PRICE = " + ticket.getPrice() +
                    ", OUT_TIME = " + new Timestamp(ticket.getOutTime().getTime()) +
                    " WHERE VEHICLE_REG_NUMBER = '" + ticket.getVehicleRegNumber() + "'");
            int updatedRowsCount = ps.executeUpdate();

            logger.info("Update ticket query executed. Rows updated: " + updatedRowsCount);
            return (updatedRowsCount == 1);
        }catch (Exception ex){
            logger.error("Error updating ticket ",ex);
            return false;
        }finally {
            dataBaseConfig.closeConnection(con);
            dataBaseConfig.closePreparedStatement(ps);
        }
    }

    public int getNbTicket(String vehicleRegNumber) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        int nbTicket = 0;
        System.out.println("number of ticket : " + vehicleRegNumber + " = " + nbTicket);
        try {
            con = dataBaseConfig.getConnection();
            ps = con.prepareStatement(DBConstants.GET_NB_TICKET);
            ps.setString(1, vehicleRegNumber);
            rs = ps.executeQuery();
            if(rs.next()){
                nbTicket = rs.getInt(1);
            }
        } catch (SQLException ex) {
            logger.error("Error fetching ticket count",ex);
            return 0;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            dataBaseConfig.closeConnection(con);
            dataBaseConfig.closeResultSet(rs);
            dataBaseConfig.closePreparedStatement(ps);
        }
        return nbTicket;
    }
}
