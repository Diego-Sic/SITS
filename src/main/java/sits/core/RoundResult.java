package sits.core;

public class RoundResult {

    private final Action actionP1;
    private final Action actionP2;
    private final int payoffP1;
    private final int payoffP2;

    public RoundResult(Action actionP1, Action actionP2, int payoffP1, int payoffP2) {
        this.actionP1 = actionP1;
        this.actionP2 = actionP2;
        this.payoffP1 = payoffP1;
        this.payoffP2 = payoffP2;
    }

    public Action getActionP1() { return actionP1; }
    public Action getActionP2() { return actionP2; }
    public int getPayoffP1()    { return payoffP1; }
    public int getPayoffP2()    { return payoffP2; }
}
