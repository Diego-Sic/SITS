package sits.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import sits.core.GameObserver;
import sits.core.GameResult;
import sits.core.MoveEvent;
import sits.core.TournamentResult;
import sits.networking.dto.MoveEventDTO;

public class ViewerBroadcaster implements GameObserver {

    private final long delayMs;
    private final List<SseEmitter> emitters;
    private final ObjectMapper mapper;

    public ViewerBroadcaster(long delayMs) {
        this.delayMs = delayMs;
        this.emitters = new CopyOnWriteArrayList<>();
        this.mapper = new ObjectMapper();
    }

    // Adds a new SseEmitter to the list of emitters and sets up completion and
    // timeout handlers.
    public void addEmitter(SseEmitter emitter) {
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitters.add(emitter);
    }

    @Override
    public void onMoveMade(MoveEvent event) {
        String json = serialize(MoveEventDTO.fromMoveEvent(event));
        if (json == null)
            return;

        // Send the JSON data to all connected emitters and remove any that have become
        // unresponsive.
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().data(json));
            } catch (IOException e) {
                dead.add(emitter);
            }
        }
        emitters.removeAll(dead);

        // Introduce a delay if specified to control the pace of updates for viewers.
        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void onGameOver(GameResult result) {
        sendDto(MoveEventDTO.gameOver(result));
    }

    @Override
    public void onTournamentOver(TournamentResult result) {
        sendDto(MoveEventDTO.tournamentOver(result));
        for (SseEmitter emitter : emitters) {
            emitter.complete();
        }
        emitters.clear();
    }

    private void sendDto(MoveEventDTO dto) {
        String json = serialize(dto);
        if (json == null)
            return;

        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().data(json));
            } catch (IOException e) {
                dead.add(emitter);
            }
        }
        emitters.removeAll(dead);
    }

    private String serialize(MoveEventDTO dto) {
        try {
            return mapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
