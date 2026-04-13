package sits.server;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class TournamentRegistry {

    private final Map<String, NetworkedTournament> tournaments = new LinkedHashMap<>();

    public void add(NetworkedTournament t) {
        tournaments.put(t.getId(), t);
    }

    public NetworkedTournament get(String id) {
        return tournaments.get(id);
    }

    public List<NetworkedTournament> listRegistering() {
        return tournaments.values().stream()
                .filter(t -> t.getStatus() == TournamentStatus.REGISTERING)
                .collect(Collectors.toList());
    }

    public List<NetworkedTournament> listActive() {
        return tournaments.values().stream()
                .filter(t -> t.getStatus() == TournamentStatus.REGISTERING
                        || t.getStatus() == TournamentStatus.RUNNING)
                .collect(Collectors.toList());
    }
}
