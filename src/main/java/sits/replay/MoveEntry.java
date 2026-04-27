package sits.replay;

public record MoveEntry(
                int roundNumber,
                String nameP1,
                String nameP2,
                String actionP1,
                String actionP2,
                int payoffP1,
                int payoffP2) {
}
