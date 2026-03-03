package sits.demo;

import sits.core.GameObserver;
import sits.core.GameResult;
import sits.core.MoveEvent;
import sits.core.TournamentResult;

import java.util.Map;

public class ConsoleObserver implements GameObserver {

    private static final String DIVIDER = "=".repeat(60);
    private static final String THIN    = "-".repeat(60);

    private int matchCount = 0;

    @Override
    public void onMoveMade(MoveEvent event) {
        // Only print the match header on the first round; skip per-round output
        if (event.getHistory().getRounds().size() != 1) return;

        matchCount++;
        System.out.printf("  MATCH %d: %s vs %s%n",
                matchCount,
                event.getHistory().getNameP1(),
                event.getHistory().getNameP2());
    }

    @Override
    public void onGameOver(GameResult result) {
        System.out.println(THIN);
        System.out.printf("  RESULT  : %s %d — %d %s%n",
                result.getHistory().getNameP1(),
                result.getTotalScoreP1(),
                result.getTotalScoreP2(),
                result.getHistory().getNameP2());
        System.out.printf("  WINNER  : %s%n", result.getWinner());
        System.out.println(DIVIDER);
        System.out.println();
    }

    @Override
    public void onTournamentOver(TournamentResult result) {
        System.out.println(DIVIDER);
        System.out.println("  TOURNAMENT FINAL STANDINGS");
        System.out.println(THIN);

        Map<String, Integer> summary = result.getSummary();

        summary.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> System.out.printf("  %-25s %d pts%n", e.getKey(), e.getValue()));

        System.out.println(THIN);

        // Best strategy: highest cumulative score across all matches
        Map.Entry<String, Integer> best = summary.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow();
        System.out.printf("  Best strategy : %s (%d pts)%n", best.getKey(), best.getValue());
        System.out.println();
        System.out.println("  Goal: MAXIMIZE points (higher = better)");
        System.out.println("  Payoff matrix: C/C → 3|3   C/D → 0|5   D/C → 5|0   D/D → 1|1");

        System.out.println(DIVIDER);
    }
}
