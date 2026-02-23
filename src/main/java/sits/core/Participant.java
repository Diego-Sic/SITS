package sits.core;

public interface Participant {
    String getName();
    Action chooseAction(GameHistory history);
    void reset();
}
