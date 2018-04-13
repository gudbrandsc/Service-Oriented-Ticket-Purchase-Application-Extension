import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class UserServiceNodeData {
    private List<NodeInfo> userServicesList;

    /** Constructor */
    public UserServiceNodeData() {
        this.userServicesList = Collections.synchronizedList(new ArrayList<NodeInfo>());
    }

    public synchronized void addNode(NodeInfo nodeInfo){
            System.out.println("Added non existing node...");
            this.userServicesList.add(nodeInfo);
    }

    public synchronized boolean checkIfNodeExist(NodeInfo nodeInfo){
        for (NodeInfo info : userServicesList){
            if (info.getPort() == nodeInfo.getPort() && info.getHost() == nodeInfo.getHost()){
                return true;
            }
        }
        return false;
    }

    public synchronized List<NodeInfo> getUserServicesListCopy() {
        List<NodeInfo> copy = new ArrayList<>();
        for (NodeInfo copynode : this.userServicesList){
            copy.add(copynode);
        }
        return copy;
    }

    public synchronized void RemoveNode(NodeInfo nodeInfo){
        System.out.println("Removing node...");
        List<NodeInfo> updatedList = Collections.synchronizedList(new ArrayList<NodeInfo>());
        for(NodeInfo info : this.userServicesList){
            if(info.getPort() != nodeInfo.getPort() && info.getHost() != nodeInfo.getHost()){
                updatedList.add(info);
            }
        }
        this.userServicesList = updatedList;
    }
}
