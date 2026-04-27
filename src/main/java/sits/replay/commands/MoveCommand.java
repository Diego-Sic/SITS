package sits.replay.commands;

import sits.replay.MoveEntry;
import sits.replay.ReplayCommand;
import sits.replay.ReplayState;

import java.util.List;

public class MoveCommand implements ReplayCommand {

    private final int roundNumber;
    private final String nameP1;
    private final String nameP2;
    private final String actionP1;
    private final String actionP2;
    private final int payoffP1;
    private final int payoffP2;

    public MoveCommand(int roundNumber, String nameP1, String nameP2,
                       String actionP1, String actionP2, int payoffP1, int payoffP2) {
        this.roundNumber = roundNumber;
        this.nameP1 = nameP1;
        this.nameP2 = nameP2;
        this.actionP1 = actionP1;
        this.actionP2 = actionP2;
        this.payoffP1 = payoffP1;
        this.payoffP2 = payoffP2;
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
        // returning null from merge removes the key — restores pre-apply state when score reaches 0
        state.getScores().merge(name, payoff, (current, toSubtract) -> {
            int result = current - toSubtract;
            return result == 0 ? null : result;
        });
    }
}
