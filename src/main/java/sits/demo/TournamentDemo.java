package sits.demo;

import sits.games.ipd.AlwaysCooperate;
import sits.games.ipd.AlwaysDefect;
import sits.games.ipd.IteratedPrisonersDilemma;
import sits.games.ipd.TitForTat;
import sits.logging.MoveLogger;
import sits.logging.ScoreLogger;
import sits.tournament.RoundRobin;

import java.util.List;

/**
 * Population is intentionally skewed toward TitForTat (5 instances)
 * to replicate Axelrod's result: in a realistic mixed population,
 * cooperative strategies outperform pure defection over the long run. or at least that's what Youtube Said
 */
public class TournamentDemo {

    private static final String DIVIDER = "=".repeat(60);

    public static void main(String[] args) {
        int rounds = 200;

        // 9 players → n*(n-1)/2 = 36 unique matches → 7200 total rounds.
        // Multiple instances of the same strategy are allowed; their scores
        // are aggregated by name in TournamentResult.getSummary().
        var participants = List.of(
                new TitForTat(),
                new TitForTat(),
                new TitForTat(),
                new TitForTat(),
                new TitForTat(),
                new AlwaysCooperate(),
                new AlwaysCooperate(),
                new AlwaysDefect(),
                new AlwaysDefect()
        );

        System.out.println(DIVIDER);
        System.out.println("  SITS — Iterated Prisoner's Dilemma Tournament");
        System.out.printf("  %d rounds per match | Round Robin format%n", rounds);
        System.out.printf("  %d players | %d matches | %d total rounds%n",
                participants.size(),
                participants.size() * (participants.size() - 1) / 2,
                participants.size() * (participants.size() - 1) / 2 * rounds);
        System.out.println(DIVIDER);
        System.out.println();

        System.out.println("  Population:");
        System.out.println("    5 x TitForTat      — mirrors opponent's last move, cooperates first");
        System.out.println("    2 x AlwaysCooperate — never defects");
        System.out.println("    2 x AlwaysDefect    — never cooperates");
        System.out.println();

        // A single Game instance is reused across all matches.
        // Observers registered here receive events for every match in the tournament.
        IteratedPrisonersDilemma game = new IteratedPrisonersDilemma(rounds);

        ConsoleObserver console = new ConsoleObserver();
        game.addObserver(console);                          // live round-by-round output
        game.addObserver(new MoveLogger("moves.log"));      // persists every move to file
        game.addObserver(new ScoreLogger("scores.log"));    // persists final scores to file

        // RoundRobin has its own observer list for the tournament-level event.
        // The same ConsoleObserver instance handles both game and tournament events.
        RoundRobin roundRobin = new RoundRobin();
        roundRobin.addObserver(console);

        roundRobin.run(participants, game);

        System.out.println();
        System.out.println("  Log files written: moves.log, scores.log");
        System.out.println("  (scores aggregated by strategy across all instances)");
    }
}
