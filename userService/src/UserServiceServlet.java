import org.eclipse.jetty.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Gudbrand Schistad
 * Servlet class that handles all get and post requests.
 */
public class UserServiceServlet extends HttpServlet {
    private UserDataMap userDataMap;
    private AtomicInteger userID;
    private ServiceHelper serviceHelper;
    private SecondariesMemberData secondariesMemberData;
    private NodeInfo nodeInfo;
    private PropertiesLoader eventService;
    private AtomicInteger version;
    private  static Logger log = LogManager.getLogger();


    /** Constructor */
    public UserServiceServlet(UserDataMap userDataMap, AtomicInteger userID, SecondariesMemberData secondariesMemberData, NodeInfo nodeInfo, PropertiesLoader eventService, AtomicInteger version) {
        this.userDataMap = userDataMap;
        this.userID = userID;
        this.serviceHelper = new ServiceHelper();
        this.secondariesMemberData = secondariesMemberData;
        this.nodeInfo = nodeInfo;
        this.eventService = eventService;
        this.version = version;
    }

    /**
     * Do get method that handles all incoming get requests.
     * @param req incoming request
     * @param resp servlet Http response object
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter out = resp.getWriter();
        String isInt = "[0-9]+";
        String pathValue = req.getPathInfo().substring(1);
        Pattern pattern = Pattern.compile(isInt);
        if(pattern.matcher(pathValue).matches()) {
            int requestUserId = Integer.valueOf(pathValue);
            if (userDataMap.checkIfUserExist(requestUserId)) {
                JSONObject userObject = buildUserObject(requestUserId);
                resp.setContentType("application/json; charset=UTF-8");
                out.println(userObject);
                out.flush();
                resp.setStatus(HttpStatus.OK_200);
            } else {
                resp.setStatus(HttpStatus.BAD_REQUEST_400);
            }
        }else{
            resp.setStatus(HttpStatus.BAD_REQUEST_400);
        }
    }

    /**
     * Do post method that handles all incoming post requests.
     * If request do not match the any pattern set response status  400
     * @param req incoming request
     * @param resp servlet Http response object
     * @throws IOException
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        PrintWriter printWriter = resp.getWriter();
        String addTicketPattern = "\\/([0-9]*)\\/tickets\\/add";
        String transferTicketPattern = "\\/([0-9]*)\\/tickets\\/transfer";
        String uri = req.getRequestURI();
        Pattern add = Pattern.compile(addTicketPattern);
        Pattern transfer = Pattern.compile(transferTicketPattern);
        Matcher matchAdd = add.matcher(uri);
        Matcher matchTransfer = transfer.matcher(uri);
        JSONObject requestBody = serviceHelper.stringToJsonObject(serviceHelper.requestToString(req));
        try {
            if (matchAdd.matches()) {
                addTicketsRequest(resp, requestBody, Integer.valueOf(matchAdd.group(1)));
            }else if(matchTransfer.matches()){
                transfereTicketsRequest(resp, requestBody, Integer.valueOf(matchTransfer.group(1)));
            }else if(uri.equals("/create")) {
                createUserRequest(resp, requestBody, printWriter);
            }else{
                resp.setStatus(HttpStatus.NOT_FOUND_404);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method that gets all information about a user by it's user ID.
     * @param requestUserId Id of the user to get information about
     * @return a JSONObject with all information about the user
     */
    private JSONObject buildUserObject(int requestUserId){
        User user = userDataMap.getUser(requestUserId);
        String username = user.getUsername();
        JSONObject json = new JSONObject();
        JSONArray eventarray = new JSONArray();
        json.put("userid", requestUserId);
        json.put("username", username);
        if (user.getNumEventsSize() > 0) {
            Iterator<JSONObject> it = user.getEvents().iterator();
            while(it.hasNext()) {
                JSONObject obj = it.next();
                JSONObject item = new JSONObject();
                item.put("eventid", obj.get("eventid").toString());
                eventarray.add(item);
            }
        }
        json.put("tickets", eventarray);
        return json;
    }

    /**
     * Method used to distinguish create requests for master node and secondary nodes.
     * @param resp request response
     * @param requestBody The JSON object from the POST request
     * @param printWriter response printWriter
     */
    private void createUserRequest(HttpServletResponse resp, JSONObject requestBody, PrintWriter printWriter) throws InterruptedException {
        if(nodeInfo.isMaster()) {
            JSONObject respJSON = new JSONObject();
            resp.setStatus(createUserAsMaster(requestBody, respJSON));
            printWriter.println(respJSON.toString());
            printWriter.flush();
        }else{
            resp.setStatus(createUserAsSecondary(requestBody));
        }
    }

