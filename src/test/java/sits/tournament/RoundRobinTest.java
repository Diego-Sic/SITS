package sits.tournament;

import org.junit.jupiter.api.Test;
import sits.core.GameResult;
import sits.core.TournamentResult;
import sits.games.ipd.AlwaysCooperate;
import sits.games.ipd.AlwaysDefect;
import sits.games.ipd.IteratedPrisonersDilemma;
import sits.games.ipd.TitForTat;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoundRobinTest {

    @Test
    void twoPLayersProduceOneMatch() {
        RoundRobin rr = new RoundRobin();
        TournamentResult result = rr.run(
                List.of(new AlwaysCooperate(), new AlwaysDefect()),
                new IteratedPrisonersDilemma(3));

        assertThat(result.getResults()).hasSize(1);
    }

    @Test
    void threePlayersProduceThreeMatches() {
        RoundRobin rr = new RoundRobin();
        TournamentResult result = rr.run(
                List.of(new AlwaysCooperate(), new AlwaysDefect(), new TitForTat()),
                new IteratedPrisonersDilemma(3));

        // 3 players → 3 unique pairs: (0,1), (0,2), (1,2)
        assertThat(result.getResults()).hasSize(3);
    }

    @Test
    void fourPlayersProduceSixMatches() {
        RoundRobin rr = new RoundRobin();
        TournamentResult result = rr.run(
                List.of(new AlwaysCooperate(), new AlwaysDefect(), new TitForTat(), new AlwaysCooperate()),
                new IteratedPrisonersDilemma(3));

        // 4 players → 6 unique pairs: n*(n-1)/2
        assertThat(result.getResults()).hasSize(6);
    }

    @Test
    void noMirrorMatches() {
        RoundRobin rr = new RoundRobin();
        TournamentResult result = rr.run(
                List.of(new AlwaysCooperate(), new AlwaysDefect(), new TitForTat()),
                new IteratedPrisonersDilemma(1));

        // Verify no pair of player names appears twice in swapped order
        List<GameResult> results = result.getResults();
        for (int i = 0; i < results.size(); i++) {
            for (int j = i + 1; j < results.size(); j++) {
                String p1i = results.get(i).getHistory().getNameP1();
                String p2i = results.get(i).getHistory().getNameP2();
                String p1j = results.get(j).getHistory().getNameP1();
                String p2j = results.get(j).getHistory().getNameP2();
                assertThat(p1i.equals(p2j) && p2i.equals(p1j)).isFalse();
            }
        }
    }

    @Test
    void resetIsCalledBeforeEachMatch() {
        class TrackingParticipant extends AlwaysCooperate {
            int resetCount = 0;
            @Override public void reset() { resetCount++; }
        }

        TrackingParticipant p1 = new TrackingParticipant();
        TrackingParticipant p2 = new TrackingParticipant();
        TrackingParticipant p3 = new TrackingParticipant();

        new RoundRobin().run(List.of(p1, p2, p3), new IteratedPrisonersDilemma(1));

        // Each participant plays 2 matches in a 3-player round robin → reset called twice each
        assertThat(p1.resetCount).isEqualTo(2);
        assertThat(p2.resetCount).isEqualTo(2);
        assertThat(p3.resetCount).isEqualTo(2);
    }

    @Test
    void summaryContainsAllPlayers() {
        RoundRobin rr = new RoundRobin();
        TournamentResult result = rr.run(
                List.of(new AlwaysCooperate(), new AlwaysDefect(), new TitForTat()),
                new IteratedPrisonersDilemma(3));

        assertThat(result.getSummary()).containsKeys("AlwaysCooperate", "AlwaysDefect", "TitForTat");
    }
}
