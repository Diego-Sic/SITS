package sits.server;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import sits.replay.ReplayStore;

import sits.games.ipd.IteratedPrisonersDilemma;
import sits.games.ipd.PrisonerAction;
import sits.tournament.RoundRobin;

@SpringBootApplication
public class TournamentServerApp {

    public static void main(String[] args) {
        SpringApplication.run(TournamentServerApp.class, args);
    }

    @Bean
    public ExecutorService executorService() {
        return Executors.newCachedThreadPool();
    }

    @Bean
    public ReplayStore replayStore() {
        return new ReplayStore(Path.of("replays"));
    }

    // Seeds one IPD tournament in REGISTERING state so network clients can join before it starts.
    // Start it via POST /tournaments/ipd-01/start or the viewer GUI "Start Tournament" button.
    @Bean
    public CommandLineRunner seedTournaments(TournamentRegistry registry) {
        return args -> {
            NetworkedTournament demo = new NetworkedTournament(
                    "ipd-01", "IPD Tournament",
                    new RoundRobin(),
                    new IteratedPrisonersDilemma(10),
                    List.of(),
                    PrisonerAction::valueOf,
                    1000L);
            registry.add(demo);
        };
    }
}
