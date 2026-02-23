package sits.tournament;

import sits.core.Game;
import sits.core.GameObserver;
import sits.core.GameResult;
import sits.core.Participant;
import sits.core.TournamentFormat;
import sits.core.TournamentResult;

import java.util.ArrayList;
import java.util.List;

public class RoundRobin implements TournamentFormat {

    private final List<GameObserver> observers = new ArrayList<>();

    public void addObserver(GameObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(GameObserver observer) {
        observers.remove(observer);
    }

    @Override
    public TournamentResult run(List<Participant> participants, Game game) {
        List<GameResult> results = new ArrayList<>();

        for (int i = 0; i < participants.size(); i++) {
            for (int j = i + 1; j < participants.size(); j++) {
                Participant p1 = participants.get(i);
                Participant p2 = participants.get(j);
                p1.reset();
                p2.reset();
                results.add(game.play(p1, p2));
            }
        }

        TournamentResult tournamentResult = new TournamentResult(results);
        for (GameObserver observer : observers) {
            observer.onTournamentOver(tournamentResult);
        }
        return tournamentResult;
    }
}
