import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author Gudbrand Schistad
 * Class servlet that is used to register secondaries and frontend services
 */
public class NodeRegistrationServlet extends HttpServlet {
    private NodeInfo nodeInfo;
    private FrontendMemberData frontendMemberData;
    private SecondariesMemberData secondariesMemberData;
    private ServiceHelper serviceHelper;
    private UserDataMap userDataMap;
    private final AtomicInteger version;
    private final AtomicInteger userID;
    private  static Logger log = LogManager.getLogger();

    /** Constructor */
    public NodeRegistrationServlet(NodeInfo nodeInfo, FrontendMemberData frontendMemberData, SecondariesMemberData secondariesMemberData,
                                   UserDataMap userDataMap, AtomicInteger version, AtomicInteger userID) {
        this.nodeInfo = nodeInfo;
        this.frontendMemberData = frontendMemberData;
        this.secondariesMemberData = secondariesMemberData;
        this.serviceHelper = new ServiceHelper();
        this.userDataMap = userDataMap;
        this.version = version;
        this.userID = userID;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter printWriter = resp.getWriter();
        resp.setContentType("application/json; charset=UTF-8");
        resp.setStatus(HttpStatus.OK_200);
        printWriter.println(buildResponse());
        printWriter.flush();

    }

    /**
     * Do post method that handles all incoming post requests.
     * If request do not match the any pattern set response status  400
     * @param req incoming request
     * @param resp servlet Http response object
     * @throws IOException
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        JSONObject requestBody = serviceHelper.stringToJsonObject(serviceHelper.requestToString(req));
        NodeInfo newNode = new NodeInfo(Integer.valueOf(requestBody.get("port").toString()), requestBody.get("host").toString());

        if(pathInfo.equals("/frontend")) {
            registerFrontend(resp, requestBody, newNode);
        } else if(pathInfo.equals("/userservice")) {
            registerSecondary(resp, requestBody, newNode);
        }
    }

    /** @return A json string containing all userdata as well as current version number and userId */
    private String buildResponse(){
        JSONObject obj = new JSONObject();
        JSONArray frontends = new JSONArray();
        JSONArray userServices = new JSONArray();

        for(NodeInfo info : this.frontendMemberData.getFrontendListCopy()){
            JSONObject item = new JSONObject();
            item.put("host", info.getHost());
            item.put("port", info.getPort());
            frontends.add(item);
        }
        for(NodeInfo info : this.secondariesMemberData.getUserServicesListCopy()){
            JSONObject item = new JSONObject();
            item.put("host", info.getHost());
            item.put("port", info.getPort());
            userServices.add(item);
        }
        obj.put("frontends", frontends);
        obj.put("userservices", userServices);
        obj.put("users", userDataMap.buildMapObject());
        obj.put("version", version.intValue());
        obj.put("userID", userID.intValue());

        return obj.toJSONString();
    }
    /**
     * Method that registers a new frontend.
     * If service is a master it will replicate the registration to all secondaries
     */
    private void registerFrontend(HttpServletResponse resp, JSONObject requestBody, NodeInfo newNode) throws IOException {
        if (this.nodeInfo.isMaster()) {
            log.info("[P] Registering new frontend " + newNode.getHost() + ":" + newNode.getPort());
            String path = "/register/frontend";
            frontendMemberData.addNode(newNode);
            resp.setStatus(serviceHelper.replicateToSecondaries(path, requestBody.toJSONString(), secondariesMemberData));
            resp.setStatus(HttpStatus.OK_200);
        }else{
            frontendMemberData.addNode(newNode);
            log.info("[S] Registering new frontend " + newNode.getHost() + ":" + newNode.getPort());
            resp.setStatus(HttpStatus.OK_200);
        }
    }

    /**
     * Method that registers a new secondaries.
     * If service is a master it will replicate the registration to all secondaries
     */
    private void registerSecondary(HttpServletResponse resp, JSONObject requestBody, NodeInfo newNode) throws IOException {
        if (this.nodeInfo.isMaster()) {
            log.info("[P] Registering new user service " + newNode.getHost() + ":" + newNode.getPort());

            PrintWriter pw = resp.getWriter();
            String path = "/register/userservice";
            resp.setStatus(serviceHelper.replicateToSecondaries(path,requestBody.toJSONString(), secondariesMemberData));

            pw.write(buildResponse());
            pw.flush();
            pw.close();
        }else{
            log.info("[S] Registering new user service " + newNode.getHost() + ":" + newNode.getPort());
            resp.setStatus(HttpStatus.OK_200);
        }
        secondariesMemberData.addNode(newNode);
    }
}
