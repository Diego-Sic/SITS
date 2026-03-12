package sits.networking;

import java.util.function.Function;

import org.springframework.web.client.RestTemplate;

import sits.core.Action;
import sits.core.GameHistory;
import sits.core.Participant;
import sits.networking.dto.GameHistoryDTO;

public class RemoteParticipant implements Participant {

    private final String name;
    private final String clientUrl;
    private final Function<String, Action> actionFactory;
    private final RestTemplate restTemplate;

    // Production constructor. RestTemplate is created internally so callers do not need to know about it.
    public RemoteParticipant(String name, String clientUrl, Function<String, Action> actionFactory) {
        this.name = name;
        this.clientUrl = clientUrl;
        this.actionFactory = actionFactory;
        this.restTemplate = new RestTemplate();
    }

    // Test constructor. Allows injecting a RestTemplate backed by MockRestServiceServer to avoid real HTTP calls. This is very cool to
    // test with easy :)
    RemoteParticipant(String name, String clientUrl, Function<String, Action> actionFactory, RestTemplate restTemplate) {
        this.name = name;
        this.clientUrl = clientUrl;
        this.actionFactory = actionFactory;
        this.restTemplate = restTemplate;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Action chooseAction(GameHistory history) {
        GameHistoryDTO dto = GameHistoryDTO.fromGameHistory(history);
        String label = restTemplate.postForObject(clientUrl + "/action", dto, String.class);
        return actionFactory.apply(label);
    }

    @Override
    public void reset() {
        restTemplate.postForObject(clientUrl + "/reset", null, Void.class);
    }
}
