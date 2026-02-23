package sits.core;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RoundResultTest {

    private static final Action COOPERATE = () -> "COOPERATE";
    private static final Action DEFECT    = () -> "DEFECT";

    @Test
    void storesActionsForBothPlayers() {
        RoundResult result = new RoundResult(COOPERATE, DEFECT, 0, 5);
        assertThat(result.getActionP1().getLabel()).isEqualTo("COOPERATE");
        assertThat(result.getActionP2().getLabel()).isEqualTo("DEFECT");
    }

    @Test
    void storesPayoffsForBothPlayers() {
        RoundResult result = new RoundResult(COOPERATE, DEFECT, 0, 5);
        assertThat(result.getPayoffP1()).isEqualTo(0);
        assertThat(result.getPayoffP2()).isEqualTo(5);
    }

    @Test
    void mutualCooperationPayoffs() {
        RoundResult result = new RoundResult(COOPERATE, COOPERATE, 3, 3);
        assertThat(result.getPayoffP1()).isEqualTo(3);
        assertThat(result.getPayoffP2()).isEqualTo(3);
    }

    @Test
    void mutualDefectionPayoffs() {
        RoundResult result = new RoundResult(DEFECT, DEFECT, 1, 1);
        assertThat(result.getPayoffP1()).isEqualTo(1);
        assertThat(result.getPayoffP2()).isEqualTo(1);
    }

    @Test
    void actionsAreStoredAsInterfaceType() {
        RoundResult result = new RoundResult(COOPERATE, DEFECT, 0, 5);
        assertThat(result.getActionP1()).isInstanceOf(Action.class);
        assertThat(result.getActionP2()).isInstanceOf(Action.class);
    }
}
