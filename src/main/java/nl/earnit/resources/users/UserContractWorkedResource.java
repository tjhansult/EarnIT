package nl.earnit.resources.users;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import nl.earnit.dao.DAOManager;
import nl.earnit.dao.WorkedDAO;
import nl.earnit.dao.WorkedWeekDAO;
import nl.earnit.dto.workedweek.WorkedWeekDTO;
import nl.earnit.models.Worked;

import java.util.List;

/**
 * The type User contract worked resource.
 */
public class UserContractWorkedResource {
    /**
     * The Uri info.
     */
    @Context
    UriInfo uriInfo;
    /**
     * The Request.
     */
    @Context
    Request request;
    private final String userId;
    private final String userContractId;
    private final String year;
    private final String week;

    // If weekId is null use year, week and user contract id
    private final String weekId;

    /**
     * Instantiates a new User contract worked resource.
     *
     * @param uriInfo        the uri info
     * @param request        the request
     * @param userId         the user id
     * @param userContractId the user contract id
     * @param year           the year
     * @param week           the week
     */
    public UserContractWorkedResource(UriInfo uriInfo, Request request, String userId, String userContractId, String year, String week) {
        this.uriInfo = uriInfo;
        this.request = request;
        this.userId = userId;
        this.userContractId = userContractId;
        this.year = year;
        this.week = week;
        this.weekId = null;
    }

    /**
     * Instantiates a new User contract worked resource.
     *
     * @param uriInfo        the uri info
     * @param request        the request
     * @param userId         the user id
     * @param userContractId the user contract id
     * @param weekId         the week id
     */
    public UserContractWorkedResource(UriInfo uriInfo, Request request, String userId, String userContractId, String weekId) {
        this.uriInfo = uriInfo;
        this.request = request;
        this.userId = userId;
        this.userContractId = userContractId;
        this.year = null;
        this.week = null;
        this.weekId = weekId;
    }

    /**
     * Gets worked week.
     *
     * @param company      the company
     * @param contract     the contract
     * @param userContract the user contract
     * @param user         the user
     * @param hours        the hours
     * @param totalHours   the total hours
     * @param order        the order
     * @return the worked week
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getWorkedWeek(@QueryParam("company") @DefaultValue("false") boolean company,
                                  @QueryParam("contract") @DefaultValue("false") boolean contract,
                                  @QueryParam("user_contract") @DefaultValue("false")
                                      boolean userContract,
                                  @QueryParam("user") @DefaultValue("false") boolean user,
                                  @QueryParam("hours") @DefaultValue("false") boolean hours,
                                  @QueryParam("totalHours") @DefaultValue("false") boolean totalHours,
                                  @QueryParam("order") @DefaultValue("hours.day:asc") String order) {
        WorkedWeekDTO workedWeek = null;
        try {
            WorkedWeekDAO workedWeekDAO = (WorkedWeekDAO) DAOManager.getInstance().getDAO(DAOManager.DAO.WORKED_WEEK);
            if (this.weekId != null) {
                workedWeek = workedWeekDAO.getWorkedWeekById(weekId, company, contract, userContract, user, hours, totalHours, order);
            } else if (this.year != null && this.week != null) {
                List<WorkedWeekDTO> workedWeeks = workedWeekDAO.getWorkedWeeksForUser(null, userContractId, Integer.parseInt(year), Integer.parseInt(week), company, contract, userContract, user, hours, totalHours, order);
                if (!workedWeeks.isEmpty()) workedWeek = workedWeeks.get(0);
            }
        } catch (Exception e) {
            return Response.serverError().build();
        }
        if (workedWeek == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(workedWeek).build();
    }

    /**
     * Update worked week task response.
     *
     * @param entry the entry
     * @return the response
     */
    @PUT
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response updateWorkedWeekTask(Worked entry) {
        WorkedDAO workedDAO;
        try {
            workedDAO = (WorkedDAO) DAOManager.getInstance().getDAO(DAOManager.DAO.WORKED);
            boolean flag = workedDAO.updateWorkedWeekTask(entry);
            if (!flag) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
        } catch (Exception e) {
            return Response.serverError().build();
        }
        return Response.ok().build();
    }

    /**
     * Add worked week task response.
     *
     * @param entry the entry
     * @return the response
     */
    @POST
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response addWorkedWeekTask(Worked entry) {
        WorkedDAO workedDAO;
        try {
            workedDAO = (WorkedDAO) DAOManager.getInstance().getDAO(DAOManager.DAO.WORKED);
            boolean flag = workedDAO.addWorkedWeekTask(entry,userContractId, year, week);
            if (!flag) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
        } catch (Exception e) {
            return Response.serverError().build();
        }
        return Response.ok().build();
    }

    /**
     * Delete worked week task response.
     *
     * @param workedId the worked id
     * @return the response
     */
    @DELETE
    @Consumes({MediaType.TEXT_PLAIN})
    public Response deleteWorkedWeekTask(String workedId) {
        WorkedDAO workedDAO;
        try {
            workedDAO = (WorkedDAO) DAOManager.getInstance().getDAO(DAOManager.DAO.WORKED);
            boolean flag = workedDAO.deleteWorkedWeekTask(workedId);
            if (!flag) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
        } catch (Exception e) {
            return Response.serverError().build();
        }
        return Response.ok().build();
    }

