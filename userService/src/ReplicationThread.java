/**
 * @author Gudbrand Schistad
 * Thread class used by master userservice to replicate a write request to a secondary
 */
class ReplicationThread implements Runnable {
    private String host;
    private String path;
    private String requestBody;
    private int port;
    private ServiceHelper serviceHelper;

    /** Constructor */
    public ReplicationThread(String host, int port, String path, String requestBody) {
        this.host = host;
        this.path = path;
        this.requestBody = requestBody;
        this.port = port;
        this.serviceHelper = new ServiceHelper();
    }

    /** Run method that sends a write request to a secondary */
    @Override
    public void run() {
        serviceHelper.sendPostRequest(host, port, path, requestBody);
    }
}