    /**
     * Creates a user as a master. Assigns a version number to the request, end replicates it to all secondaries.
     * Returns response status once replication is done
     * @param requestBody body of the request
     * @param respJSON Json used to return assigned userID to the frontend.
     * @return Http status code
     */
    private int createUserAsMaster(JSONObject requestBody, JSONObject respJSON){
        int setUserId = userID.getAndIncrement();
        User user = new User(setUserId, requestBody.get("username").toString());

        synchronized (version) {
            userDataMap.addUser(setUserId, user);
            requestBody.put("version", version.getAndIncrement());
        }

        log.info("[P] Adding new user with user id: " + setUserId);
        String path = "/create";
        respJSON.put("userid", setUserId);

        return serviceHelper.replicateToSecondaries(path, requestBody.toJSONString(), secondariesMemberData);
    }

    /**
     * Creates a user as a secondary.
     * If the version number from the request does not match the expected version then wait.
     * If the version number from the request matches the expected version, then process request.
     * Then update all waiting threads.
     * Returns response status once replication is done
     * @param requestBody body of the request
     * @return Http status code
     */
    private int createUserAsSecondary(JSONObject requestBody) throws InterruptedException {
        synchronized (version) {
            log.info("[S] Received request version " + requestBody.get("version").toString() + " expected version " + version.intValue());

            while(Integer.valueOf(requestBody.get("version").toString()) != version.intValue()){
                log.info("[S] Request --> Waiting...");
                version.wait();
            }

            int setUserId = userID.getAndIncrement();
            version.getAndIncrement();
            User user = new User(setUserId, requestBody.get("username").toString());
            userDataMap.addUser(setUserId, user);
            version.notifyAll();
            log.info("[S] Adding new user with user id: " + setUserId);
            return HttpStatus.OK_200;
        }
    }

    /**
     * Method used to distinguish create requests for master node and secondary nodes.
     * Also checks that the requested user ID exist in the data structure.
     * @param resp request response
     * @param requestBody The JSON object from the POST request
     */
    private void addTicketsRequest(HttpServletResponse resp, JSONObject requestBody, int requestUserId) throws InterruptedException {
        int eventid = Integer.parseInt(requestBody.get("eventid").toString());
        int tickets = Integer.parseInt(requestBody.get("tickets").toString());
        if(userDataMap.checkIfUserExist(requestUserId)) {
            if(nodeInfo.isMaster()) {
                resp.setStatus(addTicketsAsMaster(requestUserId, eventid, tickets, requestBody));
            }else {
                addTicketsAsSecondary(requestUserId, eventid, tickets, requestBody);
            }
        }else{
            resp.setStatus(HttpStatus.BAD_REQUEST_400);
        }
    }

    /**
     * Method that adds tickets a a master node.
     * Send's a POST request to the event service to purchase tickets.
     * If response 200 then add tickets to data structure and replicate data in all secondaries.
     * else return 400
     * @param requestUserId ID of the user that want to purchase tickets.
     * @param eventid ID of the event
     * @param tickets Number of tickets to purchase
     * @param requestBody JSON of the incoming request */
    private int addTicketsAsMaster (int requestUserId, int eventid, int tickets, JSONObject requestBody){
        //Build Json for post request to event service
        JSONObject eventRequestBody = new JSONObject();
        eventRequestBody.put("userid", requestUserId);
        eventRequestBody.put("eventid", eventid);
        eventRequestBody.put("tickets", tickets);
        String purchaseEventPath = "/purchase/" + eventid;
        //If the event service returns 200, then add tickets to data structure and replicate.
        if(serviceHelper.sendPostRequest(eventService.getEventhost(), Integer.valueOf(eventService.getEventport()), purchaseEventPath, eventRequestBody.toJSONString()) == 200){
            log.info("[P] Adding " + tickets + " tickets for eventID " + eventid + " to user with userID " + requestUserId);
            String path = "/" + requestUserId + "/tickets/add" ;
            synchronized (version) {
                requestBody.put("version", version.getAndIncrement());
                userDataMap.getUser(requestUserId).addTickets(eventid, tickets);
            }

            return serviceHelper.replicateToSecondaries(path, requestBody.toJSONString(), secondariesMemberData);
        }else {
            return HttpStatus.BAD_REQUEST_400;
        }
    }

