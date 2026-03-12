package sits.networking;

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

import sits.core.Action;
import sits.core.GameHistory;
import sits.core.Participant;

class RemoteParticipantTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private RemoteParticipant participant;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        participant = new RemoteParticipant("Alice", "http://localhost:9000",
                label -> new StringAction(label), restTemplate);
    }

    @Test
    void implementsParticipant() {
        assertThat(participant).isInstanceOf(Participant.class);
    }

    @Test
    void getName_returnsNameWithoutHttp() {
        assertThat(participant.getName()).isEqualTo("Alice");
        mockServer.verify();
    }

    @Test
    void chooseAction_postsToClientUrl() {
        mockServer.expect(requestTo("http://localhost:9000/action"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("COOPERATE", MediaType.TEXT_PLAIN));

        GameHistory history = new GameHistory("Alice", "Bob");
        participant.chooseAction(history);

        mockServer.verify();
    }

    @Test
    void chooseAction_appliesFactoryToLabel() {
        mockServer.expect(requestTo("http://localhost:9000/action"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("DEFECT", MediaType.TEXT_PLAIN));

        GameHistory history = new GameHistory("Alice", "Bob");
        Action action = participant.chooseAction(history);

        assertThat(action.getLabel()).isEqualTo("DEFECT");
    }

    @Test
    void chooseAction_serializesHistoryAsDTO() {
        mockServer.expect(requestTo("http://localhost:9000/action"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess("COOPERATE", MediaType.TEXT_PLAIN));

        GameHistory history = new GameHistory("Alice", "Bob");
        participant.chooseAction(history);

        mockServer.verify();
    }

    @Test
    void reset_postsToResetEndpoint() {
        mockServer.expect(requestTo("http://localhost:9000/reset"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess());

        participant.reset();

        mockServer.verify();
    }
}
