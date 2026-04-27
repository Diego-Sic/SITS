package sits.replay;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import sits.replay.commands.GameEndCommand;
import sits.replay.commands.MoveCommand;
import sits.replay.commands.TournamentEndCommand;

class ReplayPlayerTest {

    private static final MoveCommand MOVE_1 = new MoveCommand(1, "Alice", "Bob", "COOPERATE", "DEFECT", 0, 5);
    private static final MoveCommand MOVE_2 = new MoveCommand(2, "Alice", "Bob", "COOPERATE", "COOPERATE", 3, 3);
    private static final GameEndCommand GAME_END = new GameEndCommand(new GameSummary("Bob", "Alice", "Bob", 3, 8));
    private static final TournamentEndCommand TOURNAMENT_END = new TournamentEndCommand(
            new TournamentSummary(Map.of("Alice", 3, "Bob", 8)));

    private ReplayPlayer player;

    @BeforeEach
    void setUp() {
        player = new ReplayPlayer(List.of(MOVE_1, MOVE_2, GAME_END, TOURNAMENT_END));
    }

    // stepForward

    @Test
    void startsAtIndexZero() {
        assertThat(player.getNextIndex()).isZero();
    }

    @Test
    void stepForwardAppliesCommandAndAdvancesCursor() {
        player.stepForward();
        assertThat(player.getNextIndex()).isEqualTo(1);
        assertThat(player.getState().getCurrentRound()).isEqualTo(1);
    }

    @Test
    void stepForwardTwiceAppliesBothCommands() {
        player.stepForward();
        player.stepForward();
        assertThat(player.getNextIndex()).isEqualTo(2);
        assertThat(player.getState().getCurrentRound()).isEqualTo(2);
        assertThat(player.getState().getScores()).containsEntry("Alice", 3);
        assertThat(player.getState().getScores()).containsEntry("Bob", 8);
    }

    @Test
    void stepForwardAtEndIsNoOp() {
        for (int i = 0; i < 4; i++)
            player.stepForward();
        player.stepForward(); // at end — should not throw or advance
        assertThat(player.getNextIndex()).isEqualTo(4);
    }

    // stepBack

    @Test
    void stepBackUndoesLastCommand() {
        player.stepForward();
        player.stepForward();
        player.stepBack();
        assertThat(player.getNextIndex()).isEqualTo(1);
        assertThat(player.getState().getCurrentRound()).isEqualTo(1);
        assertThat(player.getState().getScores()).doesNotContainKey("Alice");
    }

    @Test
    void stepBackAtStartIsNoOp() {
        player.stepBack(); // cursor at 0 — should not throw or go negative
        assertThat(player.getNextIndex()).isZero();
    }

    @Test
    void stepForwardThenBackRestoresEmptyState() {
        player.stepForward();
        player.stepBack();
        assertThat(player.getNextIndex()).isZero();
        assertThat(player.getState().getCurrentRound()).isZero();
        assertThat(player.getState().getScores()).isEmpty();
        assertThat(player.getState().getMoveLog()).isEmpty();
    }

    // seekTo

    @Test
    void seekToForwardReachesTargetIndex() {
        player.seekTo(3);
        assertThat(player.getNextIndex()).isEqualTo(3);
    }

    @Test
    void seekToForwardAppliesAllIntermediateCommands() {
        player.seekTo(2);
        assertThat(player.getState().getCurrentRound()).isEqualTo(2);
        assertThat(player.getState().getScores()).containsEntry("Bob", 8);
    }

    @Test
    void seekToBackwardUndoesToTarget() {
        player.seekTo(4);
        player.seekTo(1);
        assertThat(player.getNextIndex()).isEqualTo(1);
        assertThat(player.getState().getCurrentRound()).isEqualTo(1);
        assertThat(player.getState().getCompletedGames()).isEmpty();
    }

    @Test
    void seekToCurrentIndexIsNoOp() {
        player.stepForward();
        player.stepForward();
        ReplayState stateBefore = player.getState();
        int scoreBob = stateBefore.getScores().get("Bob");

        player.seekTo(2);

        assertThat(player.getNextIndex()).isEqualTo(2);
        assertThat(player.getState().getScores().get("Bob")).isEqualTo(scoreBob);
    }

    @Test
    void seekToZeroResetsToEmptyState() {
        player.seekTo(4);
        player.seekTo(0);
        assertThat(player.getNextIndex()).isZero();
        assertThat(player.getState().getCurrentRound()).isZero();
        assertThat(player.getState().getScores()).isEmpty();
    }

    // playAsync

    @Test
    void playAsyncRunsAllCommandsAndCompletes() throws Exception {
        AtomicInteger steps = new AtomicInteger(0);
        player.playAsync(0, steps::incrementAndGet).get(2, TimeUnit.SECONDS);
        assertThat(steps.get()).isEqualTo(4);
        assertThat(player.getNextIndex()).isEqualTo(4);
    }

    @Test
    void playAsyncCallsOnEachStepAfterEveryCommand() throws Exception {
        AtomicInteger steps = new AtomicInteger(0);
        ReplayPlayer small = new ReplayPlayer(List.of(MOVE_1, MOVE_2));
        small.playAsync(0, steps::incrementAndGet).get(2, TimeUnit.SECONDS);
        assertThat(steps.get()).isEqualTo(2);
    }

    @Test
    void pauseStopsPlayback() throws Exception {
        ReplayPlayer big = new ReplayPlayer(List.of(MOVE_1, MOVE_2, GAME_END, TOURNAMENT_END));
        big.playAsync(50, () -> {
        });
        big.pause();
        Thread.sleep(200);
        assertThat(big.getNextIndex()).isLessThan(4);
    }
}
