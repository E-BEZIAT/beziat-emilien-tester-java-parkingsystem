package com.parkit.parkingsystem.dao;

import com.parkit.parkingsystem.config.DataBaseConfig;
import com.parkit.parkingsystem.constants.DBConstants;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TicketDAO {

    private static final Logger logger = LogManager.getLogger("TicketDAO");

    public DataBaseConfig dataBaseConfig = new DataBaseConfig();

    public boolean saveTicket(Ticket ticket){
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = dataBaseConfig.getConnection();
            ps = con.prepareStatement(DBConstants.SAVE_TICKET);
            ps.setInt(1, ticket.getParkingSpot().getId());
            ps.setString(2, ticket.getVehicleRegNumber());
            ps.setDouble(3, ticket.getPrice());
            ps.setTimestamp(4, new java.sql.Timestamp(ticket.getInTime().getTime()));
            ps.setTimestamp(5, (ticket.getOutTime() == null)?null: (new java.sql.Timestamp(ticket.getOutTime().getTime())) );
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
            ps = con.prepareStatement(DBConstants.UPDATE_TICKET);
            ps.setDouble(1, ticket.getPrice());
            ps.setTimestamp(2, new java.sql.Timestamp(ticket.getOutTime().getTime()));
            ps.setInt(3, ticket.getId());
            int updatedRowsCount = ps.executeUpdate();
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
