package sits.replay.commands;

import sits.replay.GameSummary;
import sits.replay.ReplayCommand;
import sits.replay.ReplayState;

import java.util.List;

public class GameEndCommand implements ReplayCommand {

    private final GameSummary summary;

    public GameEndCommand(GameSummary summary) {
        this.summary = summary;
    }

    @Override
    public void apply(ReplayState state) {
        state.getCompletedGames().add(summary);
    }

    @Override
    public void undo(ReplayState state) {
        List<GameSummary> games = state.getCompletedGames();
        games.remove(games.size() - 1);
    }
}
