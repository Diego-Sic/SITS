package sits.replay;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ReplayPlayer {

    private final List<ReplayCommand> commands;
    private final ReplayState state;
    private int nextIndex;
    private volatile boolean playing;

    public ReplayPlayer(List<ReplayCommand> commands) {
        this.commands = List.copyOf(commands);
        this.state = new ReplayState();
        this.nextIndex = 0;
        this.playing = false;
    }

    public void stepForward() {
        if (nextIndex >= commands.size()) return;
        commands.get(nextIndex).apply(state);
        nextIndex++;
    }

    public void stepBack() {
        if (nextIndex <= 0) return;
        nextIndex--;
        commands.get(nextIndex).undo(state);
    }

    public void seekTo(int targetIndex) {
        while (nextIndex < targetIndex) stepForward();
        while (nextIndex > targetIndex) stepBack();
    }

    public CompletableFuture<Void> playAsync(long perStepDelayMs, Runnable onEachStep) {
        playing = true;
        return CompletableFuture.runAsync(() -> {
            while (playing && nextIndex < commands.size()) {
                stepForward();
                onEachStep.run();
                if (perStepDelayMs > 0) {
                    try {
                        Thread.sleep(perStepDelayMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            playing = false;
        });
    }

    public void pause() {
        playing = false;
    }

    public ReplayState getState()    { return state; }
    public int getNextIndex()        { return nextIndex; }
    public int getCommandCount()     { return commands.size(); }
    public boolean isPlaying()       { return playing; }
}
