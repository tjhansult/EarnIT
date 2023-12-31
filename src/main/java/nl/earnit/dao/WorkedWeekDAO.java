package nl.earnit.dao;

import nl.earnit.dto.company.CreateNoteDTO;
import nl.earnit.dto.contracts.ContractDTO;
import nl.earnit.dto.user.UserResponseDTO;
import nl.earnit.dto.workedweek.WorkedWeekDTO;
import nl.earnit.exceptions.InvalidOrderByException;
import nl.earnit.helpers.PostgresJDBCHelper;
import nl.earnit.models.*;
import org.postgresql.util.PGobject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The type Worked week dao.
 */
public class WorkedWeekDAO extends GenericDAO<User> {
    private final OrderBy orderBy = new OrderBy(new HashMap<>() {{
        put("worked_week.id", "ww.id");
        put("worked_week.year", "ww.year");
        put("worked_week.week", "ww.week");
        put("worked_week.status", "ww.status");
        put("worked_week.total_hours", "w.minutes");

        put("user_contract.contract_id", "uc.contract_id");
        put("user_contract.user_id", "uc.user_id");
        put("user_contract.hourly_wage", "uc.hourly_wage");
        put("user_contract.active", "uc.active");

        put("user.id", "u.id");
        put("user.first_name", "u.first_name");
        put("user.last_name", "u.last_name");
        put("user.last_name_prefix", "u.last_name_prefix");
        put("user.email", "u.email");

        put("contract.id", "c.id");
        put("contract.company_id", "c.company_id");
        put("contract.role", "c.role");

        put("company.id", "cy.id");
        put("company.name", "cy.name");
    }});

    private final OrderBy orderByHours = new OrderBy(new HashMap<>() {{
        put("hours.day", "w.day");
        put("hours.minutes", "w.minutes");
    }});

    private final static String TABLE_NAME = "worked_week";

    /**
     * Instantiates a new Worked week dao.
     *
     * @param con the con
     */
    public WorkedWeekDAO(Connection con) {
        super(con, TABLE_NAME);
    }

    @Override
    public int count() throws SQLException {
        // Create query
        String query = """
             SELECT COUNT(DISTINCT ww.id) AS count FROM "%s" ww
            """.formatted(tableName);

        PreparedStatement counter = this.con.prepareStatement(query);

        // Execute query
        ResultSet res = counter.executeQuery();

        // Return count
        res.next();
        return res.getInt("count");
    }

    /**
     * Confirm worked week.
     *
     * @param userContractId the user contract id
     * @param year           the year
     * @param week           the week
     * @throws SQLException the sql SQLException
     */
    public void confirmWorkedWeek(String userContractId, String year, String week) throws SQLException {
        String query = "UPDATE worked_week SET status = 'CONFIRMED' WHERE contract_id = ? AND year = ? AND week = ?";
        PreparedStatement statement = con.prepareStatement(query);
        PostgresJDBCHelper.setUuid(statement, 1, userContractId);
        statement.setInt(2, Integer.parseInt(year));
        statement.setInt(3, Integer.parseInt(week));
        statement.executeUpdate();
    }

    /**
     * Remove confirm worked week boolean.
     *
     * @param userContractId the user contract id
     * @param year           the year
     * @param week           the week
     * @return the boolean
     * @throws SQLException the sql SQLException
     */
    public boolean removeConfirmWorkedWeek(String userContractId, String year, String week) throws SQLException {
        if (hasDatePassed(year, week)) {
            return false;
        }

        String query = "UPDATE worked_week SET status = 'NOT_CONFIRMED' WHERE contract_id = ? AND year = ? AND week = ?";
        PreparedStatement statement = con.prepareStatement(query);
        PostgresJDBCHelper.setUuid(statement, 1, userContractId);
        statement.setInt(2, Integer.parseInt(year));
        statement.setInt(3, Integer.parseInt(week));
        statement.executeUpdate();
        return true;
    }

    /**
     * Add worked week note boolean.
     *
     * @param note           the note
     * @param userContractId the user contract id
     * @param year           the year
     * @param week           the week
     * @return the boolean
     * @throws SQLException the sql SQLException
     */
    public boolean addWorkedWeekNote(String note, String userContractId, String year, String week) throws SQLException {
        String query = "UPDATE worked_week SET note = ? WHERE contract_id = ? AND year = ? AND week = ?";
        PreparedStatement statement = con.prepareStatement(query);
        statement.setString(1, note);
        PostgresJDBCHelper.setUuid(statement, 2, userContractId);
        try {
            statement.setInt(3, Integer.parseInt(year));
            statement.setInt(4, Integer.parseInt(week));
            statement.executeUpdate();
        } catch(NumberFormatException e) {
            return false;
        }
        return true;
    }

