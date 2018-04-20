import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;
import java.net.*;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author Gudbrand Schistad
 * Class that starts the user service server, and maps all requests to the correct servlet.
 */
public class UserService {
    private static NodeInfo nodeInfo;
    private static PropertiesLoader properties;
    private static FrontendMemberData frontendMemberData;
    private static SecondariesMemberData secondariesMemberData;
    private static UserDataMap userDataMap;
    private static NodeElector nodeElector;
    private static AtomicInteger version = new AtomicInteger(1);
    private static AtomicInteger userid = new AtomicInteger(1);
    private static Logger log = LogManager.getLogger();

    /**
     * Main method that starts a user service, and register the service with the master if its a secondary.
     * Also sets up all servlets and starts server
     */
    public static void main(String[] args) {

        nodeInfo = null;
        properties = new PropertiesLoader();
        frontendMemberData = new FrontendMemberData();
        secondariesMemberData = new SecondariesMemberData();
        userDataMap = new UserDataMap();
        log.debug(properties.getEventhost()+ " HOST");
        log.debug(properties.getEventport() + " PORT");

        if(args.length >= 2 && args[0].equals("-port")){
            try {
                nodeInfo = new NodeInfo(Integer.parseInt(args[1]), InetAddress.getLocalHost().getHostAddress());

                if(args[2].equals("-setAsMaster")) {
                    nodeInfo.setAsMaster();
                    version.set(1);
                }else if (args.length >= 6 && args[2].equals("-masterHost") && args[4].equals("-masterPort")){
                    nodeInfo.updateMaster(args[3], Integer.parseInt(args[5]));
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }else{
            log.fatal("Invalid arguments. \nFormat: -port **** -masterURL *****");
            System.exit(-1);
        }


        Server server = new Server(nodeInfo.getPort());
        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);
        handler.addServletWithMapping(new ServletHolder(new UserServiceServlet(userDataMap, userid, secondariesMemberData, nodeInfo,properties, version)), "/*");
        handler.addServletWithMapping(new ServletHolder(new NodeRegistationServlet(nodeInfo, frontendMemberData, secondariesMemberData, userDataMap, version, userid)), "/register/*");
        handler.addServletWithMapping(HeartServlet.class, "/alive");
        handler.addServletWithMapping(new ServletHolder(new NodeRemoverServlet(secondariesMemberData, frontendMemberData)), "/remove/*");
        handler.addServletWithMapping(new ServletHolder(new ElectionServlet(nodeInfo, secondariesMemberData, userDataMap, version, userid)), "/election/*");


        if(!nodeInfo.isMaster()){
            boolean success = registerUserServiceRequest();
            if(!success){
                log.info("Unable to register node with master");
                System.exit(-1);
            }
        }
        nodeElector = new NodeElector(secondariesMemberData, nodeInfo, frontendMemberData, userDataMap, version, userid);
        log.info("Starting server on port " + nodeInfo.getPort() + "...");
        log.info("Server is master: " + nodeInfo.isMaster() + "...");
        new Thread(new HeartBeat(nodeInfo, secondariesMemberData, frontendMemberData, nodeElector)).start();


        try {
            server.start();
            server.join();
            log.fatal("Exiting...");
        }
        catch (Exception ex) {
            log.fatal("Interrupted while running server.");
            System.exit(-1);
        }
    }

    /**
     * Method used to register a secondary with the master
     * @return true if registration was a success
     */
    private static boolean registerUserServiceRequest() {
        log.info("[S] Registering userService with master");
        ServiceHelper helper = new ServiceHelper();

        try {
            HttpURLConnection con = sendRegistrationRequest();
            if (con.getResponseCode() == 200){
                JSONObject responseData = helper.stringToJsonObject(helper.readInputStream(con));
                addFrontendNodes(responseData);
                addUserServiceNodes(responseData);
                setUserData(responseData);
                setVersionValue(responseData);
                setUserIdValue(responseData);
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    /**
     * Method that sends a registration request to the master service
     * @return reponsetype
     * */
    private static HttpURLConnection sendRegistrationRequest() throws IOException {
        JSONObject body = new JSONObject();
        body.put("host", nodeInfo.getHost());
        body.put("port", nodeInfo.getPort());

        String url = "http://" + nodeInfo.getMasterHost() + ":" + nodeInfo.getMasterPort() + "/register/userservice";
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setDoOutput(true);
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-type", "application/json");
        OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
        wr.write(body.toString());
        wr.flush();
        wr.close();
        return con;

    }

    /** Adds all frontend nodes to frontend membership list */
    private static void addFrontendNodes(JSONObject responseData){
        JSONArray frontendServices = (JSONArray) responseData.get("frontends");

        Iterator i = frontendServices.iterator();
        while (i.hasNext()) {
            JSONObject node = (JSONObject) i.next();
            String host = node.get("host").toString();
            int port = Integer.valueOf(node.get("port").toString());
            log.info("[S] Adding new frontend " + host + ":" + port);
            NodeInfo info = new NodeInfo(port,host);
            frontendMemberData.addNode(info);
        }
    }

    /** Adds all secondaries nodes to secondaries membership list */
    private static void addUserServiceNodes(JSONObject responseData){
        JSONArray userservices = (JSONArray) responseData.get("userservices");

        Iterator i = userservices.iterator();
        while (i.hasNext()) {
            JSONObject node = (JSONObject) i.next();
            String host = node.get("host").toString();
            int port = Integer.valueOf(node.get("port").toString());
            log.info("[S] Adding new user service secondary " + host + ":" + port);
            NodeInfo info = new NodeInfo(port,host);
            secondariesMemberData.addNode(info);
        }
    }

    /** Sets user data to the same as the master
     * @param responseData userdata from master registration response
     */
    private static void setUserData(JSONObject responseData){
        log.info("[S] Setting userdata ");
        JSONObject users = (JSONObject) responseData.get("users");
        JSONArray userdata = (JSONArray) users.get("userdata");
        Iterator<JSONObject> it = userdata.iterator();
        while(it.hasNext()){
            JSONObject obj = it.next();
            String username = obj.get("username").toString();
            int userid = Integer.valueOf(obj.get("userid").toString());
            User newUser = new User(userid, username);
            userDataMap.addUser(userid, newUser);
            JSONArray ticketList = (JSONArray) obj.get("tickets");
            newUser.updateTicketArray(ticketList);
        }
    }

    /**
     * Sets the version number to the same as the master
     * @param responseData response data from master
     */
    private static void setVersionValue(JSONObject responseData){
        int value =Integer.valueOf(responseData.get("version").toString());
        version.set(value);
        log.info("[S] Setting version number as " + version.intValue());
    }
    /**
     * Sets the user id number to the same as the master
     * @param responseData response data from master
     */
    private static void setUserIdValue(JSONObject responseData){
        int value =Integer.valueOf(responseData.get("userID").toString());
        userid.set(value);
        log.info("[S] Setting user ID  as " + userid.intValue());
    }
}
