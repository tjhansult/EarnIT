package nl.earnit.dao;

import nl.earnit.helpers.PostgresJDBCHelper;
import nl.earnit.models.db.User;
import nl.earnit.models.db.Worked;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WorkedDAO extends GenericDAO<User> {

    private final static String TABLE_NAME = "worked";

    public WorkedDAO(Connection con) {
        super(con, TABLE_NAME);
    }

    // counts how many rows worked has
    @Override
    public int count() throws SQLException {
        // Create query
        String query = "SELECT COUNT(*) AS count FROM  \"" + tableName + "\"";
        PreparedStatement counter = this.con.prepareStatement(query);

        // Execute query
        ResultSet res = counter.executeQuery();

        // Return count
        res.next();
        return res.getInt("count");
    }

    public List<Worked> getWorkedHours(String userContractId) throws SQLException {
        // Create query
        String query = "SELECT id, worked_week_id, day, minutes, work  FROM  \"" + tableName + "\" t JOIN worked_week ww ON ww.id=t.worked_week_id WHERE ww.contract_id=?";
        PreparedStatement counter = this.con.prepareStatement(query);
        PostgresJDBCHelper.setUuid(counter, 1, userContractId);
        // Execute query
        ResultSet res = counter.executeQuery();
        // Return count
        res.next();
        List<Worked> list = new ArrayList<>();
        while (res.next()) {
            Worked w = new Worked(res.getString("id"), res.getString("worked_week_id"), res.getInt("day"), res.getInt("minutes"), res.getString("work"));
            list.add(w);
        }
        return list;
    }

    public List<Worked> getWorkedWeek(String userContractId, int year, int week) throws SQLException {
        String query = "SELECT id, worked_week_id, day, minutes, work  FROM  \"" + tableName + "\" t JOIN worked_week ww ON ww.id=t.worked_week_id WHERE ww.contract_id=? AND ww.year=? AND ww.week=?";
        PreparedStatement counter = this.con.prepareStatement(query);
        PostgresJDBCHelper.setUuid(counter, 1, userContractId);
        PostgresJDBCHelper.setUuid(counter, 2, String.valueOf(year));
        PostgresJDBCHelper.setUuid(counter, 3, String.valueOf(week));
        // Execute query
        ResultSet res = counter.executeQuery();
        // Return count
        res.next();
        List<Worked> list = new ArrayList<>();
        while (res.next()) {
            Worked w = new Worked(res.getString("id"), res.getString("worked_week_id"), res.getInt("day"), res.getInt("minutes"), res.getString("work"));
            list.add(w);
        }
        return list;
    }

    public List<Worked> getWorkedWeekById(String userContractId, int weekId) throws SQLException {
        String query = "SELECT id, worked_week_id, day, minutes, work  FROM  \"" + tableName + "\" t JOIN worked_week ww ON ww.id=t.worked_week_id WHERE ww.contract_id=? AND ww.id=?";
        PreparedStatement counter = this.con.prepareStatement(query);
        PostgresJDBCHelper.setUuid(counter, 1, userContractId);
        PostgresJDBCHelper.setUuid(counter, 2, String.valueOf(weekId));
        // Execute query
        ResultSet res = counter.executeQuery();
        // Return count
        res.next();
        List<Worked> list = new ArrayList<>();
        while (res.next()) {
            Worked w = new Worked(res.getString("id"), res.getString("worked_week_id"), res.getInt("day"), res.getInt("minutes"), res.getString("work"));
            list.add(w);
        }
        return list;
    }
}