    /**
     * Counts all worked weeks the user has access to. Either via a company user or a contract user.
     *
     * @param userId The id of the user.
     * @return the int
     * @throws SQLException If a database error occurs.
     */
    public int countWorkedWeekForUser(String userId) throws SQLException {
        String query = """
            SELECT COUNT(DISTINCT ww.id) AS count FROM "%s" ww
                        
                JOIN user_contract uc ON uc.id = ww.contract_id
                JOIN contract c ON c.id = uc.contract_id
                JOIN company cy ON cy.id = c.company_id
                JOIN company_user cu ON cu.company_id = cy.id
                
                WHERE uc.user_id = ? OR cu.user_id = ?
            """.formatted(tableName);

        PreparedStatement counter = this.con.prepareStatement(query);

        PostgresJDBCHelper.setUuid(counter, 1, userId);
        PostgresJDBCHelper.setUuid(counter, 2, userId);

        // Execute query
        ResultSet res = counter.executeQuery();

        // Return count
        res.next();
        return res.getInt("count");
    }

    /**
     * Gets worked week by id.
     *
     * @param id the id
     * @return the worked week by id
     * @throws SQLException the sql SQLException
     */
    public WorkedWeekDTO getWorkedWeekById(String id) throws SQLException {
        return getWorkedWeekById(id, false, false, false, false, false, false, "hours.day:asc");
    }

    /**
     * Gets all worked weeks the user has access to. Either via a company user or a contract user.
     *
     * @param userId           The id of the user.
     * @param userContractId   the user contract id
     * @param year             the year
     * @param week             the week
     * @param withCompany      the with company
     * @param withContract     the with contract
     * @param withUserContract the with user contract
     * @param withUser         the with user
     * @param withHours        the with hours
     * @param withTotalHours   the with total hours
     * @param order            the order
     * @return the worked weeks for user
     * @throws SQLException If a database error occurs.
     */
    public List<WorkedWeekDTO> getWorkedWeeksForUser(String userId, String userContractId, Integer year, Integer week, boolean withCompany,
                                                     boolean withContract, boolean withUserContract,
                                                     boolean withUser, boolean withHours, boolean withTotalHours, String order) throws SQLException {
        List<String> where = new ArrayList<>();
        if (userId != null) where.add("u.id");
        if (userContractId != null) where.add("uc.id");
        if (year != null) where.add("ww.year");
        if (week != null) where.add("ww.week");

        if (where.isEmpty() || (userId == null && userContractId == null)) throw new IllegalArgumentException();

        String query = """
            SELECT DISTINCT ww.id as worked_week_id,
                ww.contract_id as worked_week_contract_id,
                ww.year as worked_week_year,
                ww.week as worked_week_week,
                ww.note as worked_week_note,
                ww.status as worked_week_status,
                ww.company_note as worked_week_company_note,
                
                uc.id as user_contract_id,
                uc.contract_id as user_contract_contract_id,
                uc.user_id as user_contract_user_id,
                uc.hourly_wage as user_contract_hourly_wage,
                uc.active as user_contract_active,
                
                u.id as user_id,
                u.first_name as user_first_name,
                u.last_name as user_last_name,
                u.last_name_prefix as user_last_name_prefix,
                u.email as user_email,
                u.type as user_type,
                u.kvk as user_kvk,
                u.btw as user_btw,
                u.address as user_address,
                
                c.id as contract_id,
                c.company_id as contract_company_id,
                c.role as contract_role,
                c.description as contract_description,
                
                cy.id as company_id,
                cy.name as company_name,
                cy.kvk as company_kvk,
                cy.address as company_address,
                
                w.hours,
                w.minutes
                
                FROM "%s" ww
                        
                JOIN user_contract uc ON uc.id = ww.contract_id
                JOIN "user" u ON u.id = uc.user_id
                JOIN contract c ON c.id = uc.contract_id
                JOIN company cy ON cy.id = c.company_id
                
                LEFT JOIN (select w.worked_week_id, array_agg(w.*%2$s) as hours, sum(w.minutes) as minutes FROM worked w GROUP BY w.worked_week_id) w ON w.worked_week_id = ww.id
                
                WHERE %3$s
            """.formatted(tableName, orderByHours.getSQLOrderBy(order, true), String.join(" AND ", where.stream().map(x -> x + " = ?").toList()));

        PreparedStatement statement = this.con.prepareStatement(query);

        if (userId != null)  PostgresJDBCHelper.setUuid(statement, where.indexOf("u.id") + 1, userId);
        if (userContractId != null)  PostgresJDBCHelper.setUuid(statement, where.indexOf("uc.id") + 1, userContractId);
        if (year != null) statement.setInt(where.indexOf("ww.year") + 1, year);
        if (week != null) statement.setInt(where.indexOf("ww.week") + 1, week);

        // Execute query
        ResultSet res = statement.executeQuery();

        // Return
        List<WorkedWeekDTO> workedWeeks = new ArrayList<>();

        while (res.next()) {
            workedWeeks.add(getWorkedWeekFromRow(res, "worked_week_", withCompany, withContract, withUserContract, withUser, withHours, withTotalHours));
        }

        return workedWeeks;
    }

