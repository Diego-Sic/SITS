package sits.tournament;

import org.junit.jupiter.api.Test;
import sits.core.GameObserver;
import sits.core.GameResult;
import sits.core.MoveEvent;
import sits.core.TournamentResult;
import sits.games.ipd.AlwaysCooperate;
import sits.games.ipd.AlwaysDefect;
import sits.games.ipd.IteratedPrisonersDilemma;
import sits.games.ipd.TitForTat;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoundRobinTournamentOverTest {

    static class CapturingObserver implements GameObserver {
        List<TournamentResult> tournamentResults = new ArrayList<>();

        @Override public void onMoveMade(MoveEvent event) {}
        @Override public void onGameOver(GameResult result) {}
        @Override public void onTournamentOver(TournamentResult result) {
            tournamentResults.add(result);
        }
    }

    @Test
    void onTournamentOverFiresExactlyOnce() {
        RoundRobin rr = new RoundRobin();
        CapturingObserver observer = new CapturingObserver();
        rr.addObserver(observer);

        rr.run(
                List.of(new AlwaysCooperate(), new AlwaysDefect(), new TitForTat()),
                new IteratedPrisonersDilemma(3));

        assertThat(observer.tournamentResults).hasSize(1);
    }

    @Test
    void tournamentResultPassedToObserverIsComplete() {
        RoundRobin rr = new RoundRobin();
        CapturingObserver observer = new CapturingObserver();
        rr.addObserver(observer);

        rr.run(
                List.of(new AlwaysCooperate(), new AlwaysDefect(), new TitForTat()),
                new IteratedPrisonersDilemma(3));

        // 3 players → 3 matches
        assertThat(observer.tournamentResults.get(0).getResults()).hasSize(3);
    }

    @Test
    void removedObserverDoesNotReceiveTournamentOver() {
        RoundRobin rr = new RoundRobin();
        CapturingObserver observer = new CapturingObserver();
        rr.addObserver(observer);
        rr.removeObserver(observer);

        rr.run(
                List.of(new AlwaysCooperate(), new AlwaysDefect()),
                new IteratedPrisonersDilemma(3));

        assertThat(observer.tournamentResults).isEmpty();
    }

    @Test
    void multipleObserversAllReceiveTournamentOver() {
        RoundRobin rr = new RoundRobin();
        CapturingObserver obs1 = new CapturingObserver();
        CapturingObserver obs2 = new CapturingObserver();
        rr.addObserver(obs1);
        rr.addObserver(obs2);

        rr.run(
                List.of(new AlwaysCooperate(), new AlwaysDefect()),
                new IteratedPrisonersDilemma(3));

        assertThat(obs1.tournamentResults).hasSize(1);
        assertThat(obs2.tournamentResults).hasSize(1);
    }
}
