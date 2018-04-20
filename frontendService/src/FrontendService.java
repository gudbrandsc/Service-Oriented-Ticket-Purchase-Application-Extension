import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.*;

/**
 * @Author Gudbrand Schistad, Omar Sharif
 * Class that starts the frontend service server, and maps all requests to the correct servlet.
 */
public class FrontendService {
    private  static Logger log = LogManager.getLogger();

    public static void main(String[] args) {
        int port = 0;
        String masterHost = ""; // URL for user service
        int masterPort = 0;

        if(args.length == 6 && args[0].equals("-port") && args[2].equals("-masterHost") && args[4].equals("-masterPort")){
            port = Integer.parseInt(args[1]);
            masterHost = args[3];
            masterPort = Integer.parseInt(args[5]);
        }else{
            log.info("Invalid arguments. \nFormat: -port **** -masterURL *****");
            System.exit(-1);
        }

        UserServiceMaster masterInfo = new UserServiceMaster(masterHost, masterPort);
        PropertiesLoader properties = new PropertiesLoader();


        Server server = new Server(port);
        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);
        handler.addServletWithMapping(new ServletHolder(new GetEventsServlet(properties)), "/events");
        handler.addServletWithMapping(new ServletHolder(new EventServlet(properties, masterInfo)), "/events/*");
        handler.addServletWithMapping(new ServletHolder(new UserServlet(properties, masterInfo)), "/users/*");
        handler.addServletWithMapping(new ServletHolder(new MasterHandler(masterInfo)), "/election");
        handler.addServletWithMapping(HeartServlet.class, "/alive");


        if(!registerFrontendRequest(masterHost, masterPort,"/register/frontend", port)) {
            log.info("Registration failed..");
            System.exit(-1);

        }

        log.info("Starting server on port " + port + "...");

        try {
            server.start();
            server.join();
        } catch (Exception ex) {
            log.info("Interrupted while running server.");
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
    private static boolean registerFrontendRequest(String masterHost, int masterPort, String path, int port) {
        log.info("Registering frontend with master...");
        JSONObject body = new JSONObject();
        try {
            body.put("host", InetAddress.getLocalHost().getHostAddress());
            body.put("port", port);

            String url = "http://" + masterHost + ":" + masterPort + path;
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-type", "application/json");
            OutputStreamWriter wr =  new OutputStreamWriter(con.getOutputStream());
            wr.write(body.toString());
            wr.flush();
            con.getResponseCode();
        } catch (IOException e) {
            // e.printStackTrace();
            return false;
        }
        return true;
    }
}
