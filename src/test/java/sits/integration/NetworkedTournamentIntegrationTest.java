package sits.integration;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import sits.games.ipd.IteratedPrisonersDilemma;
import sits.games.ipd.PrisonerAction;
import sits.networking.dto.RegistrationRequest;
import sits.server.NetworkedTournament;
import sits.server.TournamentRegistry;
import sits.server.TournamentServerApp;
import sits.server.TournamentStatus;
import sits.tournament.RoundRobin;

@SpringBootTest(classes = TournamentServerApp.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
class NetworkedTournamentIntegrationTest {

    @Autowired
    private TournamentRegistry registry;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private HttpServer stubClient1;
    private HttpServer stubClient2;

    @BeforeEach
    void startStubClients() throws IOException {
        stubClient1 = buildStubServer("COOPERATE");
        stubClient2 = buildStubServer("DEFECT");
        registry.add(new NetworkedTournament(
                "ipd-01", "IPD Tournament",
                new RoundRobin(), new IteratedPrisonersDilemma(10),
                List.of(), PrisonerAction::valueOf));
    }

    @AfterEach
    void stopStubClients() {
        if (stubClient1 != null)
            stubClient1.stop(0);
        if (stubClient2 != null)
            stubClient2.stop(0);
    }

    @Test
    void twoRemoteClientsCompleteIPDTournament() throws Exception {
        // 1. GET /tournaments returns one REGISTERING tournament
        ResponseEntity<String> listResponse = restTemplate.getForEntity("/tournaments", String.class);
        assertThat(listResponse.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode list = objectMapper.readTree(listResponse.getBody());
        assertThat(list.size()).isEqualTo(1);

        // 2. Register two remote participants
        int port1 = stubClient1.getAddress().getPort();
        int port2 = stubClient2.getAddress().getPort();
        ResponseEntity<Void> r1 = restTemplate.postForEntity("/tournaments/ipd-01/register",
                new RegistrationRequest("RemoteCooperator", "127.0.0.1", port1), Void.class);
        ResponseEntity<Void> r2 = restTemplate.postForEntity("/tournaments/ipd-01/register",
                new RegistrationRequest("RemoteDefector", "127.0.0.1", port2), Void.class);
        assertThat(r1.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(r2.getStatusCode().is2xxSuccessful()).isTrue();

        // 3. Start tournament — now returns 202 Accepted (runs on background thread),
        // this is what we commented on the SSE modifications needed
        ResponseEntity<Void> startResponse = restTemplate.postForEntity(
                "/tournaments/ipd-01/start", null, Void.class);
        assertThat(startResponse.getStatusCode().value()).isEqualTo(202);

        // Wait up to 5 s for the background thread to finish, I decided 5 seconds to be
        // always consistent, I tried 2 but sometimes it was not enough
        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> registry.get("ipd-01").getStatus() == TournamentStatus.COMPLETED);

        // 4. TournamentResult has results for all participant pairs
        // 2 participants (2 remote) → 1 pair: (RemoteCooperator, RemoteDefector)
        NetworkedTournament tournament = registry.get("ipd-01");
        assertThat(tournament.getResult().getResults()).hasSize(1);

        // 5. Tournament status is COMPLETED
        assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.COMPLETED);
    }

    private HttpServer buildStubServer(String actionLabel) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        // This function just reads the request body (to simulate the client doing
        // something with it) and then responds with a fixed action label
        server.createContext("/action", exchange -> {
            exchange.getRequestBody().readAllBytes();
            byte[] response = actionLabel.getBytes();
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        server.createContext("/reset", exchange -> {
            exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(200, -1);
            exchange.getResponseBody().close();
        });
        server.start();
        return server;
    }
}
