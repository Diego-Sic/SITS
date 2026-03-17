package sits.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

@SpringBootTest(classes = TournamentServerApp.class, webEnvironment = WebEnvironment.RANDOM_PORT)
class TournamentServerAppTest {

    @Test
    void context_loads() {
        // @SpringBootTest failing to load the context counts as a test failure, so no assertion is needed
        // Pretty sad if this doesn't work ngl
    }
}
