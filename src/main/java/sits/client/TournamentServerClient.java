package sits.client;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.client.RestTemplate;

import sits.networking.dto.RegistrationRequest;
import sits.server.NetworkedTournament;

public class TournamentServerClient {

    private final String serverUrl;
    private final RestTemplate restTemplate;

    public TournamentServerClient(String serverUrl) {
        this.serverUrl = serverUrl;
        this.restTemplate = new RestTemplate();
    }

    public TournamentServerClient(String serverUrl, RestTemplate restTemplate) {
        this.serverUrl = serverUrl;
        this.restTemplate = restTemplate;
    }

    public List<NetworkedTournament> listTournaments() {
        NetworkedTournament[] result = restTemplate.getForObject(serverUrl + "/tournaments", NetworkedTournament[].class);
        return result != null ? Arrays.asList(result) : List.of();
    }

    public void register(String tournamentId, String name, String ip, int port) {
        RegistrationRequest req = new RegistrationRequest(name, ip, port);
        restTemplate.postForObject(serverUrl + "/tournaments/" + tournamentId + "/register", req, Void.class);
    }
}
