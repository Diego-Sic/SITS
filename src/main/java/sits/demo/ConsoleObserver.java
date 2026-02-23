package sits.demo;

import sits.core.GameObserver;
import sits.core.GameResult;
import sits.core.MoveEvent;
import sits.core.TournamentResult;

import java.util.Map;

/**
 * Concrete observer that prints tournament activity to the console.
 * Implements the Observer patterns
 * No game logic lives here; it only reads from the event objects it receives.
 */
public class ConsoleObserver implements GameObserver {

    private static final String DIVIDER = "=".repeat(60);
    private static final String THIN    = "-".repeat(60);

    // Tracks how many games have started so we can label each match header.
    // State is safe here because this observer is created once per tournament run.
    private int matchCount = 0;

    /**
     * We detect the start of a new game when round == 1
     * and print the match header inline
     */
    @Override
    public void onMoveMade(MoveEvent event) {
        int round = event.getHistory().getRounds().size();
        String p1 = event.getHistory().getNameP1();
        String p2 = event.getHistory().getNameP2();
        String a1 = event.getRound().getActionP1().getLabel();
        String a2 = event.getRound().getActionP2().getLabel();
        int pay1  = event.getRound().getPayoffP1();
        int pay2  = event.getRound().getPayoffP2();

        if (round == 1) {
            matchCount++;
            System.out.printf("  MATCH %d: %s vs %s%n", matchCount, p1, p2);
            System.out.println(DIVIDER);
        }

        // %-20s left-aligns names in a 20-char field so columns line up regardless of name length
        System.out.printf("  Round %3d | %-20s %-10s (+%d) | %-20s %-10s (+%d)%n",
                round, p1, a1, pay1, p2, a2, pay2);
    }

    /**
     * Fired once when a game ends. Scores come pre-computed from GameResult,
     * which summed payoffs across all rounds inside computeFinalResult().
     */
    @Override
    public void onGameOver(GameResult result) {
        String p1     = result.getHistory().getNameP1();
        String p2     = result.getHistory().getNameP2();
        int score1    = result.getTotalScoreP1();
        int score2    = result.getTotalScoreP2();
        String winner = result.getWinner();

        System.out.println(THIN);
        System.out.printf("  RESULT  : %s %d — %d %s%n", p1, score1, score2, p2);
        System.out.printf("  WINNER  : %s%n", winner);
        System.out.println(DIVIDER);
        System.out.println();
    }

    /**
     * Fired once by RoundRobin after all matches are complete.
     * getSummary() merges scores by strategy name across all instances,
     * so multiple TitForTat players appear as one aggregated entry.
     * Entries are sorted descending so the winner appears first.
     */
    @Override
    public void onTournamentOver(TournamentResult result) {
        System.out.println(DIVIDER);
        System.out.println("  TOURNAMENT FINAL STANDINGS");
        System.out.println(THIN);

        Map<String, Integer> summary = result.getSummary();
        summary.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> System.out.printf("  %-25s %d pts%n", e.getKey(), e.getValue()));

        System.out.println(DIVIDER);
    }
}
