package sits.client;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockReset;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = ClientApp.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "tournament.server.url=http://localhost:9999",
        "tournament.id=test-tournament",
        "participant.name=TestBot"
})
class ClientAppIntegrationTest {

    @LocalServerPort
    private int port;

    @MockBean(reset = MockReset.NONE)
    private TournamentServerClient client;

    @Test
    void onReady_registersWithTournamentServer() {
        verify(client).register(eq("test-tournament"), eq("TestBot"), anyString(), anyInt());
    }

    // VEry valid concern :3
    @Test
    void portIsNonZeroAfterStartup() {
        assertThat(port).isGreaterThan(0);
    }
}
