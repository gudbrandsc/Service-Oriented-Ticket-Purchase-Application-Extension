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
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Gudbrand Schistad
 * Servlet class that is used to respond to election requests, and updated master.
 * If a master is updated it will process the request data, and set its on data accordingly
 */
public class ElectionServlet extends HttpServlet {
    private  NodeInfo nodeInfo;
    private ServiceHelper serviceHelper;
    private SecondariesMemberData secondariesMemberData;
    private  UserDataMap userDataMap;
    private  AtomicInteger version;
    private  AtomicInteger userID;
    private  static Logger log = LogManager.getLogger();

    /** Constructor */
    public ElectionServlet(NodeInfo nodeInfo, SecondariesMemberData secondariesMemberData,
                           UserDataMap userDataMap, AtomicInteger version, AtomicInteger userID) {
        this.nodeInfo = nodeInfo;
        this.serviceHelper = new ServiceHelper();
        this.secondariesMemberData = secondariesMemberData;
        this.userDataMap = userDataMap;
        this.version = version;
        this.userID = userID;
    }

    /**
     * Responds to election requests from lower valued processes
     * @param req HttpServletRequest
     * @param resp HttpServletResponse
     */
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.info("[S] Received a election request from lower value node");
        resp.setStatus(HttpStatus.OK_200);

    }


    /**
     * Method that updates the master when an election is done.
     * Processes the request data and sets it data accordingly.
     * @param resp HttpServletResponse
     * @param req HttpServletRequest
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        JSONObject requestBody = serviceHelper.stringToJsonObject(serviceHelper.requestToString(req));
        String host = requestBody.get("host").toString();
        int port = Integer.valueOf(requestBody.get("port").toString());
        JSONArray newUserData = (JSONArray) requestBody.get("userdata");

        log.info("[S] Updated master to " + host + ":" + port);
        nodeInfo.updateMaster(host, port);

        log.info("[S] Removed master from list of secondaries");
        secondariesMemberData.RemoveNode(host, port);

        int versionNumber = Integer.parseInt(requestBody.get("version").toString());
        version.set(versionNumber);
        log.info("[S] Updated to version: " + version.intValue());

        int masterUserID = Integer.parseInt(requestBody.get("userID").toString());
        userID.set(masterUserID);
        log.info("[S] Updated userID to : " + userID.intValue());

        updateUserData(newUserData);
        resp.setStatus(HttpStatus.OK_200);
    }

    /**
     * Method used when updating to a new master. Processes the data received from the request and updates its userdata.
     * @param userdata userdata extracted from the request body
     */
    private void updateUserData(JSONArray userdata){
        ConcurrentHashMap<Integer, User> updatedUserData = new ConcurrentHashMap<>();
        for (JSONObject obj : (Iterable<JSONObject>) userdata) {
            String username = obj.get("username").toString();
            int objUserID = Integer.valueOf(obj.get("userid").toString());
            User newUser = new User(objUserID, username);
            updatedUserData.put(objUserID, newUser);
            JSONArray ticketList = (JSONArray) obj.get("tickets");
            newUser.updateTicketArray(ticketList);
        }
        log.info("[S] Updated userdata map to master version");
        userDataMap.updateuserDataMap(updatedUserData);
    }
}
