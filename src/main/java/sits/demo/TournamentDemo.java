package sits.demo;

import sits.games.ipd.AlwaysCooperate;
import sits.games.ipd.AlwaysDefect;
import sits.games.ipd.IteratedPrisonersDilemma;
import sits.games.ipd.TitForTat;
import sits.logging.MoveLogger;
import sits.logging.ScoreLogger;
import sits.tournament.RoundRobin;

import java.util.List;

// more TitForTat than anything else to see if Axelrod's thing holds up — at least that's what YouTube said :D
public class TournamentDemo {

    private static final String DIVIDER = "=".repeat(60);

    public static void main(String[] args) {
        int rounds = 200;

        // 9 players = 36 matches = 7200 rounds total :0
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

        IteratedPrisonersDilemma game = new IteratedPrisonersDilemma(rounds);

        ConsoleObserver console = new ConsoleObserver();
        game.addObserver(console);                          // live round-by-round output
        game.addObserver(new MoveLogger("moves.log"));      // persists every move to file
        game.addObserver(new ScoreLogger("scores.log"));    // persists final scores to file

        RoundRobin roundRobin = new RoundRobin();
        roundRobin.addObserver(console);

        roundRobin.run(participants, game);

        System.out.println();
        System.out.println("  Log files written: moves.log, scores.log");
        System.out.println("  (scores aggregated by strategy across all instances)");
    }
}
