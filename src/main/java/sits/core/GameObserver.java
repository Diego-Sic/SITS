package sits.core;

public interface GameObserver {
    void onMoveMade(MoveEvent event);
    void onGameOver(GameResult result);
    void onTournamentOver(TournamentResult result);
}
