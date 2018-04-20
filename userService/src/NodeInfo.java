/**
 * @author Gudbrand Schistad
 * Class used to maintain data about a user service
 * */
public class NodeInfo {
    private int port;
    private String host;
    private boolean isMaster;
    private String masterHost;
    private int masterPort;

    /** Constructor */
    public NodeInfo(int port, String host) {
        this.port = port;
        this.host = host;
        this.isMaster = false;
        this.masterHost = null;
        this.masterPort = 0;
    }

    /** @return  object port */
    public int getPort() {
        return this.port;
    }

    /** @return  object ip */
    public String getHost() {
        return this.host;
    }

    /** @return  true if node is master */
    public boolean isMaster() {
        return this.isMaster;
    }

    /** @return  get master ip */
    public synchronized String getMasterHost() {
        return this.masterHost;
    }

    /** @return  object master port */
    public synchronized int getMasterPort() {
        return this.masterPort;
    }

    /** Update master ip and port */
    public synchronized void updateMaster(String host, int port){
        this.masterHost = host;
        this.masterPort = port;
    }

    /** @return set node as master */
    public synchronized void setAsMaster(){
        this.isMaster = true;
    }
}
