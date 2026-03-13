package sits.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import sits.core.Participant;
import sits.games.ipd.TitForTat;

@Configuration
public class ClientConfig {

    @Bean
    public Participant participant() {
        // This is temporal ( osea que hay que cambiarlo)
        return new TitForTat();
    }

    @Bean
    public TournamentServerClient tournamentServerClient(
            @Value("${tournament.server.url}") String url) {
        return new TournamentServerClient(url);
    }
}
