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
        state.getScores().merge(nameP1, payoffP1, Integer::sum);
        state.getScores().merge(nameP2, payoffP2, Integer::sum);
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

    private void subtractScore(ReplayState state, String name, int payoff) {
        // returning null from merge removes the key — restores pre-apply state when
        // score reaches 0
        state.getScores().merge(name, payoff, (current, toSubtract) -> {
            int result = current - toSubtract;
            return result == 0 ? null : result;
        });
    }
}