    /**
     * Gets all worked weeks the company has access to per week.
     *
     * @param companyId        The id of the company.
     * @param year             the year
     * @param week             the week
     * @param withCompany      the with company
     * @param withContract     the with contract
     * @param withUserContract the with user contract
     * @param withUser         the with user
     * @param withHours        the with hours
     * @param withTotalHours   the with total hours
     * @param order            the order
     * @return the worked weeks for company
     * @throws SQLException If a database error occurs.
     */
    public List<WorkedWeekDTO> getWorkedWeeksForCompany(String companyId, int year, int week, boolean withCompany,
                                                        boolean withContract, boolean withUserContract,
                                                        boolean withUser, boolean withHours, boolean withTotalHours, String order) throws SQLException {
        String query = """
            SELECT DISTINCT ww.id as worked_week_id,
                ww.contract_id as worked_week_contract_id,
                ww.year as worked_week_year,
                ww.week as worked_week_week,
                ww.note as worked_week_note,
                ww.status as worked_week_status,
                ww.company_note as worked_week_company_note,
                
                uc.id as user_contract_id,
                uc.contract_id as user_contract_contract_id,
                uc.user_id as user_contract_user_id,
                uc.hourly_wage as user_contract_hourly_wage,
                uc.active as user_contract_active,
                
                u.id as user_id,
                u.first_name as user_first_name,
                u.last_name as user_last_name,
                u.last_name_prefix as user_last_name_prefix,
                u.email as user_email,
                u.type as user_type,
                u.kvk as user_kvk,
                u.btw as user_btw,
                u.address as user_address,
                
                c.id as contract_id,
                c.company_id as contract_company_id,
                c.role as contract_role,
                c.description as contract_description,
                
                cy.id as company_id,
                cy.name as company_name,
                cy.kvk as company_kvk,
                cy.address as company_address,
                
                w.hours,
                w.minutes
                
                FROM "%s" ww
                        
                JOIN user_contract uc ON uc.id = ww.contract_id
                JOIN "user" u ON u.id = uc.user_id
                JOIN contract c ON c.id = uc.contract_id
                JOIN company cy ON cy.id = c.company_id
                
                LEFT JOIN (select w.worked_week_id, array_agg(w.*%2$s) as hours, sum(w.minutes) as minutes FROM worked w GROUP BY w.worked_week_id) w ON w.worked_week_id = ww.id
                                
                WHERE ww.status = 'APPROVED' AND ww.year = ? AND ww.week = ? AND cy.id = ?
                %3$s
            """.formatted(tableName, orderByHours.getSQLOrderBy(order, true), orderBy.getSQLOrderBy(order, true));

        PreparedStatement statement = this.con.prepareStatement(query);

        statement.setInt(1, year);
        statement.setInt(2, week);
        PostgresJDBCHelper.setUuid(statement, 3, companyId);

        // Execute query
        ResultSet res = statement.executeQuery();

        // Return
        List<WorkedWeekDTO> workedWeeks = new ArrayList<>();

        while (res.next()) {
            workedWeeks.add(getWorkedWeekFromRow(res, "worked_week_", withCompany, withContract, withUserContract, withUser, withHours, withTotalHours));
        }

        return workedWeeks;
    }

