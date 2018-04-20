import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author Gudbrand Schistad
 * Servlet class that handles all get and post requests.
 */
public class GetEventsServlet extends HttpServlet {
    private PropertiesLoader properties;

    /** Constructor */
    public GetEventsServlet(PropertiesLoader properties){
        this.properties = properties;
    }

    /**
     * Method that handles requests to get all events
     * @param resp http response
     * @param req  http request
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String inputLine, url;
        int responseCode;

        PrintWriter printWriter = resp.getWriter();
        url = "http://" + properties.getEventHost() + ":" + properties.getEventPort() + "/list";
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        responseCode = con.getResponseCode();
        resp.setStatus(responseCode);
        if (responseCode == 200) {
            StringBuilder response = new StringBuilder();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            in.close();
            printWriter.println(response.toString());
            printWriter.flush();
        }
    }
}
