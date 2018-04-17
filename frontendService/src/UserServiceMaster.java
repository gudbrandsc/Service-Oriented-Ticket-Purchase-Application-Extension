
public class  UserServiceMaster {

    private String userHost;
    private int userPort;

    public UserServiceMaster(String userHost, int userPort){
        this.userHost = userHost;
        this.userPort = userPort;
    }

    public synchronized String getUserHost() {
        return userHost;
    }

    public synchronized int getUserPort() {
        return this.userPort;
    }

    public synchronized void setNewMaster(String userHost, int userPort) {
        System.out.println("[S] Updating master to: " + userHost +":"+ userPort);
        this.userHost = userHost;
        this.userPort = userPort;
    }
}
