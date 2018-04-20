import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Gudbrand Schistad
 * Class that stores node information about all registered frontend services
 */
public class FrontendMemberData {
    List<NodeInfo> frontendList;

    /** Constructor */
    public FrontendMemberData() {
        this.frontendList = Collections.synchronizedList(new ArrayList<NodeInfo>());
    }

    /**
     * Method used add frontend services, if a frontend with the same ip and port exist then do nothing.
     * @param nodeInfo Node info for the frontend
     */
    public synchronized void addNode(NodeInfo nodeInfo){
        boolean add = true;
        for (NodeInfo info : frontendList){
            if (info.getPort() == nodeInfo.getPort() && info.getHost().equals(nodeInfo.getHost())){
                add = false;
            }
        }
        if(add) {
            this.frontendList.add(nodeInfo);
        }
    }

    /**
     * Method that create a copy of the frontend list.
     * @return copy of the frontend list
     */
    public synchronized List<NodeInfo> getFrontendListCopy() {
        List<NodeInfo> copy = new ArrayList<>();
        copy.addAll(this.frontendList);
        return copy;
    }

    /**
     * Method that removes a node from the list.
     * This is done by creating a list copy with all nodes except the node with matching ip & port and
     * setting the list to the updated list.
     * @param nodeInfo node to remove
     */
    public synchronized void RemoveNode(NodeInfo nodeInfo){
        List<NodeInfo> updatedList = Collections.synchronizedList(new ArrayList<NodeInfo>());
        for(NodeInfo info : this.frontendList){
            if(info.getPort() != nodeInfo.getPort() && info.getHost().equals(nodeInfo.getHost())){
                updatedList.add(info);
            }
        }
        this.frontendList = updatedList;
    }
}
