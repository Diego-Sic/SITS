package sits.client;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class ClientApp {

    @Value("${tournament.id}")
    private String tournamentId;

    @Value("${participant.name}")
    private String participantName;

    // Allows callers on a remote machine to override the IP advertised to the server.
    // InetAddress.getLocalHost() resolves to 127.0.0.1 on most Linux setups, so without
    // this override the server would store a loopback address and fail to reach this client.
    @Value("${participant.host:}")
    private String participantHost;

    @Autowired
    private TournamentServerClient client;

    public static void main(String[] args) {
        SpringApplication.run(ClientApp.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady(ApplicationReadyEvent event) throws UnknownHostException {
        if (!(event.getApplicationContext() instanceof WebServerApplicationContext ctx)) return;
        int port = ctx.getWebServer().getPort();
        String ip = participantHost.isBlank() ? InetAddress.getLocalHost().getHostAddress() : participantHost;
        client.register(tournamentId, participantName, ip, port);
    }
}
