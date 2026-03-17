package sits.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

@SpringBootTest(classes = TournamentServerApp.class, webEnvironment = WebEnvironment.RANDOM_PORT)
class TournamentServerAppTest {

    @Autowired
    private TournamentRegistry registry;

    @Test
    void context_loads() {
        // Spring context must start successfully — no assertion needed
    }

    @Test
    void seedTournaments_registersOneTournament() {
        List<NetworkedTournament> registering = registry.listRegistering();
        assertThat(registering).hasSize(1);
        assertThat(registering.get(0).getId()).isEqualTo("ipd-01");
        assertThat(registering.get(0).getName()).isEqualTo("IPD Tournament");
    }
}