    /**
     * Accept worked week response.
     *
     * @param company      the company
     * @param contract     the contract
     * @param userContract the user contract
     * @param user         the user
     * @param hours        the hours
     * @param totalHours   the total hours
     * @param order        the order
     * @return the response
     */
    @POST
    @Path("/suggestions")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response acceptWorkedWeek(@QueryParam("company") @DefaultValue("false") boolean company,
                                     @QueryParam("contract") @DefaultValue("false")
                                     boolean contract,
                                     @QueryParam("userContract") @DefaultValue("false")
                                     boolean userContract,
                                     @QueryParam("user") @DefaultValue("false") boolean user,
                                     @QueryParam("hours") @DefaultValue("false") boolean hours,
                                     @QueryParam("totalHours") @DefaultValue("false") boolean totalHours,
                                     @QueryParam("order") @DefaultValue("hours.day:asc") String order) {
        String workedWeekId = getWeekId();

        try {
            WorkedWeekDAO workedWeekDAO = (WorkedWeekDAO) DAOManager.getInstance().getDAO(
                DAOManager.DAO.WORKED_WEEK);
            WorkedDAO workedDAO = (WorkedDAO) DAOManager.getInstance().getDAO(
                DAOManager.DAO.WORKED);

            if (!workedWeekDAO.isWorkedWeekSuggested(workedWeekId)) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            if (!workedDAO.acceptCompanySuggestion(workedWeekId)) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            return Response.ok(workedWeekDAO.setWorkedWeekStatus(workedWeekId, "APPROVED", company, contract, userContract, user,
                hours, totalHours, order)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Reject worked week response.
     *
     * @param company      the company
     * @param contract     the contract
     * @param userContract the user contract
     * @param user         the user
     * @param hours        the hours
     * @param totalHours   the total hours
     * @param order        the order
     * @return the response
     */
    @DELETE
    @Path("/suggestions")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response rejectWorkedWeek(@QueryParam("company") @DefaultValue("false") boolean company,
                                     @QueryParam("contract") @DefaultValue("false")
                                     boolean contract,
                                     @QueryParam("userContract") @DefaultValue("false")
                                     boolean userContract,
                                     @QueryParam("user") @DefaultValue("false") boolean user,
                                     @QueryParam("hours") @DefaultValue("false") boolean hours,
                                     @QueryParam("totalHours") @DefaultValue("false") boolean totalHours,
                                     @QueryParam("order") @DefaultValue("hours.day:asc") String order) {
        String workedWeekId = getWeekId();

        try {
            WorkedWeekDAO workedWeekDAO = (WorkedWeekDAO) DAOManager.getInstance().getDAO(
                DAOManager.DAO.WORKED_WEEK);

            if (!workedWeekDAO.isWorkedWeekSuggested(workedWeekId)) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            return Response.ok(workedWeekDAO.setWorkedWeekStatus(workedWeekId, "SUGGESTION_DENIED", company, contract, userContract, user,
                hours, totalHours, order)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Confirm worked week response.
     *
     * @return the response
     */
    @POST
    @Path("/confirm")
    public Response confirmWorkedWeek() {
        WorkedWeekDAO workedWeekDAO;
        try {
            workedWeekDAO = (WorkedWeekDAO) DAOManager.getInstance().getDAO(DAOManager.DAO.WORKED_WEEK);
            workedWeekDAO.confirmWorkedWeek(userContractId, year, week);
        }catch (Exception e) {
            return Response.serverError().build();
        }
        return Response.ok().build();
    }


    /**
     * Remove confirm worked week response.
     *
     * @return the response
     */
    @DELETE
    @Path("/confirm")
    public Response removeConfirmWorkedWeek() {
        WorkedWeekDAO workedWeekDAO;
        try {
            workedWeekDAO = (WorkedWeekDAO) DAOManager.getInstance().getDAO(DAOManager.DAO.WORKED_WEEK);

            if (!workedWeekDAO.removeConfirmWorkedWeek(userContractId, year, week)) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            return Response.ok().build();
        }catch (Exception e) {
            return Response.serverError().build();
        }
    }

    /**
     * Add worked week note response.
     *
     * @param note the note
     * @return the response
     */
    @PUT
    @Path("/note")
    @Consumes({MediaType.TEXT_PLAIN})
    public Response addWorkedWeekNote(String note) {
        boolean success;
        WorkedWeekDAO workedWeekDAO;
        try {
            workedWeekDAO = (WorkedWeekDAO) DAOManager.getInstance().getDAO(DAOManager.DAO.WORKED_WEEK);
            success = workedWeekDAO.addWorkedWeekNote(note, userContractId, year, week);
        }catch (Exception e) {
            return Response.serverError().build();
        }
        if (success) {
            return Response.ok().build();
        }
        return Response.status(Response.Status.fromStatusCode(406)).build();
    }

    /**
     * Gets the worked week number from either id or year/week and checks if the user has access to it.
     *
     * @return Worked week id
     */
    public String getWeekId() {
        String workedWeekId = weekId;

        try {
            WorkedWeekDAO workedWeekDAO = (WorkedWeekDAO) DAOManager.getInstance().getDAO(DAOManager.DAO.WORKED_WEEK);

            if (workedWeekId == null && year != null && week != null) {
                workedWeekId = workedWeekDAO.getWorkedWeekIdByDate(userContractId, Integer.parseInt(year), Integer.parseInt(week));
            }

            if (workedWeekId == null) {
                throw new ServerErrorException(Response.Status.NOT_FOUND);
            }

            if (!workedWeekDAO.hasStudentAccessToWorkedWeek(userId, workedWeekId)) {
                throw new ServerErrorException(Response.Status.FORBIDDEN);
            }

            return workedWeekId;
        } catch (ServerErrorException e) {
            throw e;
        } catch (Exception e) {
            throw new ServerErrorException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
