package sits.core;

import org.junit.jupiter.api.Test;
import sits.games.ipd.AlwaysCooperate;
import sits.games.ipd.AlwaysDefect;
import sits.games.ipd.IteratedPrisonersDilemma;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TournamentFormatTest {

    /**
     * Minimal implementation
     * */
    static class SingleMatchFormat implements TournamentFormat {
        @Override
        public TournamentResult run(List<Participant> participants, Game game) {
            Participant p1 = participants.get(0);
            Participant p2 = participants.get(1);
            p1.reset();
            p2.reset();
            GameResult result = game.play(p1, p2);
            return new TournamentResult(List.of(result));
        }
    }

    @Test
    void runReturnsTournamentResult() {
        TournamentFormat format = new SingleMatchFormat();
        Game game = new IteratedPrisonersDilemma(3);
        List<Participant> participants = List.of(new AlwaysCooperate(), new AlwaysDefect());

        TournamentResult result = format.run(participants, game);

        assertThat(result).isNotNull();
        assertThat(result.getResults()).hasSize(1);
    }

    @Test
    void runAcceptsAnyGameInstance() {
        TournamentFormat format = new SingleMatchFormat();
        List<Participant> participants = List.of(new AlwaysCooperate(), new AlwaysDefect());

        // Works with 1-round game
        TournamentResult r1 = format.run(participants, new IteratedPrisonersDilemma(1));
        assertThat(r1.getResults().get(0).getHistory().getRounds()).hasSize(1);

        // Works with 10-round game
        TournamentResult r10 = format.run(participants, new IteratedPrisonersDilemma(10));
        assertThat(r10.getResults().get(0).getHistory().getRounds()).hasSize(10);
    }
}
