import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.servlet.ServletHolder;
import org.json.simple.JSONArray;
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
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author Gudbrand Schistad, Omar Sharif
 * Servlet class that handles all get and post requests.
 */
public class UserServlet extends HttpServlet{
    private PropertiesLoader properties;
    private UserServiceMaster masterInfo;
    //private ServletHolder servletHolder;

    /** Constructor */
    public UserServlet(PropertiesLoader properties, UserServiceMaster masterInfo){

        this.properties = properties;
        this.masterInfo = masterInfo;
    }

    /**
     * Do get method that handles all incoming get requests.
     * @param req incoming request
     * @param resp servlet Http response object
     * @throws IOException
     * @throws ServletException
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter printWriter = resp.getWriter();
        JSONObject userObject = sendGetRequest(masterInfo.getUserHost(), masterInfo.getUserPort() , req.getPathInfo().substring(1), resp);
        if(userObject != null){
            JSONObject json = new JSONObject();
            JSONArray eventarray = (JSONArray) userObject.get("tickets");
            JSONArray updatedEventarray = new JSONArray();
            Iterator<JSONObject> iterator = eventarray.iterator();

            json.put("userid", userObject.get("userid"));
            json.put("username", userObject.get("username"));

            while (iterator.hasNext()) {
                JSONObject res = iterator.next();
                long eventid = Long.parseLong(res.get("eventid").toString());

                JSONObject eventObject = sendGetRequest(properties.getEventhost(), Integer.parseInt(properties.getEventport()), String.valueOf(eventid), resp);
                if (eventObject != null) {
                    updatedEventarray.add(eventObject);
                }
            }

            json.put("tickets", updatedEventarray);
            resp.setContentType("application/json; charset=UTF-8");
            resp.setStatus(HttpStatus.OK_200);
            printWriter.println(json.toString());
            printWriter.flush();
        }else {
            resp.setStatus(HttpStatus.BAD_REQUEST_400);
        }
    }

    /**
     * Do post method that handles all incoming post requests.
     * If request do not match the any pattern set response status  400
     * @param req incoming request
     * @param resp servlet Http response object
     * @throws IOException
     * @throws ServletException
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String pathInfo = req.getPathInfo();
        String transferTicketPattern = "\\/([0-9]*)\\/tickets\\/transfer";
        Pattern transfer = Pattern.compile(transferTicketPattern);
        Matcher matchTransfer = transfer.matcher(pathInfo);

        if(pathInfo.equals("/create")){
            System.out.println("Send post to : "+masterInfo.getUserHost()+":"+masterInfo.getUserPort());
            sendPostRequestAndPrint(masterInfo.getUserHost(), masterInfo.getUserPort(), pathInfo, resp, req);
        }else if(matchTransfer.matches()){
            String path =  "/" + matchTransfer.group(1) + "/tickets/transfer";
            sendPostRequest(masterInfo.getUserHost(), masterInfo.getUserPort(), path, resp, req);
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
     * @param masterHost address of the receiver
     * @param masterPort port for the master
     * @param path path with parameters
     * @param resp response
     * @throws IOException
     */
    private JSONObject sendGetRequest(String masterHost, int masterPort, String path, HttpServletResponse resp) throws IOException {
        String url = "http://" + masterHost + ":" + masterPort + "/" + path;
        System.out.println(url);
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        int responseCode = con.getResponseCode();
        resp.setStatus(responseCode);

        if (responseCode == 200) {
            String response = readInputStream(con);
            return stringToJsonObject(response.toString());
        }
        return null;
    }

    /**
     * Method used to send post requests.
     * Build url for target path
     * Sets application type, and opens connection.
     * @param path api path
     * @param resp Http response
     * @param req Http request
     * @throws IOException
     */
    private void sendPostRequest(String masterHost, int masterPort, String path, HttpServletResponse resp, HttpServletRequest req) throws IOException {
        String url = "http://" + masterHost + ":" + masterPort + path;
        System.out.println(url);
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
     * Method used to send post requests and print response.
     * Build url for target path
     * Sets application type, and opens connection.
     * @param masterHost target host
     * @param path api path
     * @param resp Http response
     * @param req Http request
     * @throws IOException
     */
    private void sendPostRequestAndPrint(String masterHost, int masterPort, String path, HttpServletResponse resp, HttpServletRequest req) throws IOException {
        String url = "http://" + masterHost + ":" + masterPort + path;
        URL obj = new URL(url);
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

