import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Gudbrand Schistad
 * Class used to start an election and set a secondary as new master
 */
public class NodeElector {
    private SecondariesMemberData secondariesMemberData;
    private FrontendMemberData frontendMemberData;
    private ServiceHelper serviceHelper;
    private  NodeInfo nodeInfo;
    private  UserDataMap userDataMap;
    private  AtomicInteger version;
    private AtomicInteger userID;
    private  static Logger log = LogManager.getLogger();

    /** Constructor */
    public NodeElector(SecondariesMemberData secondariesMemberData, NodeInfo nodeInfo, FrontendMemberData frontendMemberData,
                       UserDataMap userDataMap, AtomicInteger version, AtomicInteger userID) {
        this.secondariesMemberData = secondariesMemberData;
        this.serviceHelper = new ServiceHelper();
        this.nodeInfo = nodeInfo;
        this.frontendMemberData = frontendMemberData;
        this.userDataMap = userDataMap;
        this.version = version;
        this.userID = userID;
    }

    /**
     * Method that sends election requests to all higher valued process secondaries, and waits for any 200 response
     * If no response is received it will set itself as the master
     */
    public void startElection(){
        log.info("[S] Starting election...");
        String nodeId = nodeInfo.getHost() + nodeInfo.getPort();
        String path = "election";
        boolean setAsMaster = true;
        int higherNumberProcess = 0;

        for (NodeInfo info : secondariesMemberData.getUserServicesListCopy()) {
            String id = info.getHost() + info.getPort();
            if (id.compareTo(nodeId) > 0) {
                higherNumberProcess++;
                if (serviceHelper.sendGetAndReturnStatus(info.getHost(), info.getPort(), path) == 200) {
                    setAsMaster = false;
                    break;
                    // Break will exist the loop closest to it
                    // See: https://docs.oracle.com/javase/tutorial/java/nutsandbolts/branch.html
                }
            }
        }

        log.info("[S] Sent " + higherNumberProcess + " election requests to services with higher process id");

        if (setAsMaster) {
            setNewMaster();
        }
    }

    /**
     * Method that sets a secondary as a new master, and informs all other secondaries about the updated.
     * Builds a body for the update request containing all necessary data
     */
    private void setNewMaster() {
        log.info("[S] Setting myself as the new master");
        nodeInfo.setAsMaster();

        String path = "/election";
        JSONObject obj = userDataMap.buildMapObject();
        List<NodeInfo> secondaries =  secondariesMemberData.getUserServicesListCopy();

        obj.put("host", nodeInfo.getHost());
        obj.put("port", nodeInfo.getPort());
        obj.put("version", version.intValue());
        obj.put("userID", userID.intValue());

        for (NodeInfo info : secondaries) {
            serviceHelper.sendPostRequest(info.getHost(), info.getPort(), path, obj.toJSONString());
        }

        for (NodeInfo info : frontendMemberData.getFrontendListCopy()) {
            serviceHelper.sendPostRequest(info.getHost(), info.getPort(), path, obj.toJSONString());
        }
    }
}
