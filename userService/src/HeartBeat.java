import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class HeartBeat implements Runnable{
    private UserServiceNodeData userServiceNodeData;
    private FrontendNodeData frontendNodeData;
    private NodeInfo nodeInfo;
    private ServletHelper servletHelper;

    public HeartBeat(NodeInfo nodeInfo, UserServiceNodeData userServiceNodeData, FrontendNodeData frontendNodeData) {
        this.userServiceNodeData = userServiceNodeData;
        this.servletHelper = new ServletHelper();
        this.nodeInfo = nodeInfo;
        this.frontendNodeData = frontendNodeData;
    }


    @Override
    public void run() {
        boolean run = true;

        while (run) {
            if (nodeInfo.isMaster()) {
                List<NodeInfo> slaves = userServiceNodeData.getUserServicesListCopy();
                for(NodeInfo info : slaves){
                    if(checkIfalive(info.getHost(), info.getPort()) != 200){
                        String path = "/remove/userservice";
                        removeDeadNode(info, slaves, path);
                    }
                }
                List<NodeInfo> frontends = frontendNodeData.getFrontendListCopy();
                for(NodeInfo info : frontends){
                    if(checkIfalive(info.getHost(), info.getPort()) != 200){
                        String path = "/remove/frontend";
                        removeDeadNode(info, slaves, path);
                    }
                }

            } else {
                if(checkIfalive(nodeInfo.getMasterHost(), nodeInfo.getMasterPort()) != 200){
                    //TODO Start selecting new master
                    System.out.println("dead Master");
                }

            }

            try {
                TimeUnit.SECONDS.sleep(1);
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
        JSONObject obj = new JSONObject();
        obj.put("host", rmNode.getHost());
        obj.put("port", rmNode.getPort());
        servletHelper.sendPostRequest(nodeInfo, path, obj.toString());
        for(NodeInfo node : slaves){
            servletHelper.sendPostRequest(node, path, obj.toString());
        }
    }
}
