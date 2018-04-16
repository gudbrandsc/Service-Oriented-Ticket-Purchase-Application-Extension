import org.eclipse.jetty.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
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

    //TODO refactor post
    /** Constructor */
    public UserServiceServlet(UserDataMap userDataMap, int userid, UserServiceNodeData userServiceNodeData, NodeInfo nodeInfo, PropertiesLoader eventService) {
        this.userDataMap = userDataMap;
        this.userid = userid;
        this.servletHelper = new ServletHelper();
        this.userServiceNodeData = userServiceNodeData;
        this.nodeInfo = nodeInfo;
        this.eventService = eventService;

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
            int userId = Integer.valueOf(pathValue);
            if (userDataMap.checkIfUserExist(userId)) {
                User user = userDataMap.getUser(userId);
                String username = user.getUsername();
                JSONObject json = new JSONObject();
                JSONArray eventarray = new JSONArray();
                json.put("userid", userId);
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
                resp.setContentType("application/json; charset=UTF-8");
                out.println(json);
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

        if (matchAdd.matches()) {
            addTicketsHandler(resp, requestBody, Integer.valueOf(matchAdd.group(1)));
        }else if(matchTransfer.matches()){
            transfereTicketsHandler(resp, requestBody, Integer.valueOf(matchTransfer.group(1)));
        }else if(req.getRequestURI().equals("/create")) {
            createUserHandler(resp, requestBody, printWriter);
        }else{
            resp.setStatus(HttpStatus.BAD_REQUEST_400);
        }
    }

    private void createUserHandler(HttpServletResponse resp, JSONObject requestBody,  PrintWriter printWriter){
        if(requestBody.containsKey("username")){
            String username = requestBody.get("username").toString();
            if(!(username.isEmpty())){
                User user = new User(userid, username);
                addUser(userid, user);
                String path = "/create";
                if(nodeInfo.isMaster()) {
                    resp.setStatus(updatedAllSlaves(resp, path, requestBody.toJSONString()));
                }else {
                    resp.setStatus(HttpStatus.OK_200);
                }
                //TODO if 400 then remove slave
                JSONObject respJSON = new JSONObject();
                respJSON.put("userid", userid);
                printWriter.println(respJSON.toString());
                printWriter.flush();
                userid++;
            }else{
                resp.setStatus(HttpStatus.BAD_REQUEST_400);

            }
        } else{
            resp.setStatus(HttpStatus.BAD_REQUEST_400);
        }
    }

    private void addTicketsHandler(HttpServletResponse resp,JSONObject requestBody, int userId){
        if(requestBody.containsKey("eventid") && requestBody.containsKey("tickets")){
            int eventid = Integer.parseInt(requestBody.get("eventid").toString());
            int tickets = Integer.parseInt(requestBody.get("tickets").toString());
            if(userDataMap.checkIfUserExist(userId)) {
                JSONObject eventRequestBody = new JSONObject();
                eventRequestBody.put("userid", userId);
                eventRequestBody.put("eventid", eventid);
                eventRequestBody.put("tickets", tickets);
                String purchaseEventPath = "/purchase/" + eventid;
                System.out.println("Port:" + eventService.getEventport());
                int eventPort = Integer.valueOf(eventService.getEventport());
                System.out.println(eventRequestBody.toString());
                if(servletHelper.sendPostRequest(eventService.getEventhost(), eventPort, purchaseEventPath, eventRequestBody.toJSONString()) == 200){
                    userDataMap.getUser(userId).addTickets(eventid, tickets);
                    String path = "/" + userId + "/tickets/add" ;
                    resp.setStatus(updatedAllSlaves(resp, path, requestBody.toJSONString()));
                }else{
                    resp.setStatus(HttpStatus.BAD_REQUEST_400);
                }
            }else{
                resp.setStatus(HttpStatus.BAD_REQUEST_400);
            }
        }else{
            resp.setStatus(HttpStatus.BAD_REQUEST_400);
        }
    }

    private void transfereTicketsHandler(HttpServletResponse resp,JSONObject requestBody, int userId){
        if(requestBody.containsKey("eventid") && requestBody.containsKey("tickets") && requestBody.containsKey("targetuser")){
            int eventid = Integer.parseInt(requestBody.get("eventid").toString());
            int tickets = Integer.parseInt(requestBody.get("tickets").toString());
            int targetuser = Integer.parseInt(requestBody.get("targetuser").toString());
            if(userDataMap.checkIfUserExist(targetuser) && userDataMap.checkIfUserExist(userId)) {
                System.out.println("found user");
                if (transferTickets(eventid, userId, targetuser, tickets)) {
                    System.out.println("Transfere tickets success");
                    String path = " /" + userId + "/tickets/transfer";
                    resp.setStatus(updatedAllSlaves(resp, path, requestBody.toJSONString()));
                } else {
                    resp.setStatus(HttpStatus.BAD_REQUEST_400);
                    System.out.println("Fail transfere");

                }
            }else {
                System.out.println("User nor exist");
                resp.setStatus(HttpStatus.BAD_REQUEST_400);
            }
        }else {
            System.out.println("requbody");

            resp.setStatus(HttpStatus.BAD_REQUEST_400);
        }
    }



    private int updatedAllSlaves(HttpServletResponse resp, String path, String body){
        int countSuccess = 0;
        for(NodeInfo slave : userServiceNodeData.getUserServicesListCopy()){
           int status = servletHelper.sendPostRequest(slave.getHost(),slave.getPort(), path, body);
           if(status == 200){
               countSuccess++;
           }else {
               //Remove node
           }
        }
        if (countSuccess == userServiceNodeData.getUserServicesListCopy().size()){
            return HttpStatus.OK_200;
        }
        return HttpStatus.BAD_REQUEST_400;
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

    private synchronized void addUser(int userid, User user){
        userDataMap.addUser(userid, user);

    }
}
