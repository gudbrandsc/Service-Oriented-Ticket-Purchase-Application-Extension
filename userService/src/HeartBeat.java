import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class HeartBeat implements Runnable{
    private UserServiceNodeData userServiceNodeData;
    private FrontendNodeData frontendNodeData;
    private NodeInfo nodeInfo;
    private ServletHelper servletHelper;
    private NodeElector nodeElector;

    public HeartBeat(NodeInfo nodeInfo, UserServiceNodeData userServiceNodeData, FrontendNodeData frontendNodeData, NodeElector nodeElector) {
        this.userServiceNodeData = userServiceNodeData;
        this.servletHelper = new ServletHelper();
        this.nodeInfo = nodeInfo;
        this.frontendNodeData = frontendNodeData;
        this.nodeElector = nodeElector;
    }


    @Override
    public void run() {
        boolean run = true;

        while (run) {
            if (nodeInfo.isMaster()) {
                List<NodeInfo> secondaries = userServiceNodeData.getUserServicesListCopy();
                List<NodeInfo> frontends = frontendNodeData.getFrontendListCopy();
                for(NodeInfo info : secondaries){
                    if(checkIfalive(info.getHost(), info.getPort()) != 200){
                        System.out.println("[P] Secondary did not respond to heartbeat " + info.getHost() + ":" + info.getPort());
                        String path = "/remove/userservice";
                        removeDeadNode(info, secondaries, path);
                    }
                }
                for(NodeInfo info : frontends){
                    if(checkIfalive(info.getHost(), info.getPort()) != 200){
                        System.out.println("[P] Frontend did not respond to heartbeat " + info.getHost() + ":" + info.getPort());
                        String path = "/remove/frontend";
                        removeDeadNode(info, secondaries, path);
                    }
                }

            } else {
                if(checkIfalive(nodeInfo.getMasterHost(), nodeInfo.getMasterPort()) != 200){
                    System.out.println("[S] Master did not respond to heartbeat");
                    nodeElector.startElection();
                    //Also update data to mach data in master
                }
            }

            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private int checkIfalive(String host, int port){
        String path = "alive";
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

    private void removeDeadNode(NodeInfo rmNode, List<NodeInfo> slaves, String path){
        System.out.println("[P] Removing dead node from all services");
        JSONObject obj = new JSONObject();
        obj.put("host", rmNode.getHost());
        obj.put("port", rmNode.getPort());
        servletHelper.sendPostRequest(nodeInfo.getHost(),nodeInfo.getPort(), path, obj.toString());
        for(NodeInfo node : slaves){
            servletHelper.sendPostRequest(node.getHost(),node.getPort(), path, obj.toString());
        }
    }
}
