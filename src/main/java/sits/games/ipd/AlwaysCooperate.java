package sits.games.ipd;

import sits.core.Action;
import sits.core.GameHistory;
import sits.core.Participant;

public class AlwaysCooperate implements Participant {

    @Override
    public String getName() { return "AlwaysCooperate"; }

    @Override
    public Action chooseAction(GameHistory history) { return PrisonerAction.COOPERATE; }

    @Override
    public void reset() { /* no internal state */ }
}
