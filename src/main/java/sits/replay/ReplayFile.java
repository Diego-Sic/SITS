package sits.replay;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ReplayFile {

    public record Meta(String tournamentId, String savedAt, int commandCount) {
    }

    private final Meta meta;
    private final List<ReplayCommand> commands;

    @JsonCreator
    public ReplayFile(
            @JsonProperty("meta") Meta meta,
            @JsonProperty("commands") List<ReplayCommand> commands) {
        this.meta = meta;
        this.commands = commands;
    }

    public Meta getMeta() {
        return meta;
    }

    public List<ReplayCommand> getCommands() {
        return commands;
    }
}
