package sits.server;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import sits.core.Action;
import sits.core.Game;
import sits.core.Participant;
import sits.core.TournamentFormat;
import sits.core.TournamentResult;
import sits.networking.RemoteParticipant;
import sits.networking.dto.RegistrationRequest;
import sits.replay.ReplayRecorder;
import sits.replay.ReplayStore;

public class NetworkedTournament {

    private static final Path DEFAULT_REPLAY_DIR = Path.of("replays");

    private final String id;
    private final String name;
    private final TournamentFormat format;
    private final Game game;
    private final List<Participant> participants;
    private final Function<String, Action> actionFactory;
    private final ViewerBroadcaster broadcaster;
    private final ReplayStore replayStore;
    private TournamentStatus status;
    private TournamentResult lastResult;

    public NetworkedTournament(String id, String name, TournamentFormat format, Game game,
                               List<Participant> initialParticipants,
                               Function<String, Action> actionFactory,
                               long delayMs, ReplayStore replayStore) {
        this.id            = id;
        this.name          = name;
        this.format        = format;
        this.game          = game;
        this.participants  = new ArrayList<>(initialParticipants);
        this.actionFactory = actionFactory;
        this.broadcaster   = new ViewerBroadcaster(delayMs);
        this.replayStore   = replayStore;
        this.status        = TournamentStatus.REGISTERING;
    }

    // Convenience overload — uses default replay directory.
    public NetworkedTournament(String id, String name, TournamentFormat format, Game game,
                               List<Participant> initialParticipants,
                               Function<String, Action> actionFactory,
                               long delayMs) {
        this(id, name, format, game, initialParticipants, actionFactory, delayMs,
                new ReplayStore(DEFAULT_REPLAY_DIR));
    }

    // Convenience overload — no delay, keeps existing callers and tests unchanged.
    public NetworkedTournament(String id, String name, TournamentFormat format, Game game,
                               List<Participant> initialParticipants,
                               Function<String, Action> actionFactory) {
        this(id, name, format, game, initialParticipants, actionFactory, 0L);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public TournamentStatus getStatus() {
        return status;
    }

    // Package-private — for testing only. No cleaner way to assert on RUNNING state
    void setStatus(TournamentStatus status) {
        this.status = status;
    }

    public void addRemoteParticipant(RegistrationRequest req) {
        if (status != TournamentStatus.REGISTERING) {
            // Come on bro, be cool don't get in the torunament now :)))
            throw new IllegalStateException("Cannot add participants: tournament is " + status);
        }
        String clientUrl = "http://" + req.ip + ":" + req.port;
        // Magia
        participants.add(new RemoteParticipant(req.name, clientUrl, actionFactory));
    }

    public TournamentResult start() {
        if (status != TournamentStatus.REGISTERING) {
            throw new IllegalStateException("Cannot start tournament: tournament is " + status);
        }
        status = TournamentStatus.RUNNING;
        game.addObserver(broadcaster);
        game.addObserver(new ReplayRecorder(id, replayStore));
        TournamentResult result = format.run(participants, game);
        broadcaster.onTournamentOver(result);
        lastResult = result;
        status = TournamentStatus.COMPLETED;
        return result;
    }

    public TournamentResult getResult() {
        return lastResult;
    }

    public ViewerBroadcaster getBroadcaster() {
        return broadcaster;
    }
}
