package sits.games.ipd;

import sits.core.Action;
import sits.core.GameHistory;
import sits.core.Participant;

public class TitForTat implements Participant {

    @Override
    public String getName() { return "TitForTat"; }

    @Override
    public Action chooseAction(GameHistory history) {
        if (history.getRounds().isEmpty()) {
            return PrisonerAction.COOPERATE;
        }
        return history.getLastRound().getActionP2();
    }

    @Override
    public void reset() { /* state is fully derived from GameHistory */ }
}
