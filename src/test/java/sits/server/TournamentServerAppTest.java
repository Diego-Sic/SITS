package sits.server;

import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

@SpringBootTest(classes = TournamentServerApp.class, webEnvironment = WebEnvironment.RANDOM_PORT)
class TournamentServerAppTest {

    @Autowired
    private TournamentRegistry registry;
    @Autowired
    private ExecutorService executorService;

    @Test
    void context_loads() {
        // @SpringBootTest failing to load the context counts as a test failure, so no
        // assertion is needed
        // Pretty sad if this doesn't work ngl
    }

    @Test
    void executorService_beanIsCreated() {
        assertThat(executorService).isNotNull();
    }

    @Test
    void seedTournaments_registersAndStartsIpdTournament() {
        NetworkedTournament t = registry.get("ipd-01");
        assertThat(t).isNotNull();
        assertThat(t.getName()).isEqualTo("IPD Tournament");
        // Tournament auto-starts on boot — it will be RUNNING or COMPLETED, never REGISTERING
        assertThat(t.getStatus()).isIn(TournamentStatus.RUNNING, TournamentStatus.COMPLETED);
    }
}
