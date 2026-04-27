package sits.replay.commands;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import sits.replay.MoveEntry;
import sits.replay.ReplayCommand;
import sits.replay.ReplayState;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MoveCommand(
        int roundNumber,
        String nameP1,
        String nameP2,
        String actionP1,
        String actionP2,
        int payoffP1,
        int payoffP2) implements ReplayCommand {

    @JsonProperty("type")
    public String type() {
        return "MOVE";
    }

    @Override
    public void apply(ReplayState state) {
        state.getMoveLog().add(new MoveEntry(roundNumber, nameP1, nameP2, actionP1, actionP2, payoffP1, payoffP2));
        addScore(state, nameP1, payoffP1);
        addScore(state, nameP2, payoffP2);
        state.setCurrentRound(roundNumber);
    }

    @Override
    public void undo(ReplayState state) {
        List<MoveEntry> log = state.getMoveLog();
        log.remove(log.size() - 1);
        subtractScore(state, nameP1, payoffP1);
        subtractScore(state, nameP2, payoffP2);
        state.setCurrentRound(log.isEmpty() ? 0 : log.get(log.size() - 1).roundNumber());
    }

    private static void addScore(ReplayState state, String name, int payoff) {
        // if-else avoids ternary null+int unboxing trap; compute(null) keeps key absent
        state.getScores().compute(name, (k, v) -> {
            if (v == null) {
                if (payoff == 0) return null; // stay absent — no spurious zero entry
                return payoff;
            }
            return v + payoff;
        });
    }

    private static void subtractScore(ReplayState state, String name, int payoff) {
        // when key is absent (was never added by apply), stays absent
        state.getScores().compute(name, (k, v) -> {
            if (v == null) return null;
            int result = v - payoff;
            if (result == 0) return null; // remove key — restores pre-apply state
            return result;
        });
    }
}
