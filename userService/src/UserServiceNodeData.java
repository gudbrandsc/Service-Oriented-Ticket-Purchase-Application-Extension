import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserServiceNodeData {
    List<NodeInfo> userServicesList;

    /** Constructor */
    public UserServiceNodeData() {
        this.userServicesList = Collections.synchronizedList(new ArrayList<NodeInfo>());
    }

    public synchronized void addNode(NodeInfo nodeInfo){
        boolean exist = false;
        for (NodeInfo info : userServicesList){
            if (info.getPort() == nodeInfo.getPort() && info.getHost() == nodeInfo.getHost()){
                exist = true;
            }
        }
        if(!exist) {
            System.out.println("Added non existing node...");
            this.userServicesList.add(nodeInfo);
        }
    }

    public List<NodeInfo> getUserServicesList() {
        return this.userServicesList;
    }
}
