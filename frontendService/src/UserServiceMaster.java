public class  UserServiceMaster {
    private String userHost;
    private int userPort;

    public UserServiceMaster(String userHost, int userPort){
        this.userHost = userHost;
        this.userPort = userPort;
    }

    public String getUserHost() {
        return userHost;
    }

    public int getUserPort() {
        return this.userPort;
    }

    public synchronized void setNewMaster(String userHost, int userPort) {
        this.userHost = userHost;
        this.userPort = userPort;
    }
}
