import org.eclipse.jetty.http.HttpStatus;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

public class MasterHandler extends HttpServlet {
    private UserServiceMaster masterInfo;

    public MasterHandler(UserServiceMaster masterInfo) {
        this.masterInfo = masterInfo;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        JSONObject requestBody = stringToJsonObject(requestToString(req));
        masterInfo.setNewMaster(requestBody.get("host").toString(), Integer.valueOf(requestBody.get("port").toString()));
        resp.setStatus(HttpStatus.OK_200);

    }

    /**
     * Method used to read the body of a request
     * @param req servlet request
     * @return String of req body
     * @throws IOException
     */
    private String requestToString(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
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
}
