package sits.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TournamentResult {

    private final List<GameResult> results;

    public TournamentResult(List<GameResult> results) {
        this.results = new ArrayList<>(results);
    }

    public List<GameResult> getResults() {
        return Collections.unmodifiableList(results);
    }

    public Map<String, Integer> getSummary() {
        Map<String, Integer> scores = new LinkedHashMap<>();
        for (GameResult result : results) {
            String p1 = result.getHistory().getNameP1();
            String p2 = result.getHistory().getNameP2();
            scores.merge(p1, result.getTotalScoreP1(), Integer::sum);
            scores.merge(p2, result.getTotalScoreP2(), Integer::sum);
        }
        return scores;
    }
}