    /**
     * Gets all worked weeks the company has access to per week.
     *
     * @param companyId        The id of the company.
     * @param userId           the user id
     * @param withCompany      the with company
     * @param withContract     the with contract
     * @param withUserContract the with user contract
     * @param withUser         the with user
     * @param withHours        the with hours
     * @param withTotalHours   the with total hours
     * @param order            the order
     * @return the worked weeks for company for user
     * @throws SQLException If a database error occurs.
     */
    public List<WorkedWeekDTO> getWorkedWeeksForCompanyForUser(String companyId, String userId, boolean withCompany,
                                                               boolean withContract, boolean withUserContract,
                                                               boolean withUser, boolean withHours, boolean withTotalHours, String order) throws SQLException {
        String query = """
            SELECT DISTINCT ww.id as worked_week_id,
                ww.contract_id as worked_week_contract_id,
                ww.year as worked_week_year,
                ww.week as worked_week_week,
                ww.note as worked_week_note,
                ww.status as worked_week_status,
                ww.company_note as worked_week_company_note,
                
                uc.id as user_contract_id,
                uc.contract_id as user_contract_contract_id,
                uc.user_id as user_contract_user_id,
                uc.hourly_wage as user_contract_hourly_wage,
                uc.active as user_contract_active,
                
                u.id as user_id,
                u.first_name as user_first_name,
                u.last_name as user_last_name,
                u.last_name_prefix as user_last_name_prefix,
                u.email as user_email,
                u.type as user_type,
                u.kvk as user_kvk,
                u.btw as user_btw,
                u.address as user_address,
                
                c.id as contract_id,
                c.company_id as contract_company_id,
                c.role as contract_role,
                c.description as contract_description,
                
                cy.id as company_id,
                cy.name as company_name,
                cy.kvk as company_kvk,
                cy.address as company_address,
                
                w.hours,
                w.minutes
                
                FROM "%s" ww
                        
                JOIN user_contract uc ON uc.id = ww.contract_id
                JOIN "user" u ON u.id = uc.user_id
                JOIN contract c ON c.id = uc.contract_id
                JOIN company cy ON cy.id = c.company_id
                
                LEFT JOIN (select w.worked_week_id, array_agg(w.*%2$s) as hours, sum(w.minutes) as minutes FROM worked w GROUP BY w.worked_week_id) w ON w.worked_week_id = ww.id
                                
                WHERE cy.id = ? AND ww.status = 'APPROVED' AND u.id = ?
                %3$s
            """.formatted(tableName, orderByHours.getSQLOrderBy(order, true), orderBy.getSQLOrderBy(order, true));

        PreparedStatement statement = this.con.prepareStatement(query);

        PostgresJDBCHelper.setUuid(statement, 1, companyId);
        PostgresJDBCHelper.setUuid(statement, 2, userId);

        // Execute query
        ResultSet res = statement.executeQuery();

        // Return
        List<WorkedWeekDTO> workedWeeks = new ArrayList<>();

        while (res.next()) {
            workedWeeks.add(getWorkedWeekFromRow(res, "worked_week_", withCompany, withContract, withUserContract, withUser, withHours, withTotalHours));
        }

        return workedWeeks;
    }

    /**
     * Gets all worked weeks for staff and is ready for approval.
     *
     * @param withCompany      the with company
     * @param withContract     the with contract
     * @param withUserContract the with user contract
     * @param withUser         the with user
     * @param withHours        the with hours
     * @param withTotalHours   the with total hours
     * @param order            the order
     * @return the worked weeks to approve for staff
     * @throws SQLException            If a database error occurs.
     * @throws InvalidOrderByException the invalid order by SQLException
     */
    public List<WorkedWeekDTO> getWorkedWeeksToApproveForStaff(boolean withCompany,
                                                               boolean withContract,
                                                               boolean withUserContract,
                                                               boolean withUser, boolean withHours, boolean withTotalHours, String order)
        throws SQLException, InvalidOrderByException {

        int currentWeek = LocalDate.now().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int currentYear = LocalDate.now().get(IsoFields.WEEK_BASED_YEAR);

        String query = """
            SELECT DISTINCT ww.id as worked_week_id,
                ww.contract_id as worked_week_contract_id,
                ww.year as worked_week_year,
                ww.week as worked_week_week,
                ww.note as worked_week_note,
                ww.status as worked_week_status,
                ww.company_note as worked_week_company_note,
                
                uc.id as user_contract_id,
                uc.contract_id as user_contract_contract_id,
                uc.user_id as user_contract_user_id,
                uc.hourly_wage as user_contract_hourly_wage,
                uc.active as user_contract_active,
                
                u.id as user_id,
                u.first_name as user_first_name,
                u.last_name as user_last_name,
                u.last_name_prefix as user_last_name_prefix,
                u.email as user_email,
                u.type as user_type,
                u.kvk as user_kvk,
                u.btw as user_btw,
                u.address as user_address,
                
                c.id as contract_id,
                c.company_id as contract_company_id,
                c.role as contract_role,
                c.description as contract_description,
                
                cy.id as company_id,
                cy.name as company_name,
                cy.kvk as company_kvk,
                cy.address as company_address,
                
                w.hours,
                w.minutes
                
                FROM "%s" ww
                        
                JOIN user_contract uc ON uc.id = ww.contract_id
                JOIN "user" u ON u.id = uc.user_id
                JOIN contract c ON c.id = uc.contract_id
                JOIN company cy ON cy.id = c.company_id
                
                LEFT JOIN (select w.worked_week_id, array_agg(w.*%2$s) as hours, sum(w.minutes) as minutes FROM worked w GROUP BY w.worked_week_id) w ON w.worked_week_id = ww.id
                                
                WHERE ww.status = 'SUGGESTION_DENIED' AND (ww.year < ? OR (ww.year = ? AND ww.week < ?))
                %3$s
            """.formatted(tableName, orderByHours.getSQLOrderBy(order, true), orderBy.getSQLOrderBy(order, true));

        PreparedStatement statement = this.con.prepareStatement(query);

        statement.setInt(1, currentYear);
        statement.setInt(2, currentYear);
        statement.setInt(3, currentWeek);

        // Execute query
        ResultSet res = statement.executeQuery();

        // Return
        List<WorkedWeekDTO> workedWeeks = new ArrayList<>();

        while (res.next()) {
            workedWeeks.add(getWorkedWeekFromRow(res, "worked_week_", withCompany, withContract, withUserContract, withUser, withHours, withTotalHours));
        }

        return workedWeeks;
    }

