package sits.networking.dto;

public class RegistrationRequest {

    public String name;
    public String ip;
    public int port;

    public RegistrationRequest() {}

    public RegistrationRequest(String name, String ip, int port) {
        this.name = name;
        this.ip = ip;
        this.port = port;
    }
}
