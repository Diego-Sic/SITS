package sits.replay;

public interface ReplayCommand {
    void apply(ReplayState state);

    void undo(ReplayState state);
}