    /**
     * Get the worked week.
     *
     * @param workedWeekId     The id of the worked week.
     * @param withCompany      the with company
     * @param withContract     the with contract
     * @param withUserContract the with user contract
     * @param withUser         the with user
     * @param withHours        the with hours
     * @param withTotalHours   the with total hours
     * @param order            the order
     * @return the worked week by id
     * @throws SQLException If a database error occurs.
     */
    public WorkedWeekDTO getWorkedWeekById(String workedWeekId, boolean withCompany,
                                           boolean withContract, boolean withUserContract,
                                           boolean withUser, boolean withHours, boolean withTotalHours, String order) throws SQLException {
        String query = """
            SELECT DISTINCT ww.id as worked_week_id,
                ww.contract_id as worked_week_contract_id,
                ww.year as worked_week_year,
                ww.week as worked_week_week,
                ww.note as worked_week_note,
                ww.status as worked_week_status,
                ww.company_note as worked_week_company_note,
                
                uc.id as user_contract_id,
                uc.contract_id as user_contract_contract_id,
                uc.user_id as user_contract_user_id,
                uc.hourly_wage as user_contract_hourly_wage,
                uc.active as user_contract_active,
                
                u.id as user_id,
                u.first_name as user_first_name,
                u.last_name as user_last_name,
                u.last_name_prefix as user_last_name_prefix,
                u.email as user_email,
                u.type as user_type,
                u.kvk as user_kvk,
                u.btw as user_btw,
                u.address as user_address,
                
                c.id as contract_id,
                c.company_id as contract_company_id,
                c.role as contract_role,
                c.description as contract_description,
                
                cy.id as company_id,
                cy.name as company_name,
                cy.kvk as company_kvk,
                cy.address as company_address,
                
                w.hours,
                w.minutes
                
                FROM "%s" ww
                        
                JOIN user_contract uc ON uc.id = ww.contract_id
                JOIN "user" u ON u.id = uc.user_id
                JOIN contract c ON c.id = uc.contract_id
                JOIN company cy ON cy.id = c.company_id
                
                LEFT JOIN (select w.worked_week_id, array_agg(w.*%2$s) as hours, sum(w.minutes) as minutes FROM worked w GROUP BY w.worked_week_id) w ON w.worked_week_id = ww.id
                
                WHERE ww.id = ?
                LIMIT 1
            """.formatted(tableName, orderByHours.getSQLOrderBy(order, true));

        PreparedStatement statement = this.con.prepareStatement(query);

        PostgresJDBCHelper.setUuid(statement, 1, workedWeekId);

        // Execute query
        ResultSet res = statement.executeQuery();

        // Return

        if (!res.next()) {
            return null;
        }
        return getWorkedWeekFromRow(res, "worked_week_", withCompany, withContract, withUserContract, withUser, withHours, withTotalHours);
    }

