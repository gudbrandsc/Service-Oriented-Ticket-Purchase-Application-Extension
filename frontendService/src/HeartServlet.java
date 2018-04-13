import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class HeartServlet extends HttpServlet{
    /**
     * Do get method that handles all incoming get requests.
     * @param req incoming request
     * @param resp servlet Http response object
     * @throws IOException
     * @throws ServletException
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setStatus(HttpStatus.OK_200);

    }
}
