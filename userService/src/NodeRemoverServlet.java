import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;
import org.json.simple.JSONObject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet that removes a dead node from the node membership data
 */
public class NodeRemoverServlet extends HttpServlet {
    private ServiceHelper serviceHelper;
    private SecondariesMemberData secondariesMemberData;
    private FrontendMemberData frontendMemberData;
    private  static Logger log = LogManager.getLogger();

    /** Constructor */
    public NodeRemoverServlet(SecondariesMemberData secondariesMemberData, FrontendMemberData frontendMemberData) {
        this.secondariesMemberData = secondariesMemberData;
        this.serviceHelper = new ServiceHelper();
        this.frontendMemberData = frontendMemberData;
    }

    /**
     * Do post method that removes a frontend- or user service from membership list
     * If request do not match the any pattern set response status  400
     * @param req incoming request
     * @param resp servlet Http response object
     * @throws IOException
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        JSONObject requestBody = serviceHelper.stringToJsonObject(serviceHelper.requestToString(req));
        NodeInfo removeNode = new NodeInfo(Integer.valueOf(requestBody.get("port").toString()), requestBody.get("host").toString());
        if(pathInfo.equals("/userservice")) {
            log.info("[S/P] Removing dead user service " + requestBody.get("host").toString() + ":" + Integer.valueOf(requestBody.get("port").toString()));
            secondariesMemberData.RemoveNode(removeNode.getHost(), removeNode.getPort());
            resp.setStatus(HttpStatus.OK_200);
        }else if(pathInfo.equals("/frontend")){
            log.info("[S/P] Removing dead frontend " + requestBody.get("host").toString() + ":" + Integer.valueOf(requestBody.get("port").toString()));
            frontendMemberData.RemoveNode(removeNode);
            resp.setStatus(HttpStatus.OK_200);
        }
    }
}
