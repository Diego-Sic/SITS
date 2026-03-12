package sits.server;

import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import sits.core.Action;
import sits.core.Participant;
import sits.core.TournamentResult;
import sits.games.ipd.IteratedPrisonersDilemma;
import sits.games.ipd.PrisonerAction;
import sits.games.ipd.TitForTat;
import sits.networking.dto.RegistrationRequest;
import sits.tournament.RoundRobin;

class NetworkedTournamentTest {

    // I love re using things that I built :3
    private static final Function<String, Action> FACTORY = PrisonerAction::valueOf;

    private final IteratedPrisonersDilemma game = new IteratedPrisonersDilemma(1);
    private final RoundRobin format = new RoundRobin();
    private final Participant localParticipant = new TitForTat();
    private final RegistrationRequest remoteReq = new RegistrationRequest("Remote", "127.0.0.1", 9999);

    private NetworkedTournament tournament;

    @BeforeEach
    void setUp() {
        tournament = new NetworkedTournament("t1", "Test Tournament", format, game,
                List.of(localParticipant), FACTORY);
    }

    @Test
    void initialStatus_isRegistering() {
        assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.REGISTERING);
    }

    @Test
    void addRemoteParticipant_addsParticipant() {
        tournament.addRemoteParticipant(remoteReq);
        assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.REGISTERING);
    }

    @Test
    void addRemoteParticipant_throwsWhenRunning() {
        // We couldn't find an easier way to test the RUNNING guard without either
        // spinning up threads (complex) or making start() async (architectural change).
        // Using the package-private setStatus() to force the state directly.
        tournament.setStatus(TournamentStatus.RUNNING);

        assertThatThrownBy(() -> tournament.addRemoteParticipant(remoteReq))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void addRemoteParticipant_throwsWhenCompleted() {
        tournament.start();
        assertThatThrownBy(() -> tournament.addRemoteParticipant(remoteReq))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void start_throwsWhenAlreadyRunning() {
        tournament.start();
        assertThatThrownBy(() -> tournament.start())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void start_transitionsToCompleted() {
        tournament.start();
        assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.COMPLETED);
    }

    @Test
    void start_includesLocalAndRemoteParticipants() {
        List<?>[] captured = new List<?>[1];

        NetworkedTournament t = new NetworkedTournament(
                "t3", "Capture",
                (participants, g) -> {
                    captured[0] = participants;
                    return new TournamentResult(List.of());
                },
                game, List.of(localParticipant), FACTORY);

        t.addRemoteParticipant(remoteReq);
        t.start();

        assertThat(captured[0]).hasSize(2);
    }
}
