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
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;


public class NodeRegistationServlet extends HttpServlet {
    private NodeInfo nodeInfo;
    private FrontendNodeData frontendNodeData;
    private UserServiceNodeData userServiceNodeData;
    private ServletHelper servletHelper;
    private UserDataMap userDataMap;
    private final AtomicInteger version;

    public NodeRegistationServlet(NodeInfo nodeInfo, FrontendNodeData frontendNodeData, UserServiceNodeData userServiceNodeData, UserDataMap userDataMap, AtomicInteger version) {
        this.nodeInfo = nodeInfo;
        this.frontendNodeData = frontendNodeData;
        this.userServiceNodeData = userServiceNodeData;
        this.servletHelper = new ServletHelper();
        this.userDataMap = userDataMap;
        this.version = version;
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
        JSONObject requestBody = servletHelper.stringToJsonObject(servletHelper.requestToString(req));
        NodeInfo newNode = new NodeInfo(Integer.valueOf(requestBody.get("port").toString()), requestBody.get("host").toString());

        if(pathInfo.equals("/frontend")) {
            addFrontend(resp, requestBody, newNode);
        } else if(pathInfo.equals("/userservice")) {
            addUserService(resp, requestBody, newNode);
            //Response with all data
        }
    }
    private String buildResponse(){
        JSONObject obj = new JSONObject();
        JSONArray frontends = new JSONArray();
        JSONArray userServices = new JSONArray();

        for(NodeInfo info : this.frontendNodeData.getFrontendListCopy()){
            JSONObject item = new JSONObject();
            item.put("host", info.getHost());
            item.put("port", info.getPort());
            frontends.add(item);
        }
        for(NodeInfo info : this.userServiceNodeData.getUserServicesListCopy()){
            JSONObject item = new JSONObject();
            item.put("host", info.getHost());
            item.put("port", info.getPort());
            userServices.add(item);
        }
        obj.put("frontends", frontends);
        obj.put("userservices", userServices);
        obj.put("users", userDataMap.buildMapObject());
        obj.put("version", version.intValue());

        return obj.toJSONString();
    }

    private void addFrontend(HttpServletResponse resp, JSONObject requestBody, NodeInfo newNode) throws IOException {
        if(!frontendNodeData.checkIfNodeExist(newNode)) {
            if (this.nodeInfo.isMaster()) {
                System.out.println("[P] Registering new frontend " + newNode.getHost() + ":" + newNode.getPort());
                String path = "/register/frontend";
                replicateData(resp, path, requestBody);
            }else{
                System.out.println("[S] Registering new frontend " + newNode.getHost() + ":" + newNode.getPort());
            }
            frontendNodeData.addNode(newNode);
        }
        resp.setStatus(HttpStatus.OK_200);
    }

    private void addUserService(HttpServletResponse resp, JSONObject requestBody, NodeInfo newNode) throws IOException {
        if(!userServiceNodeData.checkIfNodeExist(newNode)){
            if (this.nodeInfo.isMaster()) { //TODO add return 200 if n nodes return 200
                System.out.println("[P] Registering new user service " + newNode.getHost() + ":" + newNode.getPort());
                String path = "/register/userservice";
                replicateData(resp, path, requestBody);
            }else{
                System.out.println("[S] Registering new user service " + newNode.getHost() + ":" + newNode.getPort());
            }
            userServiceNodeData.addNode(newNode);
        }
        resp.setStatus(HttpStatus.OK_200);
    }

    private void replicateData(HttpServletResponse resp, String path, JSONObject requestBody) throws IOException {
        PrintWriter pw = resp.getWriter();
        String responseData = buildResponse();
        for(NodeInfo nodeInfo : userServiceNodeData.getUserServicesListCopy()){
          int respCode = servletHelper.sendPostRequest(nodeInfo.getHost(), nodeInfo.getPort(), path, requestBody.toString());
          if(respCode != 200){
              //TODO remove node
          }
        }
        pw.write(responseData);
        pw.flush();
        pw.close();
        //TODO remove node if it do not resp may be done with throws
    }
}
