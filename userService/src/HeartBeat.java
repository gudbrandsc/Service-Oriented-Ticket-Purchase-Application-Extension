import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author gudbrand schistad
 * Class that is used by all user services to check if other services are alive.
 * If a the thread is a master service it will check all secondaries and frontend.
 * if the thread is a secondary it will check that the master is alive.
 */
public class HeartBeat implements Runnable{
    private SecondariesMemberData secondariesMemberData;
    private FrontendMemberData frontendMemberData;
    private NodeInfo nodeInfo;
    private ServiceHelper serviceHelper;
    private NodeElector nodeElector;
    private  static Logger log = LogManager.getLogger();

    /** Constructor */
    public HeartBeat(NodeInfo nodeInfo, SecondariesMemberData secondariesMemberData, FrontendMemberData frontendMemberData, NodeElector nodeElector) {
        this.secondariesMemberData = secondariesMemberData;
        this.serviceHelper = new ServiceHelper();
        this.nodeInfo = nodeInfo;
        this.frontendMemberData = frontendMemberData;
        this.nodeElector = nodeElector;
    }

    /**
     * Method that sends GET requests to check if a node is alive.
     * If user service is a master then it will check all secondaries and frontend's.
     * If user service is a secondary it will check if the master is alive.
     * Sends heartbeats every 5 seconds
     */
    @Override
    public void run() {
        boolean run = true;

        while (run) {
            if (nodeInfo.isMaster()) {
                List<NodeInfo> secondaries = secondariesMemberData.getUserServicesListCopy();
                List<NodeInfo> frontends = frontendMemberData.getFrontendListCopy();
                sendHearBeatToSecondaries(secondaries);
                sendHearBeatToFrontends(secondaries, frontends);
            } else {
                sendHearBeatToMaster();
            }

            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Method used by the master to send a heartbeat request to all secondaries
     * If a secondary do not respond with a Http status 200.
     * then remove the secondary from all membership lists
     * @param secondaries list copy of all registered secondaries
     */
    private void sendHearBeatToSecondaries(List<NodeInfo> secondaries){
        for(NodeInfo info : secondaries){
            if(checkIfAlive(info.getHost(), info.getPort()) != 200){
                log.info("[P] Secondary did not respond to heartbeat " + info.getHost() + ":" + info.getPort());
                String path = "/remove/userservice";
                removeDeadNode(info, secondaries, path);
            }
        }
    }

    /**
     * Method used by the master to send a heartbeat request to all frontend services
     * If a frontend services do not respond with a Http status 200,
     * then remove the secondary from all membership lists
     * @param secondaries list copy of all registered secondaries
     */
    private void sendHearBeatToFrontends(List<NodeInfo> secondaries, List<NodeInfo> frontends ){
        for(NodeInfo info : frontends){
            if(checkIfAlive(info.getHost(), info.getPort()) != 200){
                log.info("[P] Frontend did not respond to heartbeat");
                String path = "/remove/frontend";
                removeDeadNode(info, secondaries, path);
            }
        }
    }

    /**
     * Method used by the secondaries to send a heartbeat request to the master
     * If a master do not respond,
     * it will start an election
     */
    private void sendHearBeatToMaster(){
        if(checkIfAlive(nodeInfo.getMasterHost(), nodeInfo.getMasterPort()) != 200){
            log.info("[S] Master did not respond to heartbeat...");
            nodeElector.startElection();
        }
    }

    /**
     * Sends get methods to check if a service is alive and returns response code,
     * or 401 if there is no response
     * @param host host of the service
     * @param port port of the service
     * @return 200 if it got resp and 401 on timeout
     */
    private int checkIfAlive(String host, int port){
        String path = "alive";
        int respCode;
        try {
            String url = "http://" + host + ":" + port + "/" + path;
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            respCode = con.getResponseCode();

        } catch (IOException e) {
            return 401;
        }
        return respCode;
    }

    /**
     * Removes a dead node from all secondaries.
     * @param rmNode node info for the node to remove
     * @param secondaries list of all registered secondaries
     * @param path path to specify if node is a frontend or user service. */
    private void removeDeadNode(NodeInfo rmNode, List<NodeInfo> secondaries, String path){
        log.info("[P] Removing dead node from all services");
        JSONObject obj = new JSONObject();
        obj.put("host", rmNode.getHost());
        obj.put("port", rmNode.getPort());
        serviceHelper.sendPostRequest(nodeInfo.getHost(),nodeInfo.getPort(), path, obj.toString());
        for(NodeInfo node : secondaries){
            serviceHelper.sendPostRequest(node.getHost(), node.getPort(), path, obj.toString());
        }
    }
}
