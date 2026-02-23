package sits.games.ipd;

import sits.core.Action;
import sits.core.Game;
import sits.core.GameHistory;
import sits.core.GameResult;
import sits.core.Participant;
import sits.core.RoundResult;

public class IteratedPrisonersDilemma extends Game {

    private final int roundLimit;

    public IteratedPrisonersDilemma(int roundLimit) {
        this.roundLimit = roundLimit;
    }

    @Override
    protected RoundResult doRound(Participant p1, Participant p2, GameHistory history) {
        Action a1 = p1.chooseAction(history);
        Action a2 = p2.chooseAction(history);
        int[] payoffs = getPayoff(a1, a2);
        return new RoundResult(a1, a2, payoffs[0], payoffs[1]);
    }

    @Override
    protected boolean isOver(GameHistory history) {
        return history.getRounds().size() >= roundLimit;
    }

    @Override
    protected GameResult computeFinalResult(GameHistory history) {
        return new GameResult(history);
    }

    /**
     * The only place in the system that references PrisonerAction by name.
     * Fails loudly with IllegalArgumentException on unrecognized action types.
     */
    private int[] getPayoff(Action a1, Action a2) {
        if (a1 == PrisonerAction.COOPERATE && a2 == PrisonerAction.COOPERATE) return new int[]{3, 3};
        if (a1 == PrisonerAction.COOPERATE && a2 == PrisonerAction.DEFECT)    return new int[]{0, 5};
        if (a1 == PrisonerAction.DEFECT    && a2 == PrisonerAction.COOPERATE) return new int[]{5, 0};
        if (a1 == PrisonerAction.DEFECT    && a2 == PrisonerAction.DEFECT)    return new int[]{1, 1};
        throw new IllegalArgumentException(
                "Unrecognized actions: " + a1.getLabel() + ", " + a2.getLabel()
        );
    }
}
