public class UserServiceData {
    private boolean isMaster;
    private int nodeId;

    UserServiceData(boolean isMaster){
        this.isMaster = isMaster;
    }

    public synchronized boolean isMaster() {
        return this.isMaster;
    }

    public synchronized void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }
}
