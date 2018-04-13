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

public class ServletHelper {

    public ServletHelper() {
    }

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

    public void sendPostRequest(NodeInfo nodeInfo, String path, String body) {
        try {

            String url = "http://" + nodeInfo.getHost() + ":" + nodeInfo.getPort() + path;
            System.out.println(url);
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-type", "application/json");
            OutputStreamWriter wr =  new OutputStreamWriter(con.getOutputStream());
            wr.write(body);
            wr.flush();
            wr.close();
            con.getResponseCode();
        } catch (IOException e) {
            // e.printStackTrace();
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
}
