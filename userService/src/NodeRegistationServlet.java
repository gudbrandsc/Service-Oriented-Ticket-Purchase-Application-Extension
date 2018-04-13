import org.eclipse.jetty.http.HttpStatus;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;


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
        if(pathInfo.equals("/frontend")) {
            addFrontend(resp, requestBody);
        } else if(pathInfo.equals("/userservice")) {
            addUserService(resp, requestBody);
        }
    }

    private void addFrontend(HttpServletResponse resp, JSONObject requestBody) throws IOException {
        if (nodeInfo.isMaster()) {
            String path = "/register/frontend";
            replicateData(resp, path, requestBody);
        }
        frontendNodeData.addNode(new NodeInfo(Integer.valueOf(requestBody.get("port").toString()), requestBody.get("host").toString()));
        resp.setStatus(HttpStatus.OK_200);
    }

    private void addUserService(HttpServletResponse resp, JSONObject requestBody) throws IOException {
        if (nodeInfo.isMaster()) { //TODO add return 200 if n nodes return 200
            String path = "/register/userservice";
            replicateData(resp, path, requestBody);
        }
            userServiceNodeData.addNode(new NodeInfo(Integer.valueOf(requestBody.get("port").toString()), requestBody.get("host").toString()));
            resp.setStatus(HttpStatus.OK_200);
    }

    private void replicateData(HttpServletResponse resp, String path, JSONObject requestBody) throws IOException {
        JSONObject response = new JSONObject();
        PrintWriter pw = resp.getWriter();

        for(NodeInfo nodeInfo : userServiceNodeData.getUserServicesList()){
            servletHelper.sendPostRequest(nodeInfo, path, requestBody.toString());
            response.put("host", nodeInfo.getHost());
            response.put("port", nodeInfo.getPort());
        }

        pw.write(response.toString());
        pw.flush();
        pw.close();
        //TODO remove node if it do not resp may be done with throws
    }




}
