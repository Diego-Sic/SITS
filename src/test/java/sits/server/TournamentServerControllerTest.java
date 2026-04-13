package sits.server;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import sits.networking.dto.RegistrationRequest;

@WebMvcTest(TournamentServerController.class)
class TournamentServerControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private TournamentRegistry registry;
    @MockBean
    private ExecutorService executor;

    // ── GET /tournaments ──────────────────────────────────────────────

    @Test
    void getTournaments_returnsActiveList() throws Exception {
        NetworkedTournament t = mock(NetworkedTournament.class);
        when(t.getId()).thenReturn("t1");
        when(t.getName()).thenReturn("Test Tournament");
        when(t.getStatus()).thenReturn(TournamentStatus.REGISTERING);
        when(registry.listActive()).thenReturn(List.of(t));

        mockMvc.perform(get("/tournaments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("t1"))
                .andExpect(jsonPath("$[0].name").value("Test Tournament"))
                .andExpect(jsonPath("$[0].status").value("REGISTERING"));
    }

    @Test
    void getTournaments_includesStatusField() throws Exception {
        NetworkedTournament t = mock(NetworkedTournament.class);
        when(t.getId()).thenReturn("t2");
        when(t.getName()).thenReturn("Running Tournament");
        when(t.getStatus()).thenReturn(TournamentStatus.RUNNING);
        when(registry.listActive()).thenReturn(List.of(t));

        mockMvc.perform(get("/tournaments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("RUNNING"));
    }

    // ── POST /{id}/register ───────────────────────────────────────────

    @Test
    void register_unknownId_returns404() throws Exception {
        when(registry.get("bad")).thenReturn(null);

        RegistrationRequest req = new RegistrationRequest("Alice", "127.0.0.1", 9000);
        mockMvc.perform(post("/tournaments/bad/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    void register_validId_returns200() throws Exception {
        NetworkedTournament t = mock(NetworkedTournament.class);
        when(registry.get("t1")).thenReturn(t);

        RegistrationRequest req = new RegistrationRequest("Alice", "127.0.0.1", 9000);
        mockMvc.perform(post("/tournaments/t1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(t).addRemoteParticipant(any(RegistrationRequest.class));
    }

    // Post /{id}/start

    @Test
    void start_unknownId_returns404() throws Exception {
        when(registry.get("bad")).thenReturn(null);

        mockMvc.perform(post("/tournaments/bad/start"))
                .andExpect(status().isNotFound());
    }

    @Test
    void start_validId_returns202() throws Exception {
        NetworkedTournament t = mock(NetworkedTournament.class);
        when(registry.get("t1")).thenReturn(t);

        mockMvc.perform(post("/tournaments/t1/start"))
                .andExpect(status().isAccepted());
    }

    @Test
    void start_validId_submitsTournamentToExecutor() throws Exception {
        NetworkedTournament t = mock(NetworkedTournament.class);
        when(registry.get("t1")).thenReturn(t);

        mockMvc.perform(post("/tournaments/t1/start"));

        verify(executor).submit(any(Callable.class));
    }

    // GET /{id}/stream

    @Test
    void stream_unknownId_returns404() throws Exception {
        when(registry.get("bad")).thenReturn(null);

        mockMvc.perform(get("/tournaments/bad/stream"))
                .andExpect(status().isNotFound());
    }

    @Test
    void stream_validId_returnsEventStream() throws Exception {
        NetworkedTournament t = mock(NetworkedTournament.class);
        when(registry.get("t1")).thenReturn(t);
        when(t.getBroadcaster()).thenReturn(new ViewerBroadcaster(0L));

        mockMvc.perform(get("/tournaments/t1/stream"))
                .andExpect(status().isOk());
    }

    @Test
    void stream_validId_registersEmitterWithBroadcaster() throws Exception {
        NetworkedTournament t = mock(NetworkedTournament.class);
        ViewerBroadcaster broadcaster = mock(ViewerBroadcaster.class);
        when(registry.get("t1")).thenReturn(t);
        when(t.getBroadcaster()).thenReturn(broadcaster);

        mockMvc.perform(get("/tournaments/t1/stream"));

        verify(broadcaster).addEmitter(any());
    }
}
