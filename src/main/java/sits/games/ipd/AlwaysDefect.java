package sits.games.ipd;

import sits.core.Action;
import sits.core.GameHistory;
import sits.core.Participant;

public class AlwaysDefect implements Participant {

    @Override
    public String getName() { return "AlwaysDefect"; }

    @Override
    public Action chooseAction(GameHistory history) { return PrisonerAction.DEFECT; }

    @Override
    public void reset() { /* no internal state */ }
}
