package sits.client;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import sits.networking.dto.RegistrationRequest;

class TournamentServerClientTest {

    private MockRestServiceServer server;
    private TournamentServerClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        client = new TournamentServerClient("http://localhost:8080", restTemplate);
    }

    @Test
    void listTournaments_callsCorrectUrl() throws Exception {
        server.expect(requestTo("http://localhost:8080/tournaments"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<?> result = client.listTournaments();

        server.verify();
        assertThat(result).isEmpty();
    }

    @Test
    void register_postsToCorrectUrl() throws Exception {
        server.expect(requestTo("http://localhost:8080/tournaments/t1/register"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess());

        client.register("t1", "Alice", "127.0.0.1", 9000);

        server.verify();
    }

    @Test
    void register_sendsCorrectBody() throws Exception {
        RegistrationRequest expected = new RegistrationRequest("Alice", "127.0.0.1", 9000);

        server.expect(requestTo("http://localhost:8080/tournaments/t1/register"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(objectMapper.writeValueAsString(expected)))
                .andRespond(withSuccess());

        client.register("t1", "Alice", "127.0.0.1", 9000);

        server.verify();
    }
}
