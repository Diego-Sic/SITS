package sits.games.ipd;

import sits.core.Action;

public enum PrisonerAction implements Action {
    COOPERATE, DEFECT;

    @Override
    public String getLabel() { return name(); }
}