    /**
     * Method that adds tickets as a secondary.
     * If request version number is not equal expected version -> wait
     * else add tickets to user increment version and notify
     * @param requestUserId ID of the user that want to purchase tickets.
     * @param eventid ID of the event
     * @param tickets Number of tickets to purchase
     * @param requestBody JSON of the incoming request */
    private int addTicketsAsSecondary (int requestUserId, int eventid, int tickets, JSONObject requestBody) throws InterruptedException {
        log.info("[S] Received request version " + requestBody.get("version").toString() + " expected version " + version.intValue());
        synchronized (version) {

            while (Integer.valueOf(requestBody.get("version").toString()) != version.intValue()) {
                version.wait();
            }

            log.info("[S] Adding " + tickets + " tickets for eventID " + eventid + " to user with userID " + requestUserId);
            userDataMap.getUser(requestUserId).addTickets(eventid, tickets);
            version.getAndIncrement();
            version.notifyAll();
            return HttpStatus.OK_200;
        }
    }

    /**
     * Method used to distinguish transfer ticket requests for master node and secondary nodes.
     * @param resp request response
     * @param requestBody The JSON object from the POST request
     */
    private void transfereTicketsRequest(HttpServletResponse resp, JSONObject requestBody, int requestUserId) throws InterruptedException {
        int eventid = Integer.parseInt(requestBody.get("eventid").toString());
        int tickets = Integer.parseInt(requestBody.get("tickets").toString());
        int targetuser = Integer.parseInt(requestBody.get("targetuser").toString());
        if(nodeInfo.isMaster()) {
            resp.setStatus(transferAsMaster(requestBody, requestUserId, targetuser, eventid, tickets));
        }else {
            resp.setStatus(transfereAsSecondary(requestBody, requestUserId, targetuser, eventid, tickets));
        }
    }

    /**
     * Transfers tickets between two users, if the transfer is a success, then the master will replicate the write operation to
     * all secondaries.
     * @param requestBody json from the post request
     * @param requestUserId id of the user to transfer tickets from
     * @param targetuser Id of the user to transfer tickets to
     * @param eventid id of the event
     * @param tickets number of tickets to transfer
     */
    private int transferAsMaster(JSONObject requestBody, int requestUserId, int targetuser, int eventid, int tickets) {
        if (userDataMap.checkIfUserExist(targetuser) && userDataMap.checkIfUserExist(requestUserId)) {
            synchronized (version){
                if (transferTickets(eventid, requestUserId, targetuser, tickets)) {
                    log.info("[P] Transferring  " + tickets + " tickets from userID " + requestUserId + " to userID: " + targetuser);
                    String path = "/" + requestUserId + "/tickets/transfer";
                    requestBody.put("version", version.getAndIncrement());
                    return serviceHelper.replicateToSecondaries(path, requestBody.toJSONString(), secondariesMemberData);
                } else {
                    return HttpStatus.BAD_REQUEST_400;
                }
            }
        } else {
            return HttpStatus.BAD_REQUEST_400;
        }
    }

    /**
     * Transfers tickets between two users, waits until the request version matches the expected version.
     * @param requestBody json from the post request
     * @param requestUserId id of the user to transfer tickets from
     * @param targetuser Id of the user to transfer tickets to
     * @param eventid id of the event
     * @param tickets number of tickets to transfer
     */
    private int transfereAsSecondary (JSONObject requestBody, int requestUserId, int targetuser, int eventid, int tickets) throws InterruptedException {
        log.info("[S] Received request version " + requestBody.get("version").toString() + " expected version " + version.intValue());
        synchronized (version) {
            while (Integer.valueOf(requestBody.get("version").toString()) != version.intValue()) {
                version.wait();
            }

            log.info("[S] Transferring  " + tickets + " tickets from userId " + requestUserId + " to userId: " + targetuser);
            transferTickets(eventid, requestUserId, targetuser, tickets);
            version.getAndIncrement();
            version.notifyAll();
            return 200;
        }
    }



    /** Synchronized method that transfers tickets between to users.
     * @param eventId Id of the events to transfer from
     * @param userId Id of the user to transfer tickets from
     * @param targetUser Id of the user to transfer tickets to
     * @param numTickets Number of tickets to transfer
     */
    private synchronized boolean transferTickets(int eventId, int userId, int targetUser, int numTickets){
        if (userDataMap.getUser(userId).validateNumTickets(eventId, numTickets)) {
            userDataMap.getUser(userId).removeTickets(eventId, numTickets);
            userDataMap.getUser(targetUser).addTickets(eventId, numTickets);
            return true;
        }
        return false;
    }

}
