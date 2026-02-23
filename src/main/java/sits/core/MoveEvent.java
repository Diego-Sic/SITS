package sits.core;

public class MoveEvent {

    private final RoundResult round;
    private final GameHistory history;

    public MoveEvent(RoundResult round, GameHistory history) {
        this.round = round;
        this.history = history;
    }

    public RoundResult getRound()     { return round; }
    public GameHistory getHistory()   { return history; }
}
