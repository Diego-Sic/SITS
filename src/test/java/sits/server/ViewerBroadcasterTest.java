package sits.server;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import sits.core.GameHistory;
import sits.core.GameResult;
import sits.core.MoveEvent;
import sits.core.RoundResult;
import sits.core.TournamentResult;
import sits.networking.StringAction;

@ExtendWith(MockitoExtension.class)
class ViewerBroadcasterTest {

    @Mock
    private SseEmitter emitter;

    private ViewerBroadcaster broadcaster;

    @BeforeEach
    void setUp() {
        broadcaster = new ViewerBroadcaster(0L);
    }

    // ── addEmitter ────────────────────────────────────────────────────

    @Test
    void addEmitter_registersOnCompletionCallback() {
        broadcaster.addEmitter(emitter);
        verify(emitter).onCompletion(any());
    }

    @Test
    void addEmitter_registersOnTimeoutCallback() {
        broadcaster.addEmitter(emitter);
        verify(emitter).onTimeout(any());
    }

    @Test
    void onCompletion_removesEmitterFromList() throws IOException {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        broadcaster.addEmitter(emitter);
        verify(emitter).onCompletion(captor.capture());

        captor.getValue().run(); // simulate disconnect

        broadcaster.onMoveMade(buildMoveEvent());
        verify(emitter, never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void onTimeout_removesEmitterFromList() throws IOException {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        broadcaster.addEmitter(emitter);
        verify(emitter).onTimeout(captor.capture());

        captor.getValue().run(); // simulate timeout

        broadcaster.onMoveMade(buildMoveEvent());
        verify(emitter, never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    // ── onMoveMade ────────────────────────────────────────────────────

    @Test
    void onMoveMade_sendsToRegisteredEmitter() throws IOException {
        broadcaster.addEmitter(emitter);
        broadcaster.onMoveMade(buildMoveEvent());
        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void onMoveMade_sendsToMultipleEmitters() throws IOException {
        SseEmitter second = mock(SseEmitter.class);
        broadcaster.addEmitter(emitter);
        broadcaster.addEmitter(second);
        broadcaster.onMoveMade(buildMoveEvent());
        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(second).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void onMoveMade_removesDeadEmitter() throws IOException {
        SseEmitter alive = mock(SseEmitter.class);
        doThrow(new IOException("closed")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));
        broadcaster.addEmitter(emitter);
        broadcaster.addEmitter(alive);

        broadcaster.onMoveMade(buildMoveEvent()); // emitter dies here
        broadcaster.onMoveMade(buildMoveEvent()); // second move

        verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
        verify(alive, times(2)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void onMoveMade_noEmitters_doesNotThrow() {
        broadcaster.onMoveMade(buildMoveEvent()); // should not throw
    }

    // ── onGameOver ────────────────────────────────────────────────────

    @Test
    void onGameOver_sendsToRegisteredEmitter() throws IOException {
        broadcaster.addEmitter(emitter);
        broadcaster.onGameOver(buildGameResult());
        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void onGameOver_removesDeadEmitter() throws IOException {
        doThrow(new IOException("closed")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));
        broadcaster.addEmitter(emitter);

        broadcaster.onGameOver(buildGameResult()); // emitter dies here
        broadcaster.onGameOver(buildGameResult()); // second call

        verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    // ── onTournamentOver ──────────────────────────────────────────────

    @Test
    void onTournamentOver_sendsToRegisteredEmitter() throws IOException {
        broadcaster.addEmitter(emitter);
        broadcaster.onTournamentOver(buildTournamentResult());
        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void onTournamentOver_completesAllEmitters() {
        broadcaster.addEmitter(emitter);
        broadcaster.onTournamentOver(buildTournamentResult());
        verify(emitter).complete();
    }

    @Test
    void onTournamentOver_clearsListSoNoFurtherSendsOccur() throws IOException {
        broadcaster.addEmitter(emitter);
        broadcaster.onTournamentOver(buildTournamentResult());
        broadcaster.onMoveMade(buildMoveEvent()); // should reach nobody

        verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    // ── helpers ───────────────────────────────────────────────────────

    private MoveEvent buildMoveEvent() {
        GameHistory history = new GameHistory("Alice", "Bob");
        RoundResult round = new RoundResult(new StringAction("COOPERATE"), new StringAction("DEFECT"), 3, 5);
        history.getRounds().add(round);
        return new MoveEvent(round, history);
    }

    private GameResult buildGameResult() {
        GameHistory history = new GameHistory("Alice", "Bob");
        history.getRounds().add(new RoundResult(new StringAction("C"), new StringAction("C"), 3, 3));
        return new GameResult(history);
    }

    private TournamentResult buildTournamentResult() {
        return new TournamentResult(List.of(buildGameResult()));
    }
}
