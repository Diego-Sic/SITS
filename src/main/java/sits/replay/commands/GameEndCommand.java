package sits.replay.commands;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import sits.replay.GameSummary;
import sits.replay.ReplayCommand;
import sits.replay.ReplayState;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GameEndCommand(GameSummary summary) implements ReplayCommand {

    @JsonProperty("type")
    public String type() {
        return "GAME_END";
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
