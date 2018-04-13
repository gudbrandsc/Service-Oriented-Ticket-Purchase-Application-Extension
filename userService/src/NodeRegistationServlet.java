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


public class NodeRegistationServlet extends HttpServlet {
    private NodeInfo nodeInfo;
    private FrontendNodeData frontendNodeData;
    private UserServiceNodeData userServiceNodeData;
    private ServletHelper servletHelper;

    public NodeRegistationServlet(NodeInfo nodeInfo, FrontendNodeData frontendNodeData, UserServiceNodeData userServiceNodeData) {
        this.nodeInfo = nodeInfo;
        this.frontendNodeData = frontendNodeData;
        this.userServiceNodeData = userServiceNodeData;
        this.servletHelper = new ServletHelper();
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
        System.out.println("Got request");
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
//        System.out.println("resp : \n" + obj.toJSONString());
        return obj.toJSONString();

    }

    private void addFrontend(HttpServletResponse resp, JSONObject requestBody, NodeInfo newNode) throws IOException {
        if(!frontendNodeData.checkIfNodeExist(newNode)) {
            if (this.nodeInfo.isMaster()) {
                String path = "/register/frontend";
                replicateData(resp, path, requestBody);
            }
            frontendNodeData.addNode(newNode);
        }
        resp.setStatus(HttpStatus.OK_200);
    }

    private void addUserService(HttpServletResponse resp, JSONObject requestBody, NodeInfo newNode) throws IOException {
        if(!userServiceNodeData.checkIfNodeExist(newNode)){
            if (this.nodeInfo.isMaster()) { //TODO add return 200 if n nodes return 200
                String path = "/register/userservice";
                replicateData(resp, path, requestBody);
            }
            userServiceNodeData.addNode(newNode);
        }
        resp.setStatus(HttpStatus.OK_200);
    }

    private void replicateData(HttpServletResponse resp, String path, JSONObject requestBody) throws IOException {
        PrintWriter pw = resp.getWriter();

        for(NodeInfo nodeInfo : userServiceNodeData.getUserServicesListCopy()){
            servletHelper.sendPostRequest(nodeInfo, path, requestBody.toString());
        }

        pw.write(buildResponse());
        pw.flush();
        pw.close();
        //TODO remove node if it do not resp may be done with throws
    }




}
