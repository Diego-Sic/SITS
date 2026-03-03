package sits.demo;

import sits.games.ipd.AlwaysCooperate;
import sits.games.ipd.AlwaysDefect;
import sits.games.ipd.IteratedPrisonersDilemma;
import sits.games.ipd.TitForTat;
import sits.logging.MoveLogger;
import sits.logging.ScoreLogger;
import sits.tournament.RoundRobin;

import java.util.List;

// Replicates Axelrod's tournament: heavier TitForTat population to test cooperative dominance
public class TournamentDemo {

    private static final String DIVIDER = "=".repeat(60);

    public static void main(String[] args) {
        int rounds = 200;

        // Step 1: Build the participant pool
        var participants = List.of(
                new TitForTat(),        // mirrors opponent's last move, cooperates first
                new TitForTat(),
                new TitForTat(),
                new TitForTat(),
                new TitForTat(),
                new AlwaysCooperate(),  // never defects
                new AlwaysCooperate(),
                new AlwaysDefect(),     // never cooperates
                new AlwaysDefect()
        );

        int n       = participants.size();
        int matches = n * (n - 1) / 2;

        System.out.println(DIVIDER);
        System.out.printf("  SITS — Iterated Prisoner's Dilemma Tournament%n");
        System.out.printf("  %d players | %d matches | %d rounds each%n", n, matches, rounds);
        System.out.println(DIVIDER);
        System.out.println();

        // Step 2: Configure the game and wire up observers
        IteratedPrisonersDilemma game = new IteratedPrisonersDilemma(rounds);
        ConsoleObserver console = new ConsoleObserver();
        game.addObserver(console);                           // prints match headers and results
        game.addObserver(new MoveLogger("moves.log"));       // logs every move to file
        game.addObserver(new ScoreLogger("scores.log"));     // logs final scores to file

        // Step 3: Run the round-robin tournament
        RoundRobin roundRobin = new RoundRobin();
        roundRobin.addObserver(console);
        roundRobin.run(participants, game);

        System.out.println("  Log files: moves.log, scores.log");
    }
}
