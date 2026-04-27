package sits.replay;

import java.util.Map;

import sits.replay.commands.GameEndCommand;
import sits.replay.commands.MoveCommand;
import sits.replay.commands.TournamentEndCommand;

public class CommandTypeRegistry {

    // If you ever create a new command, please add it to this map and add a test in
    // CommandTypeRegistryTest to verify it returns the correct class.
    private static final Map<String, Class<? extends ReplayCommand>> REGISTRY = Map.of(
            "MOVE", MoveCommand.class,
            "GAME_END", GameEndCommand.class,
            "TOURNAMENT_END", TournamentEndCommand.class);

    private CommandTypeRegistry() {
    }

    public static Class<? extends ReplayCommand> get(String type) {
        if (type == null)
            throw new IllegalArgumentException("Unknown command type: null");
        Class<? extends ReplayCommand> cls = REGISTRY.get(type);
        if (cls == null)
            throw new IllegalArgumentException("Unknown command type: " + type);
        return cls;
    }
}
