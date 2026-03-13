package sits.client;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import sits.core.Action;
import sits.core.GameHistory;
import sits.core.Participant;
import sits.networking.dto.GameHistoryDTO;

@RestController
public class ParticipantController {

    private final Participant participant;

    public ParticipantController(Participant participant) {
        this.participant = participant;
    }

    @GetMapping("/name")
    public String getName() {
        return participant.getName();
    }

    @PostMapping("/action")
    public String getAction(@RequestBody GameHistoryDTO dto) {
        GameHistory history = dto.toGameHistory();
        Action action = participant.chooseAction(history);
        return action.getLabel();
    }

    @PostMapping("/reset")
    public void reset() {
        participant.reset();
    }
}
