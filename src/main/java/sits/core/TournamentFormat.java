package sits.core;

import java.util.List;

public interface TournamentFormat {
    TournamentResult run(List<Participant> participants, Game game);
}
