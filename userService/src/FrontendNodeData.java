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
        boolean exist = false;
        for (NodeInfo info : frontendList){
            if (info.getPort() == nodeInfo.getPort() && info.getHost() == nodeInfo.getHost()){
                exist = true;
            }
        }
        if(!exist) {
            System.out.println("Added non existing node...");
            this.frontendList.add(nodeInfo);
        }
   }

    public List<NodeInfo> getFrontendList() {
        return this.frontendList;
    }
}
