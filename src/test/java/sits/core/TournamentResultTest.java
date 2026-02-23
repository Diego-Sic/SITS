package sits.core;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class TournamentResultTest {

    private static final Action COOPERATE = () -> "COOPERATE";
    private static final Action DEFECT    = () -> "DEFECT";

    private GameResult gameResult(String p1, String p2, RoundResult... rounds) {
        GameHistory history = new GameHistory(p1, p2);
        for (RoundResult r : rounds) {
            history.getRounds().add(r);
        }
        return new GameResult(history);
    }

    @Test
    void storesAllGameResults() {
        GameResult r1 = gameResult("Alice", "Bob",
                new RoundResult(COOPERATE, COOPERATE, 3, 3));
        GameResult r2 = gameResult("Alice", "Carol",
                new RoundResult(DEFECT, COOPERATE, 5, 0));

        TournamentResult tournament = new TournamentResult(List.of(r1, r2));

        assertThat(tournament.getResults()).hasSize(2);
        assertThat(tournament.getResults()).containsExactly(r1, r2);
    }

    @Test
    void resultsListIsUnmodifiable() {
        GameResult r1 = gameResult("Alice", "Bob",
                new RoundResult(COOPERATE, COOPERATE, 3, 3));

        TournamentResult tournament = new TournamentResult(List.of(r1));

        assertThat(tournament.getResults()).hasSize(1);
        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> tournament.getResults().clear()
        );
    }

    @Test
    void summaryAccumulatesScoresPerPlayer() {
        // Alice vs Bob: Alice 5, Bob 0
        // Alice vs Carol: Alice 3, Carol 3
        GameResult r1 = gameResult("Alice", "Bob",
                new RoundResult(DEFECT, COOPERATE, 5, 0));
        GameResult r2 = gameResult("Alice", "Carol",
                new RoundResult(COOPERATE, COOPERATE, 3, 3));

        TournamentResult tournament = new TournamentResult(List.of(r1, r2));
        Map<String, Integer> summary = tournament.getSummary();

        assertThat(summary.get("Alice")).isEqualTo(8);
        assertThat(summary.get("Bob")).isEqualTo(0);
        assertThat(summary.get("Carol")).isEqualTo(3);
    }

    @Test
    void summaryContainsAllPlayers() {
        GameResult r1 = gameResult("Alice", "Bob",
                new RoundResult(COOPERATE, COOPERATE, 3, 3));
        GameResult r2 = gameResult("Bob", "Carol",
                new RoundResult(COOPERATE, COOPERATE, 3, 3));

        TournamentResult tournament = new TournamentResult(List.of(r1, r2));

        assertThat(tournament.getSummary()).containsKeys("Alice", "Bob", "Carol");
    }

    @Test
    void emptyTournamentReturnsEmptyCollections() {
        TournamentResult tournament = new TournamentResult(List.of());
        assertThat(tournament.getResults()).isEmpty();
        assertThat(tournament.getSummary()).isEmpty();
    }
}
