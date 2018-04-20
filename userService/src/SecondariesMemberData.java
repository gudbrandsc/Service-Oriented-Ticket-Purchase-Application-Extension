import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Gudbrand Schistad
 * Class that stores node information about all registered secondaries services
 */
public class SecondariesMemberData {
    private List<NodeInfo> userServicesList;

    /** Constructor */
    public SecondariesMemberData() {
        this.userServicesList = Collections.synchronizedList(new ArrayList<NodeInfo>());
    }


    /**
     * Method used add a secondary, if a secondary with the same ip and port exist then do nothing.
     * @param nodeInfo Node info for the frontend
     */
    public synchronized void addNode(NodeInfo nodeInfo){
        boolean add = true;
        for (NodeInfo info : userServicesList){
            if (info.getPort() == nodeInfo.getPort() && info.getHost().equals(nodeInfo.getHost())){
                add = false;
            }
        }
        if(add) {
            this.userServicesList.add(nodeInfo);
        }
    }

    /**
     * Method that create a copy of the secondaries list.
     * @return copy of the frontend list
     */
    public synchronized List<NodeInfo> getUserServicesListCopy() {
        List<NodeInfo> copy = new ArrayList<>();
        for (NodeInfo copynode : this.userServicesList){
            copy.add(copynode);
        }
        return copy;
    }

    /**
     * Method that removes a node from the list.
     * This is done by creating a list copy with all nodes except the node with matching ip & port and
     * setting the list to the updated list.
     * @param host of node to remove
     * @param port of node to remove
     */
    public synchronized void RemoveNode(String host, int port){
        List<NodeInfo> updatedList = Collections.synchronizedList(new ArrayList<NodeInfo>());
        for(NodeInfo info : this.userServicesList){
            if(info.getPort() != port && info.getHost() != host){
                updatedList.add(info);
            }
        }
        this.userServicesList = updatedList;
    }
}
