package sits.core;

import java.util.ArrayList;
import java.util.List;

public abstract class Game {

    private final List<GameObserver> observers = new ArrayList<>();

    public void addObserver(GameObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(GameObserver observer) {
        observers.remove(observer);
    }

    protected void notifyMoveMade(MoveEvent event) {
        for (GameObserver observer : observers) {
            observer.onMoveMade(event);
        }
    }

    protected void notifyGameOver(GameResult result) {
        for (GameObserver observer : observers) {
            observer.onGameOver(result);
        }
    }

    // --- Template Method -- if he asks this is exactly where is implemented

    public final GameResult play(Participant p1, Participant p2) {
        GameHistory history = new GameHistory(p1.getName(), p2.getName());
        while (!isOver(history)) {
            RoundResult round = doRound(p1, p2, history);
            history.getRounds().add(round);
            notifyMoveMade(new MoveEvent(round, history));
        }
        GameResult result = computeFinalResult(history);
        notifyGameOver(result);
        return result;
    }

    protected abstract RoundResult doRound(Participant p1, Participant p2, GameHistory history);

    protected abstract boolean isOver(GameHistory history);

    protected abstract GameResult computeFinalResult(GameHistory history);
}