    /**
     * Gets all worked weeks in a company and is ready for approval.
     *
     * @param companyId        The id of the company.
     * @param withCompany      the with company
     * @param withContract     the with contract
     * @param withUserContract the with user contract
     * @param withUser         the with user
     * @param withHours        the with hours
     * @param withTotalHours   the with total hours
     * @param order            the order
     * @return the worked weeks to approve for company
     * @throws SQLException            If a database error occurs.
     * @throws InvalidOrderByException the invalid order by SQLException
     */
    public List<WorkedWeekDTO> getWorkedWeeksToApproveForCompany(String companyId,
                                                                 boolean withCompany,
                                                                 boolean withContract,
                                                                 boolean withUserContract,
                                                                 boolean withUser, boolean withHours, boolean withTotalHours, String order)
        throws SQLException, InvalidOrderByException {

        int currentWeek = LocalDate.now().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int currentYear = LocalDate.now().get(IsoFields.WEEK_BASED_YEAR);

        String query = """
            SELECT DISTINCT ww.id as worked_week_id,
                ww.contract_id as worked_week_contract_id,
                ww.year as worked_week_year,
                ww.week as worked_week_week,
                ww.note as worked_week_note,
                ww.status as worked_week_status,
                ww.company_note as worked_week_company_note,
                
                uc.id as user_contract_id,
                uc.contract_id as user_contract_contract_id,
                uc.user_id as user_contract_user_id,
                uc.hourly_wage as user_contract_hourly_wage,
                uc.active as user_contract_active,
                
                u.id as user_id,
                u.first_name as user_first_name,
                u.last_name as user_last_name,
                u.last_name_prefix as user_last_name_prefix,
                u.email as user_email,
                u.type as user_type,
                u.kvk as user_kvk,
                u.btw as user_btw,
                u.address as user_address,
                
                c.id as contract_id,
                c.company_id as contract_company_id,
                c.role as contract_role,
                c.description as contract_description,
                
                cy.id as company_id,
                cy.name as company_name,
                cy.kvk as company_kvk,
                cy.address as company_address,
                
                w.hours,
                w.minutes
                
                FROM "%s" ww
                        
                JOIN user_contract uc ON uc.id = ww.contract_id
                JOIN "user" u ON u.id = uc.user_id
                JOIN contract c ON c.id = uc.contract_id
                JOIN company cy ON cy.id = c.company_id
                
                LEFT JOIN (select w.worked_week_id, array_agg(w.*%2$s) as hours, sum(w.minutes) as minutes FROM worked w GROUP BY w.worked_week_id) w ON w.worked_week_id = ww.id
                                
                WHERE cy.id = ? AND ww.status = 'CONFIRMED' AND (ww.year < ? OR (ww.year = ? AND ww.week < ?))
                %3$s
            """.formatted(tableName, orderByHours.getSQLOrderBy(order, true), orderBy.getSQLOrderBy(order, true));

        PreparedStatement statement = this.con.prepareStatement(query);

        PostgresJDBCHelper.setUuid(statement, 1, companyId);
        statement.setInt(2, currentYear);
        statement.setInt(3, currentYear);
        statement.setInt(4, currentWeek);

        // Execute query
        ResultSet res = statement.executeQuery();

        // Return
        List<WorkedWeekDTO> workedWeeks = new ArrayList<>();

        while (res.next()) {
            workedWeeks.add(getWorkedWeekFromRow(res, "worked_week_", withCompany, withContract, withUserContract, withUser, withHours, withTotalHours));
        }

        return workedWeeks;
    }

    /**
     * Has company access to worked week boolean.
     *
     * @param companyId    the company id
     * @param workedWeekId the worked week id
     * @return the boolean
     * @throws SQLException the sql SQLException
     */
    public boolean hasCompanyAccessToWorkedWeek(String companyId, String workedWeekId)
        throws SQLException {
        String query = """
            SELECT COUNT(DISTINCT ww.id) as count FROM "%s" ww
                        
                JOIN user_contract uc ON uc.id = ww.contract_id
                JOIN contract c ON c.id = uc.contract_id
                JOIN company cy ON cy.id = c.company_id
                
                WHERE cy.id = ? AND ww.id = ?
            """.formatted(tableName);

        PreparedStatement statement = this.con.prepareStatement(query);

        PostgresJDBCHelper.setUuid(statement, 1, companyId);
        PostgresJDBCHelper.setUuid(statement, 2, workedWeekId);

        // Execute query
        ResultSet res = statement.executeQuery();

        // Return
        res.next();
        return res.getInt("count") > 0;
    }


