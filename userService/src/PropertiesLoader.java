
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesLoader {
    private String eventhost;
    private String eventport;


    public PropertiesLoader() {
        loadProperties();

    }

    private void loadProperties() {
        Properties prop = new Properties();
        InputStream input = null;
        try {

            input = new FileInputStream("./userService/config.properties");

            // load a properties file
            prop.load(input);
            this.eventhost = prop.getProperty("eventhost");
            this.eventhost = prop.getProperty("eventport");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public String getEventhost() {
        return eventhost;
    }

    public String getEventport() {
        return eventport;
    }
}