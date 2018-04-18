import org.eclipse.jetty.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ElectionServlet extends HttpServlet {
    private  NodeInfo nodeInfo;
    private  NodeElector nodeElector;
    private  ServletHelper servletHelper;
    private  UserServiceNodeData userServiceNodeData;
    private  UserDataMap userDataMap;
    private  AtomicInteger version;

    public ElectionServlet(NodeInfo nodeInfo, NodeElector nodeElector, UserServiceNodeData userServiceNodeData, UserDataMap userDataMap, AtomicInteger version) {
        this.nodeInfo = nodeInfo;
        this.nodeElector = nodeElector;
        this.servletHelper = new ServletHelper();
        this.userServiceNodeData = userServiceNodeData;
        this.userDataMap = userDataMap;
        this.version = version;
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("[S] Received election request from lower value node");
        resp.setStatus(HttpStatus.OK_200);

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        JSONObject requestBody = servletHelper.stringToJsonObject(servletHelper.requestToString(req));
        String host = requestBody.get("host").toString();
        int port = Integer.valueOf(requestBody.get("port").toString());
        JSONArray newUserData = (JSONArray) requestBody.get("userdata");
        System.out.println("[S] Updating master to " + host + ":" + port);
        nodeInfo.updateMaster(host, port);
        System.out.println("[S] Removing master from list of secondaries");
        userServiceNodeData.RemoveNode(host, port);
        System.out.println("[S] Updating to version: " + version.intValue());
        int versionNumber = Integer.parseInt(requestBody.get("version").toString());
        version.set(versionNumber);
        updateUserData(newUserData);
        resp.setStatus(HttpStatus.OK_200);
    }

    private void updateUserData(JSONArray userdata){
        ConcurrentHashMap<Integer, User> updatedUserData = new ConcurrentHashMap<Integer, User>();
        Iterator<JSONObject> it = userdata.iterator();
        while(it.hasNext()){
            JSONObject obj = it.next();
            String username = obj.get("username").toString();
            int userid = Integer.valueOf(obj.get("userid").toString());
            User newUser = new User(userid, username);
            updatedUserData.put(userid,newUser);
            JSONArray ticketList = (JSONArray) obj.get("tickets");
            newUser.updateTicketArray(ticketList);
        }
        System.out.println("[S] Updated userdata map to master version");
        userDataMap.updateuserDataMap(updatedUserData);
    }
}
