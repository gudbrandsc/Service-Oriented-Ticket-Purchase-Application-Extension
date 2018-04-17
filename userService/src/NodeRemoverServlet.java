import org.eclipse.jetty.http.HttpStatus;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class NodeRemoverServlet extends HttpServlet {
    private ServletHelper servletHelper;
    private UserServiceNodeData userServiceNodeData;
    private FrontendNodeData frontendNodeData;

    public NodeRemoverServlet(UserServiceNodeData userServiceNodeData, FrontendNodeData frontendNodeData) {
        this.userServiceNodeData = userServiceNodeData;
        this.servletHelper = new ServletHelper();
        this.frontendNodeData = frontendNodeData;
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
        NodeInfo removeNode = new NodeInfo(Integer.valueOf(requestBody.get("port").toString()), requestBody.get("host").toString());
        if(pathInfo.equals("/userservice")) {
            System.out.println("[S] Removing dead user service " + requestBody.get("host").toString() + ":" + Integer.valueOf(requestBody.get("port").toString()));
            removeUserServiceNode(removeNode);
            resp.setStatus(HttpStatus.OK_200);
        }else if(pathInfo.equals("/frontend")){
            removeFrontendNode(removeNode);
            System.out.println("[S] Removing dead frontend " + requestBody.get("host").toString() + ":" + Integer.valueOf(requestBody.get("port").toString()));
            resp.setStatus(HttpStatus.OK_200);

        }
    }

    private void removeUserServiceNode(NodeInfo node){
        userServiceNodeData.RemoveNode(node.getHost(), node.getPort());
    }

    private void removeFrontendNode(NodeInfo node){
        frontendNodeData.RemoveNode(node);
    }
}
