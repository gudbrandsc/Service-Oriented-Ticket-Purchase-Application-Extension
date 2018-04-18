import org.eclipse.jetty.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author Gudbrand Schistad
 * Servlet class that handles all get and post requests.
 */
public class UserServiceServlet extends HttpServlet{
    private UserDataMap userDataMap;
    private static volatile int userid;
    private ServletHelper servletHelper;
    private UserServiceNodeData userServiceNodeData;
    private NodeInfo nodeInfo;
    private PropertiesLoader eventService;
    private AtomicInteger version;


    //TODO refactor post
    /** Constructor */
    public UserServiceServlet(UserDataMap userDataMap, int userid, UserServiceNodeData userServiceNodeData, NodeInfo nodeInfo, PropertiesLoader eventService, AtomicInteger version) {
        this.userDataMap = userDataMap;
        this.userid = userid;
        this.servletHelper = new ServletHelper();
        this.userServiceNodeData = userServiceNodeData;
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
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

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
        JSONObject requestBody = servletHelper.stringToJsonObject(servletHelper.requestToString(req));
        try {
            if (matchAdd.matches()) {
                addTicketsRequest(resp, requestBody, Integer.valueOf(matchAdd.group(1)));
            }else if(matchTransfer.matches()){
                transfereTicketsRequest(resp, requestBody, Integer.valueOf(matchTransfer.group(1)));
            }else if(uri.equals("/create")) {
                createUserRequest(resp, requestBody, printWriter);
            }else{
                resp.setStatus(HttpStatus.BAD_REQUEST_400);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method that gets all information about a user by its userid.
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

    private void createUserRequest(HttpServletResponse resp, JSONObject requestBody, PrintWriter printWriter) throws InterruptedException {
        String username = requestBody.get("username").toString();
        if(!(username.isEmpty())){
            User user = new User(userid, username);
            if(nodeInfo.isMaster()) {
                addUser(userid, user);
                requestBody.put("version", version.getAndIncrement());
                System.out.println("[P] Adding new user with userid: " + userid);
                String path = "/create";
                updatedAllSlaves(path, requestBody.toJSONString());
            }else{
                int expectedVersion = version.getAndIncrement();
                System.out.println("Got Version : " + Integer.valueOf(requestBody.get("version").toString()) );
                while(Integer.valueOf(requestBody.get("version").toString()) != expectedVersion){
                    synchronized (version) {
                        version.wait();
                    }
                }
                addUser(userid, user);
                System.out.println("Expecting version: " + version.intValue());
                synchronized (version) {
                    version.notify();
                }
                System.out.println("[S] Adding new user with userid: " + userid);
            }
            resp.setStatus(HttpStatus.OK_200);
            JSONObject respJSON = new JSONObject();
            respJSON.put("userid", userid);
            printWriter.println(respJSON.toString());
            printWriter.flush();
            userid++;
            //TODO change to make random value that is not in the dataset yet
        }else{
            resp.setStatus(HttpStatus.BAD_REQUEST_400);
        }
    }

    private void addTicketsRequest(HttpServletResponse resp, JSONObject requestBody, int requestUserId){
        int eventid = Integer.parseInt(requestBody.get("eventid").toString());
        int tickets = Integer.parseInt(requestBody.get("tickets").toString());
        if(userDataMap.checkIfUserExist(requestUserId)) {
            if(nodeInfo.isMaster()) {
                resp.setStatus(addTicketsAsMaster(requestUserId, eventid, tickets, requestBody));
            }else{
                System.out.println("[S] Adding " + tickets + " to event with id " + eventid + " for user with user id " + requestUserId);
                userDataMap.getUser(requestUserId).addTickets(eventid, tickets);
                resp.setStatus(HttpStatus.OK_200);
            }
        }else{
            resp.setStatus(HttpStatus.BAD_REQUEST_400);
        }

    }

    private void transfereTicketsRequest(HttpServletResponse resp, JSONObject requestBody, int requestUserId){
        int eventid = Integer.parseInt(requestBody.get("eventid").toString());
        int tickets = Integer.parseInt(requestBody.get("tickets").toString());
        int targetuser = Integer.parseInt(requestBody.get("targetuser").toString());
        if(nodeInfo.isMaster()) {
            if (userDataMap.checkIfUserExist(targetuser) && userDataMap.checkIfUserExist(requestUserId)) {
                if (transferTickets(eventid, requestUserId, targetuser, tickets)) {
                    System.out.println("[P] Transferring tickets " + tickets + " from user with id "+ requestUserId + " to user with id: " + targetuser);
                    String path = "/" + requestUserId + "/tickets/transfer";
                    updatedAllSlaves(path, requestBody.toJSONString());
                    resp.setStatus(HttpStatus.OK_200);
                } else {
                    resp.setStatus(HttpStatus.BAD_REQUEST_400);
                }
            } else {
                resp.setStatus(HttpStatus.BAD_REQUEST_400);
            }
        }else {
            System.out.println("[S] Transferring tickets " + tickets + " from user with id "+ requestUserId + " to user with id: " + targetuser);
            transferTickets(eventid, requestUserId, targetuser,tickets);
        }

    }

    private int addTicketsAsMaster (int requestUserId, int eventid, int tickets, JSONObject requestBody){
        JSONObject eventRequestBody = new JSONObject();
        eventRequestBody.put("userid", requestUserId);
        eventRequestBody.put("eventid", eventid);
        eventRequestBody.put("tickets", tickets);
        String purchaseEventPath = "/purchase/" + eventid;
        if(servletHelper.sendPostRequest(eventService.getEventhost(), Integer.valueOf(eventService.getEventport()), purchaseEventPath, eventRequestBody.toJSONString()) == 200){
            userDataMap.getUser(requestUserId).addTickets(eventid, tickets);
            System.out.println("[P] Adding " + tickets + " to event with id " + eventid + " for user with user id " + requestUserId);
            String path = "/" + requestUserId + "/tickets/add" ;
            updatedAllSlaves(path, requestBody.toJSONString());
            return HttpStatus.OK_200;
        }else {
            return HttpStatus.BAD_REQUEST_400;
        }
    }

    private void updatedAllSlaves(String path, String body){
        System.out.println("[P] Replicating write request to all secondaries");
        List<NodeInfo> secondaryCopy = userServiceNodeData.getUserServicesListCopy();
        for(NodeInfo secondary : secondaryCopy){
            int status = servletHelper.sendPostRequest(secondary.getHost(), secondary.getPort(), path, body);
            if(status != 200){
                System.out.println("[P] Unable to replicate data to " + secondary.getHost() + ":" + secondary.getPort());
                String removeNodePath = "/remove/userservice";
                removeDeadNode(secondary, secondaryCopy, removeNodePath);
            }
        }
    }

    //TODO change slave to secondary
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

    private synchronized void addUser(int userid, User user){
        userDataMap.addUser(userid, user);

    }
    private void removeDeadNode(NodeInfo rmNode, List<NodeInfo> secondaries, String path){
        JSONObject obj = new JSONObject();
        obj.put("host", rmNode.getHost());
        obj.put("port", rmNode.getPort());
        servletHelper.sendPostRequest(nodeInfo.getHost(),nodeInfo.getPort(), path, obj.toString());
        for(NodeInfo node : secondaries){
            servletHelper.sendPostRequest(node.getHost(),node.getPort(), path, obj.toString());
        }
    }
}
