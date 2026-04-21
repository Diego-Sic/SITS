package sits.server;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import sits.games.ipd.AlwaysCooperate;
import sits.games.ipd.AlwaysDefect;
import sits.games.ipd.IteratedPrisonersDilemma;
import sits.games.ipd.PrisonerAction;
import sits.games.ipd.TitForTat;
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

    // Seeds one IPD tournament and immediately starts it in the background
    // so a viewer can connect and watch straight away.
    @Bean
    public CommandLineRunner seedTournaments(TournamentRegistry registry, ExecutorService executor) {
        return args -> {
            NetworkedTournament demo = new NetworkedTournament(
                    "ipd-01", "IPD Tournament",
                    new RoundRobin(),
                    new IteratedPrisonersDilemma(10),
                    List.of(new AlwaysCooperate(), new AlwaysDefect(), new TitForTat()),
                    PrisonerAction::valueOf,
                    1000L);
            registry.add(demo);
            executor.submit(demo::start);
        };
    }
}
