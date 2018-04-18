import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class NodeElector {
    private  UserServiceNodeData userServiceNodeData;
    private  FrontendNodeData frontendNodeData;
    private  ServletHelper servletHelper;
    private  NodeInfo nodeInfo;
    private  UserDataMap userDataMap;
    private  AtomicInteger version;

    public NodeElector(UserServiceNodeData userServiceNodeData, NodeInfo nodeInfo, FrontendNodeData frontendNodeData, UserDataMap userDataMap, AtomicInteger version) {
        this.userServiceNodeData = userServiceNodeData;
        this.servletHelper = new ServletHelper();
        this.nodeInfo = nodeInfo;
        this.frontendNodeData = frontendNodeData;
        this.userDataMap = userDataMap;
        this.version = version;
    }

    public void startElection(){
        String nodeId = nodeInfo.getHost() + nodeInfo.getPort();
        System.out.println("[S] Starting election...");

        boolean setAsMaster = true;
        int higherNumberProcess = 0;
        for (NodeInfo info : userServiceNodeData.getUserServicesListCopy()) {
            String id = info.getHost() + info.getPort();
            if (id.compareTo(nodeId) > 0) {
                higherNumberProcess++;
                if (sendGetElection(info.getHost(),info.getPort()) == 200) {
                    setAsMaster = false;
                }
            }
        }
        System.out.println("[S] Sent " + higherNumberProcess + " election requests to services with higher process id");

        if (setAsMaster) {
            setNewMaster();
        }

    }

    private void setNewMaster() {
        System.out.println("[S] Setting myself as the new master");
        String path = "/election";

        nodeInfo.setAsMaster();
        JSONObject obj = userDataMap.buildMapObject();
        obj.put("host", nodeInfo.getHost());
        obj.put("port", nodeInfo.getPort());
        obj.put("version", version.intValue());
        List<NodeInfo> secondaries =  userServiceNodeData.getUserServicesListCopy();
        for (NodeInfo info : secondaries) {
            int status = servletHelper.sendPostRequest(info.getHost(), info.getPort(), path, obj.toJSONString());
            if (status != 200) {
                //TODO: Remove node and send request
            }
        }

        for (NodeInfo info : frontendNodeData.getFrontendListCopy()) {
            int status = servletHelper.sendPostRequest(info.getHost(), info.getPort(), path, obj.toJSONString());
            if (status != 200) {
                //TODO: Remove frontend
            }
        }
    }


    private int sendGetElection(String host, int port){
        String path = "election";
        try {
            String url = "http://" + host + ":" + port + "/" + path;
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            return con.getResponseCode();

        } catch (IOException e) {
            return 401;
        }
    }
}
