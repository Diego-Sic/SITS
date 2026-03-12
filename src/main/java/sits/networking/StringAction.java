package sits.networking;

import sits.core.Action;

public class StringAction implements Action {

    private final String label;

    public StringAction(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
