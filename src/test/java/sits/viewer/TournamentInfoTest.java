package sits.viewer;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class TournamentInfoTest {

    @Test
    void fields_canBeSetAndRead() {
        TournamentInfo info = new TournamentInfo();
        info.id     = "t1";
        info.name   = "IPD Tournament";
        info.status = "REGISTERING";

        assertThat(info.id).isEqualTo("t1");
        assertThat(info.name).isEqualTo("IPD Tournament");
        assertThat(info.status).isEqualTo("REGISTERING");
    }

    @Test
    void defaultConstructor_fieldsAreNull() {
        TournamentInfo info = new TournamentInfo();
        assertThat(info.id).isNull();
        assertThat(info.name).isNull();
        assertThat(info.status).isNull();
    }
}
