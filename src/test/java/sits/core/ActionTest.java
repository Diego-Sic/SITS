package sits.core;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ActionTest {

    @Test
    void labelIsReturnedCorrectly() {
        Action action = () -> "COOPERATE";
        assertThat(action.getLabel()).isEqualTo("COOPERATE");
    }

    @Test
    void labelCanBeAnyString() {
        Action action = () -> "ROCK";
        assertThat(action.getLabel()).isEqualTo("ROCK");
    }

    @Test
    void differentImplementationsReturnDifferentLabels() {
        Action a1 = () -> "DEFECT";
        Action a2 = () -> "COOPERATE";
        assertThat(a1.getLabel()).isNotEqualTo(a2.getLabel());
    }
}
