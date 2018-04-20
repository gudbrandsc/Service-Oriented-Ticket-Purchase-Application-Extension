import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Gudbrand Schistad
 * Class used to avoid duplicate code
 */
public class ServiceHelper {
    private  static Logger log = LogManager.getLogger();


    /**
     * Method used to read the body of a request
     * @param req servlet request
     * @return String of req body
     * @throws IOException
     */
    public String requestToString(HttpServletRequest req) throws IOException {
        StringBuffer sb = new StringBuffer();
        String line;

        BufferedReader in = req.getReader();

        while ((line = in.readLine()) != null) {
            sb.append(line);
        }
        String res = sb.toString();
        in.close();
        return res;
    }

    /**
     * Method that parses a json string and returns a json object
     * @param json String representation of json
     * @return JSONObject
     */
    public JSONObject stringToJsonObject(String json){
        JSONObject obj = null;
        JSONParser parser = new JSONParser();
        try {
            obj = (JSONObject)parser.parse(json);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return obj;
    }
    /**
     * Method that sends a post request and returns the http response
     * @return http response status
     */
    public int sendPostRequest(String host, int port, String path, String body) {
        try {
            String url = "http://" + host + ":" + port + path;
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-type", "application/json");
            OutputStreamWriter wr =  new OutputStreamWriter(con.getOutputStream());
            wr.write(body);
            wr.flush();
            wr.close();
           return con.getResponseCode();
        } catch (IOException e) {
            return 401;
        }
    }

    /**
     * Method to read the inputstream and append it to a string
     * @param con between services
     * @return response string
     */
    public static String readInputStream(HttpURLConnection con) throws IOException {
        StringBuffer response = new StringBuffer();

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        return response.toString();
    }

    /**
     * Method used by the master service to replicate a write operation to all secondaries
     * Waits for all threads to either finish or timeout before responding.
     * If a secondary is not responding to the request, it will be removed by the heartbeat
     */
    public int replicateToSecondaries(String path, String body, SecondariesMemberData secondariesMemberData){
        log.info("[P] Replicating to all secondaries");
        List<NodeInfo> secondaryCopy = secondariesMemberData.getUserServicesListCopy();
        List<Thread> replicationThreads = new ArrayList<>();

        for(NodeInfo secondary : secondaryCopy){
            Thread t = new Thread(new ReplicationThread(secondary.getHost(), secondary.getPort(), path, body));
            t.start();
            replicationThreads.add(t);
        }

        for(Thread t : replicationThreads){
            try {
                t.join();
            } catch (InterruptedException e) {
                log.info("[P] Unable to replicate to a secondary still returning 200");
            }
        }
        return 200;
    }

    /**
     * Method that sends a get request and returns the Http status code
     * @param host of target
     * @param port of target
     */
    public int sendGetAndReturnStatus(String host, int port, String path){
        try {
            String url = "http://" + host + ":" + port + "/" + path;
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            return con.getResponseCode();

        } catch (IOException e) {
            return 401;
        }
    }
}
