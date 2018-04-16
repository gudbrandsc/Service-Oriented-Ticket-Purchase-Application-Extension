import org.json.simple.JSONObject;

public class NodeElector {
    private UserServiceNodeData userServiceNodeData;
    private FrontendNodeData frontendNodeData;
    private ServletHelper servletHelper;
    private NodeInfo nodeInfo;
    private UserDataMap userDataMap;


    public NodeElector(UserServiceNodeData userServiceNodeData, NodeInfo nodeInfo, FrontendNodeData frontendNodeData, UserDataMap userDataMap) {
        this.userServiceNodeData = userServiceNodeData;
        this.servletHelper = new ServletHelper();
        this.nodeInfo = nodeInfo;
        this.frontendNodeData = frontendNodeData;
        this.userDataMap = userDataMap;
    }

    public synchronized void startElection(){
        String nodeId = nodeInfo.getHost() + nodeInfo.getPort();
        String path = "/election/elect";
        JSONObject obj = new JSONObject();
        boolean setAsMaster = true;
        if(!nodeInfo.isMaster()) {
            for (NodeInfo info : userServiceNodeData.getUserServicesListCopy()) {
                String id = info.getHost() + info.getPort();
                if (id.compareTo(nodeId) > 0) {
                    if (servletHelper.sendPostRequest(info.getHost(),info.getPort(), path, obj.toJSONString()) == 200) {
                        System.out.println("MESSAGE: Sent election request to secondary");
                        setAsMaster = false;
                    }
                }
            }
            if (setAsMaster) {
                setNewMaster();
            }
        }
    }

    private synchronized void setNewMaster() {
        System.out.println("MESSAGE: Im the new master");
        String path = "/election/update";

        nodeInfo.setAsMaster();
        JSONObject obj = new JSONObject();
        obj.put("host", nodeInfo.getHost());
        obj.put("port", nodeInfo.getPort());
        System.out.println("MESSAGE: Update master on all user services ");
        JSONObject data = userDataMap.buildMapObject();
        for (NodeInfo info : userServiceNodeData.getUserServicesListCopy()) {
            int status = servletHelper.sendPostRequest(info.getHost(), info.getPort(), path, obj.toJSONString());
            if (status != 200) {
                //Remove node and send request
            }
        }

        for (NodeInfo info : frontendNodeData.getFrontendListCopy()) {
            System.out.println("MESSAGE:Update master on all frontend services");
            int status = servletHelper.sendPostRequest(info.getHost(), info.getPort(), path, obj.toJSONString());
            if (status != 200) {
                //Remove frontend
            }

        }
    }
}
