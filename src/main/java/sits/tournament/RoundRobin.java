package sits.tournament;

import sits.core.Game;
import sits.core.GameResult;
import sits.core.Participant;
import sits.core.TournamentFormat;
import sits.core.TournamentResult;

import java.util.ArrayList;
import java.util.List;

public class RoundRobin implements TournamentFormat {

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

        return new TournamentResult(results);
    }
}