    /**
     * Update worked week dto.
     *
     * @param workedWeek the worked week
     * @return the worked week dto
     * @throws SQLException the sql SQLException
     */
    public WorkedWeekDTO updateWorkedWeek(WorkedWeek workedWeek) throws SQLException {
        // Create query
        String query = "UPDATE \"" + tableName +
            "\" SET note = ?, status = ? WHERE \"id\" = ? RETURNING id";

        PreparedStatement statement = this.con.prepareStatement(query);
        statement.setString(1, workedWeek.getNote());

        statement.setString(2, workedWeek.getStatus());
        PostgresJDBCHelper.setUuid(statement, 3, workedWeek.getId());

        // Execute query
        ResultSet res = statement.executeQuery();

        // None found
        if (!res.next()) {
            return null;
        }

        // Return worked week
        return getWorkedWeekById(res.getString("id"));
    }

    /**
     * Sets worked week status.
     *
     * @param workedWeekId     the worked week id
     * @param status           the status
     * @param withCompany      the with company
     * @param withContract     the with contract
     * @param withUserContract the with user contract
     * @param withUser         the with user
     * @param withHours        the with hours
     * @param withTotalHours   the with total hours
     * @param order            the order
     * @return the worked week status
     * @throws SQLException the sql SQLException
     */
    public WorkedWeekDTO setWorkedWeekStatus(String workedWeekId, String status, boolean withCompany,
                                             boolean withContract, boolean withUserContract,
                                             boolean withUser, boolean withHours, boolean withTotalHours, String order) throws SQLException {
        // Create query
        String query = """
            UPDATE "%s" ww SET status = ?
                WHERE "id" = ? RETURNING id""".formatted(tableName);

        PreparedStatement statement = this.con.prepareStatement(query);
        statement.setString(1, status);
        PostgresJDBCHelper.setUuid(statement, 2, workedWeekId);

        // Execute query
        ResultSet res = statement.executeQuery();

        // None found
        if (!res.next()) {
            return null;
        }

        // Return worked week
        return getWorkedWeekById(res.getString("id"), withCompany, withContract, withUserContract, withUser, withHours, withTotalHours, order);
    }

