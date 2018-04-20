import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class  UserServiceMaster {

    private String userHost;
    private int userPort;
    private  static Logger log = LogManager.getLogger();

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
        log.info("[F] Updating master to: " + userHost +":"+ userPort);
        this.userHost = userHost;
        this.userPort = userPort;
    }
}
