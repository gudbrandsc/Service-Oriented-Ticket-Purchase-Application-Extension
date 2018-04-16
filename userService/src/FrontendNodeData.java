import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class FrontendNodeData {
    List<NodeInfo> frontendList;

    /** Constructor */
    public FrontendNodeData() {
        this.frontendList = Collections.synchronizedList(new ArrayList<NodeInfo>());
    }

   public synchronized void addNode(NodeInfo nodeInfo){
       System.out.println("MESSAGE: Adding frontend " + nodeInfo.getHost() + ":" + nodeInfo.getPort());
            this.frontendList.add(nodeInfo);

   }

    public synchronized boolean checkIfNodeExist(NodeInfo nodeInfo){
        for (NodeInfo info : frontendList){
            if (info.getPort() == nodeInfo.getPort() && info.getHost() == nodeInfo.getHost()){
                return true;
            }
        }
        return false;
    }



    public synchronized List<NodeInfo> getFrontendListCopy() {
        List<NodeInfo> copy = new ArrayList<>();
        for (NodeInfo copynode : this.frontendList){
            copy.add(copynode);
        }
        return copy;
    }

    public synchronized void RemoveNode(NodeInfo nodeInfo){
        System.out.println("MESSAGE: Removing frontend " + nodeInfo.getHost() + ":" + nodeInfo.getPort());
        List<NodeInfo> updatedList = Collections.synchronizedList(new ArrayList<NodeInfo>());
        for(NodeInfo info : this.frontendList){
            if(info.getPort() != nodeInfo.getPort() && info.getHost() != nodeInfo.getHost()){
                updatedList.add(info);
            }
        }
        this.frontendList = updatedList;
    }
}
