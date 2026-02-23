package sits.core;

import java.util.ArrayList;
import java.util.List;

public class GameHistory {

    private final String nameP1;
    private final String nameP2;
    private final List<RoundResult> rounds;

    public GameHistory(String nameP1, String nameP2) {
        this.nameP1 = nameP1;
        this.nameP2 = nameP2;
        this.rounds = new ArrayList<>();
    }

    public String getNameP1() { return nameP1; }
    public String getNameP2() { return nameP2; }

    /**
     * Returns the mutable list of rounds. Callers add super important to remember this @Robert
     * RoundResult objects directly to this list during the game loop.
     */
    public List<RoundResult> getRounds() { return rounds; }

    /**
     * Returns the last RoundResult, or null if no rounds have been played yet.
     */
    public RoundResult getLastRound() {
        if (rounds.isEmpty()) return null;
        return rounds.get(rounds.size() - 1);
    }
}
