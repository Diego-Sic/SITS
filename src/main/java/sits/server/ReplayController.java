package sits.server;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sits.replay.ReplayFile;
import sits.replay.ReplayStore;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/replays")
public class ReplayController {

    private final ReplayStore store;

    public ReplayController(ReplayStore store) {
        this.store = store;
    }

    @GetMapping
    public List<ReplayFile.Meta> listReplays() throws IOException {
        return store.list();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReplayFile> getReplay(@PathVariable String id) throws IOException {
        Optional<ReplayFile> file = store.loadById(id);
        if (file.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(file.get());
    }
}