    private WorkedWeekDTO getWorkedWeekFromRow(ResultSet res, String prefix, boolean withCompany, boolean withContract, boolean withUserContract, boolean withUser, boolean withHours, boolean withTotalHours) throws SQLException {
        List<Worked> hours = new ArrayList<>();

        if (withHours) {
            ResultSet hoursSet = res.getArray("hours").getResultSet();

            while (hoursSet.next()) {
                String data = ((PGobject) hoursSet.getObject("VALUE")).getValue();
                if (data == null) continue;

                data = data.substring(1, data.length() - 1);
                String[] dataStrings = data.split(",(?=(?:[^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1);
                String note = dataStrings[4];
                if (note.startsWith("\"") && note.endsWith("\"")) note = note.substring(1, note.length() - 1);

                Worked worked = new Worked(dataStrings[0], dataStrings[1], Integer.parseInt(dataStrings[2]), Integer.parseInt(dataStrings[3]), note);
                worked.setSuggestion(dataStrings.length > 5 && dataStrings[5].trim().length() > 0 ? Integer.parseInt(dataStrings[5]) : null);
                hours.add(worked);
            }
        }

        WorkedWeekDTO dto = new WorkedWeekDTO(res.getString(prefix + "id"),
            res.getString(prefix + "contract_id"),
            PostgresJDBCHelper.getInteger(res, prefix + "year"),
            PostgresJDBCHelper.getInteger(res, prefix + "week"),
            res.getString(prefix + "note"),
            res.getString(prefix + "status"),
            withUser ? new UserResponseDTO(res.getString("user_id"),
                res.getString("user_email"),
                res.getString("user_first_name"),
                res.getString("user_last_name"),
                res.getString("user_last_name_prefix"),
                res.getString("user_type"), res.getString("user_kvk"), res.getString("user_btw"), res.getString("user_address")) : null,
            withCompany ? new Company(res.getString("company_id"),
                res.getString("company_name"), res.getString("company_kvk"), res.getString("company_address")) : null,
            withUserContract ? new UserContract(res.getString("user_contract_id"),
                res.getString("user_contract_contract_id"),
                res.getString("user_contract_user_id"),
                res.getInt("user_contract_hourly_wage"),
                res.getBoolean("user_contract_active")) : null,
            withContract ? new ContractDTO(res.getString("contract_id"),
                res.getString("contract_role"),
                res.getString("contract_description")) : null,
            withHours ? hours : null,
            withTotalHours ? res.getInt("minutes") : null
        );

        dto.setCompanyNote(res.getString(prefix + "company_note"));

        return dto;
    }

    /**
     * Add worked week.
     *
     * @param contractId the contract id
     * @param year       the year
     * @param week       the week
     * @throws SQLException the sql SQLException
     */
    public void addWorkedWeek(String contractId, String year, String week) throws SQLException {
        String query = "INSERT INTO \"" + tableName + "\" (contract_id, year, week) " +
            "VALUES (?, ?, ?)";
        PreparedStatement statement = this.con.prepareStatement(query);
        PostgresJDBCHelper.setUuid(statement, 1, contractId);
        statement.setInt(2, Integer.parseInt(year));
        statement.setInt(3, Integer.parseInt(week));

        statement.executeUpdate();
    }

    /**
     * Has date passed boolean.
     *
     * @param year the year
     * @param week the week
     * @return the boolean
     */
    public boolean hasDatePassed(String year, String week) {
        int y = 0;
        int w = 0;
        try {
            w = Integer.parseInt(week);
            y = Integer.parseInt(year);
        } catch (NumberFormatException e) {
            return true;
        }

        int currentWeek = LocalDate.now().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int currentYear = LocalDate.now().get(IsoFields.WEEK_BASED_YEAR);
        return currentYear > y || (currentYear == y && currentWeek > w);
    }

    /**
     * Sets company note.
     *
     * @param workedWeekId the worked week id
     * @param note         the note
     * @throws SQLException the sql SQLException
     */
    public void setCompanyNote(String workedWeekId, CreateNoteDTO note) throws SQLException {
        String query = "UPDATE \"" + tableName + "\" SET company_note = ? WHERE id = ?";
        PreparedStatement statement = this.con.prepareStatement(query);
        statement.setString(1, note.getNote());
        PostgresJDBCHelper.setUuid(statement, 2, workedWeekId);

        statement.executeUpdate();
    }

    /**
     * Is worked week confirmed boolean.
     *
     * @param workedWeekId the worked week id
     * @return the boolean
     * @throws SQLException the sql SQLException
     */
    public boolean isWorkedWeekConfirmed(String workedWeekId) throws SQLException {
        String query = "SELECT status FROM worked_week WHERE id = ?";
        PreparedStatement statement = this.con.prepareStatement(query);
        PostgresJDBCHelper.setUuid(statement, 1, workedWeekId);
        ResultSet resultSet = statement.executeQuery();
        if (!resultSet.next()) return false;
        return !resultSet.getString("status").equals("NOT_CONFIRMED");
    }

    /**
     * Has student access to worked week boolean.
     *
     * @param userId       the user id
     * @param workedWeekId the worked week id
     * @return the boolean
     * @throws SQLException the sql SQLException
     */
    public boolean hasStudentAccessToWorkedWeek(String userId, String workedWeekId) throws SQLException {
        String query = """
            SELECT COUNT(DISTINCT ww.id) as count FROM "%s" ww
                JOIN user_contract uc ON uc.id = ww.contract_id
                
                WHERE uc.user_id = ? AND ww.id = ?
            """.formatted(tableName);

        PreparedStatement statement = this.con.prepareStatement(query);

        PostgresJDBCHelper.setUuid(statement, 1, userId);
        PostgresJDBCHelper.setUuid(statement, 2, workedWeekId);

        // Execute query
        ResultSet res = statement.executeQuery();

        // Return
        res.next();
        return res.getInt("count") > 0;
    }

    /**
     * Gets worked week id by date.
     *
     * @param userContractId the user contract id
     * @param year           the year
     * @param week           the week
     * @return the worked week id by date
     * @throws SQLException the sql SQLException
     */
    public String getWorkedWeekIdByDate(String userContractId, int year, int week) throws SQLException {
        String query = """
            SELECT ww.id FROM "%s" ww
                JOIN user_contract uc ON uc.id = ww.contract_id
                
                WHERE uc.id = ? AND ww.year = ? AND ww.week = ?
            """.formatted(tableName);

        PreparedStatement statement = this.con.prepareStatement(query);

        PostgresJDBCHelper.setUuid(statement, 1, userContractId);
        statement.setInt(2, year);
        statement.setInt(3, week);

        // Execute query
        ResultSet res = statement.executeQuery();

        // Return
        if (!res.next()) return null;
        return res.getString("id");
    }

    /**
     * Is worked week suggested boolean.
     *
     * @param workedWeekId the worked week id
     * @return the boolean
     * @throws SQLException the sql SQLException
     */
    public boolean isWorkedWeekSuggested(String workedWeekId) throws SQLException {
        String query = "SELECT status FROM worked_week WHERE id = ?";
        PreparedStatement statement = this.con.prepareStatement(query);
        PostgresJDBCHelper.setUuid(statement, 1, workedWeekId);
        ResultSet resultSet = statement.executeQuery();
        if (!resultSet.next()) return false;
        return resultSet.getString("status").equals("SUGGESTED");
    }
}
