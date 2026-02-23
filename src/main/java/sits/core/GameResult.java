package sits.core;

public class GameResult {

    private final GameHistory history;
    private final String winner;
    private final int totalScoreP1;
    private final int totalScoreP2;

    public GameResult(GameHistory history) {
        this.history = history;
        this.totalScoreP1 = sumPayoffsP1(history);
        this.totalScoreP2 = sumPayoffsP2(history);
        this.winner = determineWinner(history.getNameP1(), history.getNameP2());
    }

    private int sumPayoffsP1(GameHistory history) {
        return history.getRounds().stream().mapToInt(RoundResult::getPayoffP1).sum();
    }

    private int sumPayoffsP2(GameHistory history) {
        return history.getRounds().stream().mapToInt(RoundResult::getPayoffP2).sum();
    }

    private String determineWinner(String nameP1, String nameP2) {
        if (totalScoreP1 > totalScoreP2) return nameP1;
        if (totalScoreP2 > totalScoreP1) return nameP2;
        return "DRAW";
    }

    public GameHistory getHistory()   { return history; }
    public String getWinner()         { return winner; }
    public int getTotalScoreP1()      { return totalScoreP1; }
    public int getTotalScoreP2()      { return totalScoreP2; }
}
