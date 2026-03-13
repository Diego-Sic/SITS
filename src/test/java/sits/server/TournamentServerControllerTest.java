package sits.server;

import java.util.List;

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

import sits.core.TournamentResult;
import sits.networking.dto.RegistrationRequest;

@WebMvcTest(TournamentServerController.class)
class TournamentServerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TournamentRegistry registry;

    @Test
    void getTournaments_returnsRegisteringList() throws Exception {
        NetworkedTournament t = mock(NetworkedTournament.class);
        when(t.getId()).thenReturn("t1");
        when(t.getName()).thenReturn("Test Tournament");
        when(t.getStatus()).thenReturn(TournamentStatus.REGISTERING);
        when(registry.listRegistering()).thenReturn(List.of(t));

        mockMvc.perform(get("/tournaments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("t1"))
                .andExpect(jsonPath("$[0].name").value("Test Tournament"));
    }

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

    @Test
    void start_unknownId_returns404() throws Exception {
        when(registry.get("bad")).thenReturn(null);

        mockMvc.perform(post("/tournaments/bad/start"))
                .andExpect(status().isNotFound());
    }

    @Test
    void start_validId_returnsTournamentResult() throws Exception {
        NetworkedTournament t = mock(NetworkedTournament.class);
        when(registry.get("t1")).thenReturn(t);
        when(t.start()).thenReturn(new TournamentResult(List.of()));

        mockMvc.perform(post("/tournaments/t1/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray());
    }
}