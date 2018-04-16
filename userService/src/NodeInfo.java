public class NodeInfo {
    private int port;
    private String host;
    private boolean isMaster;
    private String masterHost;
    private int masterPort;

    public NodeInfo(int port, String host) {
        this.port = port;
        this.host = host;
        this.isMaster = false;
        this.masterHost = null;
        this.masterPort = 0;
    }

    public int getPort() {
        return this.port;
    }

    public String getHost() {
        return this.host;
    }

    public synchronized boolean isMaster() {
        return this.isMaster;
    }

    public synchronized String getMasterHost() {
        return this.masterHost;
    }

    public synchronized int getMasterPort() {
        return this.masterPort;
    }

    public synchronized void updateMaster(String host, int port){
        System.out.println("MESSAGE: Updating master to: " + host + ":" + port);
        this.masterHost = host;
        this.masterPort = port;
    }

    public synchronized void setAsMaster(){
        this.isMaster = true;
    }

}
