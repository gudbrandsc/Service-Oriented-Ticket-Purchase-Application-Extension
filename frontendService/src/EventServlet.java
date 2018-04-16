
import org.eclipse.jetty.http.HttpStatus;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author Gudbrand Schistad, Omar Sharif
 * Servlet class that handles all get and post requests.
 */
public class EventServlet extends HttpServlet{
    private PropertiesLoader properties;
    private UserServiceMaster masterInfo;


    /** Constructor*/
    public EventServlet(PropertiesLoader properties, UserServiceMaster masterInfo){

        this.properties = properties;
        this.masterInfo = masterInfo;
    }

    /**
     * Do get method that handles all incoming get requests.
     * Require a event id to be specified in the request uri
     * @param req incoming request
     * @param resp servlet Http response object
     * @throws IOException
     * @throws ServletException
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String eventid = req.getPathInfo().substring(1);
        String eventidPattern = "([0-9]*)";

        PrintWriter printWriter = resp.getWriter();
        Pattern transfer = Pattern.compile(eventidPattern);
        Matcher matchTransfer = transfer.matcher(eventid);

        if(matchTransfer.matches()){
            JSONObject event = sendGetRequest(properties.getEventhost(), properties.getEventport(), matchTransfer.group(1), resp);
            printWriter.println(event);
            printWriter.flush();
        }else {
            resp.setStatus(HttpStatus.BAD_REQUEST_400);
        }
    }

    /**
     * Do post method that handles all incoming post requests.
     * This method supports both user creation and ticket purchase.
     * @param req incoming request
     * @param resp servlet Http response object
     * @throws IOException
     * @throws ServletException
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        String regex = "\\/([0-9]*)\\/purchase\\/([0-9]*)";

        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(pathInfo);

        if(pathInfo.equals("/create")){
            System.out.println(pathInfo);
            sendPostRequestAndPrint(properties.getEventhost(), properties.getEventport(), pathInfo, resp, req);
        }else if(m.matches()){
            int  eventid = Integer.parseInt(m.group(1));
            int  userid = Integer.parseInt(m.group(2));
            String path = "/"+ userid + "/tickets/add";
            JSONObject object = new JSONObject();
            JSONObject reqObj = stringToJsonObject(requestToString(req));
            object.put("tickets", reqObj.get("tickets"));
            object.put("eventid", eventid);
            System.out.println(masterInfo.getUserPort());
            System.out.println(object.toJSONString());
            sendPostRequest(masterInfo.getUserHost(),masterInfo.getUserPort(),path,resp,object.toJSONString());
        }else{
            resp.setStatus(HttpStatus.BAD_REQUEST_400);
        }
    }

    /**
     * Method used to read the body of a request
     * @param req servlet request
     * @return String of req body
     * @throws IOException
     */
    private String requestToString(HttpServletRequest req) throws IOException {
        String line;

        StringBuffer sb = new StringBuffer();
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
    private JSONObject stringToJsonObject(String json){
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
     * Method to read the inputstream and append it to a string
     * @param con between services
     * @return response string
     */
    private String readInputStream(HttpURLConnection con) throws IOException {
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
     * Method used to send a get request.
     * Builds a utl pattern, and opens a connection between the host.
     * @param host address of the receiver
     * @param port port number for the receiver
     * @param path path with parameters
     * @param resp response
     * @throws IOException
     */
    private JSONObject sendGetRequest(String host, String port, String path, HttpServletResponse resp) throws IOException {
        String url = "http://" + host + ":" + port + "/" + path;
        System.out.println(url);
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        int responseCode = con.getResponseCode();
        resp.setStatus(responseCode);
        if (responseCode == 200) { // Do not read input stream if the request was  not a success
            String response = readInputStream(con);
            return stringToJsonObject(response);
        }
        return null;
    }

    /**
     * Method used to send post requests.
     * Build url for target path
     * Sets application type, and opens connection.
     * @param host target host
     * @param port target port
     * @param path api path
     * @param resp Http response
     * @param req Http request
     * @throws IOException
     */
    private void sendPostRequest(String host, int port, String path, HttpServletResponse resp, HttpServletRequest req) throws IOException {
        String url = "http://" + host + ":" + port + "/" + path;
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setDoOutput(true);
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-type", "application/json");
        OutputStreamWriter wr =  new OutputStreamWriter(con.getOutputStream());
        wr.write(requestToString(req));
        wr.flush();

        resp.setStatus(con.getResponseCode());
    }

    /**
     * Method used to send post requests.
     * Build url for target path
     * Sets application type, and opens connection.
     * @param host target host
     * @param port target port
     * @param path api path
     * @param resp Http response
     * @param json json string
     * @throws IOException
     */
    private void sendPostRequest(String host, int port, String path, HttpServletResponse resp, String json) throws IOException {
        String url = "http://" + host + ":" + port + path;
        URL obj = new URL(url);
        System.out.println(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setDoOutput(true);
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-type", "application/json");
        OutputStreamWriter wr =  new OutputStreamWriter(con.getOutputStream());
        wr.write(json);
        wr.flush();
        resp.setStatus(con.getResponseCode());
    }

    /**
     * Method used to send post requests and print response.
     * Build url for target path
     * Sets application type, and opens connection.
     * @param host target host
     * @param port target port
     * @param path api path
     * @param resp Http response
     * @param req Http request
     * @throws IOException
     */
    private void sendPostRequestAndPrint(String host, String port, String path, HttpServletResponse resp, HttpServletRequest req) throws IOException {
        String url = "http://" + host + ":" + port + path;
        URL obj = new URL(url);
        System.out.println(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setDoOutput(true);
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-type", "application/json");
        OutputStreamWriter wr =  new OutputStreamWriter(con.getOutputStream());
        wr.write(requestToString(req));
        wr.flush();
        resp.setStatus(con.getResponseCode());
        if (con.getResponseCode() == 200){
            PrintWriter writer = resp.getWriter();
            writer.println(readInputStream(con));
            writer.flush();
        }
    }

}
