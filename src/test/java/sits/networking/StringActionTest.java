package sits.networking;

import org.junit.jupiter.api.Test;
import sits.core.Action;

import static org.assertj.core.api.Assertions.assertThat;

class StringActionTest {

    @Test
    void getLabel_returnsConstructorArgument() {
        StringAction action = new StringAction("COOPERATE");
        assertThat(action.getLabel()).isEqualTo("COOPERATE");
    }

    @Test
    void implementsAction() {
        StringAction action = new StringAction("DEFECT");
        assertThat(action).isInstanceOf(Action.class);
    }
}
