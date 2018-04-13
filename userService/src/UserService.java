import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

/**
 * @Author Gudbrand Schistad
 * Class that starts the user service server, and maps all requests to the correct servlet.
 */
public class UserService {
    private static volatile int userid = 1;

    public static void main(String[] args) {
        int port = 0;
        String masterHost = ""; // URL for user service
        int masterPort = 0;
        int  nodeId = 1;
        NodeInfo nodeInfo = null;

        if(args.length >= 2 && args[0].equals("-port")){
            try {
                nodeInfo = new NodeInfo(Integer.parseInt(args[1]), InetAddress.getLocalHost().getHostAddress());

                if(args[2].equals("-setAsMaster")) {
                    nodeInfo.setAsMaster();

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

        PropertiesLoader properties = new PropertiesLoader();
        FrontendNodeData frontendNodeData = new FrontendNodeData();
        UserServiceNodeData userServiceNodeData = new UserServiceNodeData();

        if(!nodeInfo.isMaster()){
            String path = "/register/userservice";
            registerUserServiceRequest(nodeInfo, path);
        }
        System.out.println(nodeInfo.getPort());
        Server server = new Server(nodeInfo.getPort());
        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);
        UserDataMap userDataMap = new UserDataMap();
        handler.addServletWithMapping(new ServletHolder(new UserServiceServlet(userDataMap, userid, properties)), "/*");
        handler.addServletWithMapping(new ServletHolder(new NodeRegistationServlet(nodeInfo, frontendNodeData, userServiceNodeData)), "/register/*");

        System.out.println("Starting server on port " + nodeInfo.getPort() + "...");
        System.out.println("Server is master: " + nodeInfo.isMaster() + "...");


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
     * @param masterHost target host
     * @param path api path
     * @throws IOException
     */
    private static boolean registerUserServiceRequest(NodeInfo nodeInfo, String path) {
        System.out.println("Registering userService with master...");
        JSONObject body = new JSONObject();
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
            System.out.println(con.getResponseCode());
            con.getResponseCode();
        } catch (IOException e) {
            // e.printStackTrace();
            return false;
        }
        return true;
    }
}
