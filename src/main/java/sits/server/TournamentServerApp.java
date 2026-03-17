package sits.server;

import java.util.List;
import java.util.function.Function;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import sits.core.Action;
import sits.core.Participant;
import sits.games.ipd.AlwaysCooperate;
import sits.games.ipd.AlwaysDefect;
import sits.games.ipd.IteratedPrisonersDilemma;
import sits.games.ipd.PrisonerAction;
import sits.tournament.RoundRobin;

@SpringBootApplication
public class TournamentServerApp {

    public static void main(String[] args) {
        SpringApplication.run(TournamentServerApp.class, args);
    }

    @Bean
    public CommandLineRunner seedTournaments(TournamentRegistry registry) {
        return args -> {
            Function<String, Action> factory = PrisonerAction::valueOf;
            List<Participant> locals = List.of(new AlwaysCooperate(), new AlwaysDefect());
            NetworkedTournament t = new NetworkedTournament(
                "ipd-01", "IPD Tournament",
                new RoundRobin(), new IteratedPrisonersDilemma(10),
                locals, factory
            );
            registry.add(t);
        };
    }
}
