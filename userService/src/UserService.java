import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author Gudbrand Schistad
 * Class that starts the user service server, and maps all requests to the correct servlet.
 */
public class UserService {
    private static volatile int userid = 1;
    private static NodeInfo nodeInfo;
    private static PropertiesLoader properties;
    private static FrontendNodeData frontendNodeData;
    private static UserServiceNodeData userServiceNodeData;
    private static UserDataMap userDataMap;
    private static NodeElector nodeElector;

    public static void main(String[] args) {
        AtomicInteger version = new AtomicInteger(0);
        nodeInfo = null;
        properties = new PropertiesLoader();
        frontendNodeData = new FrontendNodeData();
        userServiceNodeData = new UserServiceNodeData();
        userDataMap = new UserDataMap();

        if(args.length >= 2 && args[0].equals("-port")){
            try {
                nodeInfo = new NodeInfo(Integer.parseInt(args[1]), InetAddress.getLocalHost().getHostAddress());

                if(args[2].equals("-setAsMaster")) {
                    nodeInfo.setAsMaster();
                    version.set(9);
                }else if (args.length >= 6 && args[2].equals("-masterHost") && args[4].equals("-masterPort")){
                    nodeInfo.updateMaster(args[3], Integer.parseInt(args[5]));
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }else{
            System.out.println("Invalid arguments. \nFormat: -port **** -masterURL *****");
            System.exit(-1);
        }


        Server server = new Server(nodeInfo.getPort());
        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);
        handler.addServletWithMapping(new ServletHolder(new UserServiceServlet(userDataMap, userid, userServiceNodeData, nodeInfo,properties, version)), "/*");
        handler.addServletWithMapping(new ServletHolder(new NodeRegistationServlet(nodeInfo, frontendNodeData, userServiceNodeData, userDataMap, version)), "/register/*");
        handler.addServletWithMapping(HeartServlet.class, "/alive");
        handler.addServletWithMapping(new ServletHolder(new NodeRemoverServlet(userServiceNodeData, frontendNodeData)), "/remove/*");
        handler.addServletWithMapping(new ServletHolder(new ElectionServlet(nodeInfo,nodeElector, userServiceNodeData, userDataMap, version)), "/election/*");


        if(!nodeInfo.isMaster()){
            String path = "/register/userservice";
            boolean success = registerUserServiceRequest(nodeInfo, path, frontendNodeData, userServiceNodeData, userDataMap, version);
            if(!success){
                System.out.println("Unable to register node with master");
                System.exit(-1);
            }
        }
        nodeElector = new NodeElector(userServiceNodeData, nodeInfo, frontendNodeData, userDataMap, version);

        System.out.println("Starting server on port " + nodeInfo.getPort() + "...");
        System.out.println("Server is master: " + nodeInfo.isMaster() + "...");
        new Thread(new HeartBeat(nodeInfo, userServiceNodeData, frontendNodeData, nodeElector)).start();


        try {
            server.start();
            server.join();
            System.out.println("Exiting...");
        }
        catch (Exception ex) {
            System.out.println("Interrupted while running server.");
            System.exit(-1);
        }
    }

    /**
     * Method used to send post requests.
     * Build url for target path
     * Sets application type, and opens connection.
     * @param path api path
     * @throws IOException
     */
    private static boolean registerUserServiceRequest(NodeInfo nodeInfo, String path, FrontendNodeData frontendNodeData, UserServiceNodeData userServiceNodeData, UserDataMap userDataMap, AtomicInteger version) {
        System.out.println("[S] Registering userService with master");
        JSONObject body = new JSONObject();
        ServletHelper helper = new ServletHelper();
        //TODO Get atomic integer
        try {
            body.put("host", nodeInfo.getHost());
            body.put("port", nodeInfo.getPort());

            String url = "http://" + nodeInfo.getMasterHost() + ":" + nodeInfo.getMasterPort() + path;
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-type", "application/json");
            OutputStreamWriter wr =  new OutputStreamWriter(con.getOutputStream());
            wr.write(body.toString());
            wr.flush();
            wr.close();
            if (con.getResponseCode() == 200){
                //Add response
                JSONObject responseData = helper.stringToJsonObject(helper.readInputStream(con));
                addFrontendNodes(responseData, frontendNodeData);
                addUserServiceNodes(responseData, userServiceNodeData);
                setUserData(responseData, userDataMap);
                setVersionValue(responseData, version);
            }
        } catch (IOException e) {
            // e.printStackTrace();
            return false;
        }
        return true;
    }

    private static void addFrontendNodes(JSONObject responseData, FrontendNodeData frontendNodeData){
        JSONArray frontendServices = (JSONArray) responseData.get("frontends");

        Iterator i = frontendServices.iterator();
        while (i.hasNext()) {
            JSONObject node = (JSONObject) i.next();
            String host = node.get("host").toString();
            int port = Integer.valueOf(node.get("port").toString());
            System.out.println("[S] Adding new frontend " + host + ":" + port);
            NodeInfo info = new NodeInfo(port,host);
            frontendNodeData.addNode(info);
        }
    }

    private static void addUserServiceNodes(JSONObject responseData, UserServiceNodeData userServiceNodeData){
        JSONArray userservices = (JSONArray) responseData.get("userservices");

        Iterator i = userservices.iterator();
        while (i.hasNext()) {
            JSONObject node = (JSONObject) i.next();
            String host = node.get("host").toString();
            int port = Integer.valueOf(node.get("port").toString());
            System.out.println("[S] Adding new user service secondary " + host + ":" + port);
            NodeInfo info = new NodeInfo(port,host);
            userServiceNodeData.addNode(info);
        }
    }

    private static void setUserData(JSONObject responseData, UserDataMap userDataMap){
        System.out.println("[S] Setting userdata ");
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
    private static void setVersionValue(JSONObject responseData, AtomicInteger version){
        int value =Integer.valueOf(responseData.get("version").toString());
        version.set(value);
        System.out.println("[S] Setting version number as " + version.intValue());

    }
}
