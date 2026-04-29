package sits.server;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import sits.replay.GameSummary;
import sits.replay.ReplayFile;
import sits.replay.ReplayStore;
import sits.replay.TournamentSummary;
import sits.replay.commands.GameEndCommand;
import sits.replay.commands.MoveCommand;
import sits.replay.commands.TournamentEndCommand;

@WebMvcTest(ReplayController.class)
class ReplayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReplayStore store;
    @MockBean
    private TournamentRegistry registry;
    @MockBean
    private ExecutorService executor;

    private static final ReplayFile.Meta META_1 = new ReplayFile.Meta("t-001", "2026-01-01T00:00:00Z", 3);
    private static final ReplayFile.Meta META_2 = new ReplayFile.Meta("t-002", "2026-01-02T00:00:00Z", 1);

    private static final ReplayFile FULL_FILE = new ReplayFile(META_1, List.of(
            new MoveCommand(1, "Alice", "Bob", "COOPERATE", "DEFECT", 0, 5),
            new GameEndCommand(new GameSummary("Bob", "Alice", "Bob", 0, 5)),
            new TournamentEndCommand(new TournamentSummary(Map.of("Alice", 0, "Bob", 5)))));

    // GET /replays

    @Test
    void listReplays_returnsOk() throws Exception {
        when(store.list()).thenReturn(List.of(META_1, META_2));
        mockMvc.perform(get("/replays")).andExpect(status().isOk());
    }

    @Test
    void listReplays_returnsTournamentIds() throws Exception {
        when(store.list()).thenReturn(List.of(META_1, META_2));
        mockMvc.perform(get("/replays"))
                .andExpect(jsonPath("$[0].tournamentId").value("t-001"))
                .andExpect(jsonPath("$[1].tournamentId").value("t-002"));
    }

    @Test
    void listReplays_returnsCommandCounts() throws Exception {
        when(store.list()).thenReturn(List.of(META_1));
        mockMvc.perform(get("/replays"))
                .andExpect(jsonPath("$[0].commandCount").value(3));
    }

    @Test
    void listReplays_returnsEmptyArrayWhenNoReplays() throws Exception {
        when(store.list()).thenReturn(List.of());
        mockMvc.perform(get("/replays"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // GET /replays/{id}

    @Test
    void getReplay_returnsOkWhenFound() throws Exception {
        when(store.loadById("t-001")).thenReturn(Optional.of(FULL_FILE));
        mockMvc.perform(get("/replays/t-001")).andExpect(status().isOk());
    }

    @Test
    void getReplay_returnsMetaFields() throws Exception {
        when(store.loadById("t-001")).thenReturn(Optional.of(FULL_FILE));
        mockMvc.perform(get("/replays/t-001"))
                .andExpect(jsonPath("$.meta.tournamentId").value("t-001"))
                .andExpect(jsonPath("$.meta.commandCount").value(3));
    }

    @Test
    void getReplay_returnsCommandsWithTypeField() throws Exception {
        when(store.loadById("t-001")).thenReturn(Optional.of(FULL_FILE));
        mockMvc.perform(get("/replays/t-001"))
                .andExpect(jsonPath("$.commands[0].type").value("MOVE"))
                .andExpect(jsonPath("$.commands[1].type").value("GAME_END"))
                .andExpect(jsonPath("$.commands[2].type").value("TOURNAMENT_END"));
    }

    @Test
    void getReplay_returns404WhenNotFound() throws Exception {
        when(store.loadById("missing")).thenReturn(Optional.empty());
        mockMvc.perform(get("/replays/missing")).andExpect(status().isNotFound());
    }
}
