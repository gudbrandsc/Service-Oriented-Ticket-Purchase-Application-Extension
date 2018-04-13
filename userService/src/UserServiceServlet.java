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
    private PropertiesLoader properties;
    private ServletHelper servletHelper;

    //TODO refactor post
    /** Constructor */
    public UserServiceServlet(UserDataMap userDataMap, int userid, PropertiesLoader properties) {
        this.userDataMap = userDataMap;
        this.userid = userid;
        this.properties = properties;
        this.servletHelper = new ServletHelper();

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
            if(requestBody.containsKey("eventid") && requestBody.containsKey("tickets")){
                int userId = Integer.parseInt(matchAdd.group(1));
                int eventid = Integer.parseInt(requestBody.get("eventid").toString());
                int tickets = Integer.parseInt(requestBody.get("tickets").toString());
                if(userDataMap.checkIfUserExist(userId)) {
                    userDataMap.getUser(userId).addTickets(eventid, tickets);
                    resp.setStatus(HttpStatus.OK_200);
                }else{
                    resp.setStatus(HttpStatus.BAD_REQUEST_400);
                }
            }else{
                resp.setStatus(HttpStatus.BAD_REQUEST_400);
            }
        }else if(matchTransfer.matches()){
            if(requestBody.containsKey("eventid") && requestBody.containsKey("tickets") && requestBody.containsKey("targetuser")){
                int userId = Integer.parseInt(matchTransfer.group(1));
                int eventid = Integer.parseInt(requestBody.get("eventid").toString());
                int tickets = Integer.parseInt(requestBody.get("tickets").toString());
                int targetuser = Integer.parseInt(requestBody.get("targetuser").toString());
                if(userDataMap.checkIfUserExist(targetuser) && userDataMap.checkIfUserExist(userId)) {
                    if (transferTickets(eventid, userId, targetuser, tickets)) {
                        resp.setStatus(HttpStatus.OK_200);
                    } else {
                        resp.setStatus(HttpStatus.BAD_REQUEST_400);
                    }
                }else {
                    resp.setStatus(HttpStatus.BAD_REQUEST_400);
                }
            }else {
                resp.setStatus(HttpStatus.BAD_REQUEST_400);
            }
        }else if(req.getRequestURI().equals("/create")) {
            if(requestBody.containsKey("username")){
                String username = requestBody.get("username").toString();
                if(!(username.isEmpty())){
                    User user = new User(userid, username);
                    userDataMap.addUser(userid, user);
                    resp.setStatus(HttpStatus.OK_200);
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
        }else{
            resp.setStatus(HttpStatus.BAD_REQUEST_400);
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
