package sits.server;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sits.core.TournamentResult;
import sits.networking.dto.RegistrationRequest;

@RestController
@RequestMapping("/tournaments")
public class TournamentServerController {

    private final TournamentRegistry registry;

    public TournamentServerController(TournamentRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public List<TournamentSummary> getTournaments() {
        return registry.listRegistering().stream()
                .map(t -> new TournamentSummary(t.getId(), t.getName()))
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

    @PostMapping("/{id}/start")
    public ResponseEntity<TournamentResult> start(@PathVariable String id) {
        NetworkedTournament tournament = registry.get(id);
        if (tournament == null) {
            return ResponseEntity.notFound().build();
        }
        TournamentResult result = tournament.start();
        return ResponseEntity.ok(result);
    }

    record TournamentSummary(String id, String name) {}
}
