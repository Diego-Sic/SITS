package sits.server;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import sits.games.ipd.IteratedPrisonersDilemma;
import sits.games.ipd.PrisonerAction;
import sits.games.ipd.TitForTat;
import sits.tournament.RoundRobin;

class TournamentRegistryTest {

    private TournamentRegistry registry;

    private NetworkedTournament makeTournament(String id) {
        return new NetworkedTournament(
                id, "Tournament " + id,
                new RoundRobin(),
                new IteratedPrisonersDilemma(1),
                List.of(new TitForTat()),
                PrisonerAction::valueOf);
    }

    @BeforeEach
    void setUp() {
        registry = new TournamentRegistry();
    }

    @Test
    void add_and_get_byId() {
        NetworkedTournament t = makeTournament("t1");
        registry.add(t);
        assertThat(registry.get("t1")).isSameAs(t);
    }

    @Test
    void get_unknownId_returnsNull() {
        assertThat(registry.get("nonexistent")).isNull();
    }

    @Test
    void listRegistering_includesRegistering() {
        NetworkedTournament t = makeTournament("t1");
        registry.add(t);
        assertThat(registry.listRegistering()).contains(t);
    }

    @Test
    void listRegistering_excludesRunning() {
        NetworkedTournament t = makeTournament("t2");
        t.setStatus(TournamentStatus.RUNNING);
        registry.add(t);
        assertThat(registry.listRegistering()).doesNotContain(t);
    }

    @Test
    void listRegistering_excludesCompleted() {
        NetworkedTournament t = makeTournament("t3");
        t.setStatus(TournamentStatus.COMPLETED);
        registry.add(t);
        assertThat(registry.listRegistering()).doesNotContain(t);
    }

    @Test
    void listActive_includesRegistering() {
        NetworkedTournament t = makeTournament("t4");
        registry.add(t);
        assertThat(registry.listActive()).contains(t);
    }

    @Test
    void listActive_includesRunning() {
        NetworkedTournament t = makeTournament("t5");
        t.setStatus(TournamentStatus.RUNNING);
        registry.add(t);
        assertThat(registry.listActive()).contains(t);
    }

    @Test
    void listActive_excludesCompleted() {
        NetworkedTournament t = makeTournament("t6");
        t.setStatus(TournamentStatus.COMPLETED);
        registry.add(t);
        assertThat(registry.listActive()).doesNotContain(t);
    }
}
