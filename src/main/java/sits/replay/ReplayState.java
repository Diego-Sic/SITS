package sits.replay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ReplayState {

    private int currentRound;
    private final Map<String, Integer> scores;
    private final List<MoveEntry> moveLog;
    private final List<GameSummary> completedGames;
    private Optional<TournamentSummary> finalResult;

    public ReplayState() {
        this.currentRound = 0;
        this.scores = new HashMap<>();
        this.moveLog = new ArrayList<>();
        this.completedGames = new ArrayList<>();
        this.finalResult = Optional.empty();
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(int round) {
        this.currentRound = round;
    }

    public Map<String, Integer> getScores() {
        return scores;
    }

    public List<MoveEntry> getMoveLog() {
        return moveLog;
    }

    public List<GameSummary> getCompletedGames() {
        return completedGames;
    }

    public Optional<TournamentSummary> getFinalResult() {
        return finalResult;
    }

    public void setFinalResult(Optional<TournamentSummary> result) {
        this.finalResult = result;
    }
}
