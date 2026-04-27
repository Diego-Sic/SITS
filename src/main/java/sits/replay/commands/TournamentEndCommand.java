package sits.replay.commands;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import sits.replay.ReplayCommand;
import sits.replay.ReplayState;
import sits.replay.TournamentSummary;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TournamentEndCommand(TournamentSummary summary) implements ReplayCommand {

    @JsonProperty("type")
    public String type() {
        return "TOURNAMENT_END";
    }

    @Override
    public void apply(ReplayState state) {
        state.setFinalResult(Optional.of(summary));
    }

    @Override
    public void undo(ReplayState state) {
        state.setFinalResult(Optional.empty());
    }
}
