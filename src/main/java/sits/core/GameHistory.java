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

    // careful, this list is mutable! don't forget @Robert :0
    public List<RoundResult> getRounds() { return rounds; }

    public RoundResult getLastRound() {
        if (rounds.isEmpty()) return null;
        return rounds.get(rounds.size() - 1);
    }
}
