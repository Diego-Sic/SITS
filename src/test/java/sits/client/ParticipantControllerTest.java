package sits.client;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import sits.core.GameHistory;
import sits.core.Participant;
import sits.games.ipd.PrisonerAction;
import sits.networking.dto.GameHistoryDTO;

@WebMvcTest(ParticipantController.class)
class ParticipantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private Participant participant;

    // Required: @WebMvcTest loads ClientApp which @Autowires TournamentServerClient, so Spring needs this mock to build the context.
    @MockBean
    private TournamentServerClient tournamentServerClient;

    @Test
    void getName_returnsParticipantName() throws Exception {
        when(participant.getName()).thenReturn("Alice");

        mockMvc.perform(get("/name"))
                .andExpect(status().isOk())
                .andExpect(content().string("Alice"));
    }

    @Test
    void getAction_callsChooseActionWithHistory() throws Exception {
        when(participant.getName()).thenReturn("Alice");
        when(participant.chooseAction(any(GameHistory.class))).thenReturn(PrisonerAction.COOPERATE);

        GameHistoryDTO dto = new GameHistoryDTO("Alice", "Bob", java.util.List.of());
        mockMvc.perform(post("/action")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(participant).chooseAction(any(GameHistory.class));
    }

    @Test
    void getAction_returnsActionLabel() throws Exception {
        when(participant.chooseAction(any(GameHistory.class))).thenReturn(PrisonerAction.COOPERATE);

        GameHistoryDTO dto = new GameHistoryDTO("Alice", "Bob", java.util.List.of());
        mockMvc.perform(post("/action")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string("COOPERATE"));
    }

    @Test
    void reset_callsParticipantReset() throws Exception {
        mockMvc.perform(post("/reset"))
                .andExpect(status().isOk());

        verify(participant).reset();
    }
}
