package sits.server;

import java.util.List;
import java.util.concurrent.ExecutorService;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import sits.networking.dto.RegistrationRequest;

@RestController
@RequestMapping("/tournaments")
public class TournamentServerController {

    private final TournamentRegistry registry;
    private final ExecutorService executor;

    public TournamentServerController(TournamentRegistry registry, ExecutorService executor) {
        this.registry = registry;
        this.executor = executor;
    }

    @GetMapping
    public List<TournamentSummary> getTournaments() {
        return registry.listActive().stream()
                .map(t -> new TournamentSummary(t.getId(), t.getName(), t.getStatus()))
                .toList();
    }

    @PostMapping("/{id}/register")
    public ResponseEntity<Void> register(@PathVariable String id, @RequestBody RegistrationRequest req) {
        NetworkedTournament tournament = registry.get(id);
        if (tournament == null) {
            return ResponseEntity.notFound().build();
        }
        tournament.addRemoteParticipant(req);
        return ResponseEntity.ok().build();
    }

    // Submits the tournament to a background thread and returns 202 immediately.
    // This frees the HTTP thread so the SSE stream endpoint can accept viewer
    // connections
    // while the tournament is still running.
    @PostMapping("/{id}/start")
    public ResponseEntity<Void> start(@PathVariable String id) {
        NetworkedTournament tournament = registry.get(id);
        if (tournament == null) {
            return ResponseEntity.notFound().build();
        }
        executor.submit(tournament::start);
        return ResponseEntity.accepted().build();
    }

    // Opens a persistent SSE connection for a viewer.
    // The emitter is handed to the tournament's ViewerBroadcaster, which pushes
    // move events to it as the game runs on the background thread.
    @GetMapping(value = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMoves(@PathVariable String id) {
        NetworkedTournament tournament = registry.get(id);
        if (tournament == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        SseEmitter emitter = new SseEmitter(0L);
        tournament.getBroadcaster().addEmitter(emitter);
        return emitter;
    }

    record TournamentSummary(String id, String name, TournamentStatus status) {
    }
}
