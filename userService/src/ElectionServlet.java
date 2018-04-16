import org.eclipse.jetty.http.HttpStatus;
import org.json.simple.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ElectionServlet extends HttpServlet {
    private NodeInfo nodeInfo;
    private NodeElector nodeElector;
    private ServletHelper servletHelper;

    public ElectionServlet(NodeInfo nodeInfo, NodeElector nodeElector) {
        this.nodeInfo = nodeInfo;
        this.nodeElector = nodeElector;
        this.servletHelper = new ServletHelper();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        if (pathInfo.equals("/update")) {
            System.out.println("Got update master");
            JSONObject requestBody = servletHelper.stringToJsonObject(servletHelper.requestToString(req));
            if (requestBody.containsKey("host") && requestBody.containsKey("port")) {
                resp.setStatus(HttpStatus.OK_200);
                nodeInfo.updateMaster(requestBody.get("host").toString(), Integer.valueOf(requestBody.get("port").toString()));
            } else {
                resp.setStatus(HttpStatus.BAD_REQUEST_400);
            }
        } else if (pathInfo.equals("/elect")) {
            System.out.println("Got request for election");
            resp.setStatus(HttpStatus.OK_200);
            nodeElector.startElection();
        } else {
            resp.setStatus(HttpStatus.BAD_REQUEST_400);
        }
    }
}
