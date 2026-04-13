package sits.networking.dto;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import sits.core.GameHistory;
import sits.core.GameResult;
import sits.core.MoveEvent;
import sits.core.RoundResult;
import sits.core.TournamentResult;
import sits.networking.StringAction;

class MoveEventDTOTest {

    @Test
    void fromMoveEvent_typeIsMove() {
        MoveEventDTO dto = MoveEventDTO.fromMoveEvent(buildMoveEvent());
        assertThat(dto.type).isEqualTo("MOVE");
    }

    @Test
    void fromMoveEvent_roundNumberMatchesHistorySize() {
        GameHistory history = new GameHistory("Alice", "Bob");
        history.getRounds().add(round("C", "D", 3, 5));
        history.getRounds().add(round("D", "D", 1, 1));
        MoveEvent event = new MoveEvent(history.getLastRound(), history);

        assertThat(MoveEventDTO.fromMoveEvent(event).roundNumber).isEqualTo(2);
    }

    @Test
    void fromMoveEvent_playerNamesPreserved() {
        MoveEventDTO dto = MoveEventDTO.fromMoveEvent(buildMoveEvent());
        assertThat(dto.p1Name).isEqualTo("Alice");
        assertThat(dto.p2Name).isEqualTo("Bob");
    }

    @Test
    void fromMoveEvent_actionLabelsPreserved() {
        MoveEventDTO dto = MoveEventDTO.fromMoveEvent(buildMoveEvent());
        assertThat(dto.action1).isEqualTo("COOPERATE");
        assertThat(dto.action2).isEqualTo("DEFECT");
    }

    @Test
    void fromMoveEvent_payoffsPreserved() {
        MoveEventDTO dto = MoveEventDTO.fromMoveEvent(buildMoveEvent());
        assertThat(dto.payoff1).isEqualTo(3);
        assertThat(dto.payoff2).isEqualTo(5);
    }

    @Test
    void fromMoveEvent_gameOverFieldsAreNullOrZero() {
        MoveEventDTO dto = MoveEventDTO.fromMoveEvent(buildMoveEvent());
        assertThat(dto.winner).isNull();
        assertThat(dto.totalScore1).isZero();
        assertThat(dto.totalScore2).isZero();
    }

    // Game Over parts

    @Test
    void gameOver_typeIsGameOver() {
        assertThat(MoveEventDTO.gameOver(buildGameResult()).type).isEqualTo("GAME_OVER");
    }

    @Test
    void gameOver_playerNamesPreserved() {
        MoveEventDTO dto = MoveEventDTO.gameOver(buildGameResult());
        assertThat(dto.p1Name).isEqualTo("Alice");
        assertThat(dto.p2Name).isEqualTo("Bob");
    }

    @Test
    void gameOver_winnerIsHighScorer() {
        GameHistory history = new GameHistory("Alice", "Bob");
        history.getRounds().add(round("C", "D", 0, 5)); // Bob wins
        MoveEventDTO dto = MoveEventDTO.gameOver(new GameResult(history));
        assertThat(dto.winner).isEqualTo("Bob");
    }

    @Test
    void gameOver_winnerIsDraw() {
        GameHistory history = new GameHistory("Alice", "Bob");
        history.getRounds().add(round("D", "D", 1, 1));
        MoveEventDTO dto = MoveEventDTO.gameOver(new GameResult(history));
        assertThat(dto.winner).isEqualTo("DRAW");
    }

    @Test
    void gameOver_totalScoresPreserved() {
        GameHistory history = new GameHistory("Alice", "Bob");
        history.getRounds().add(round("C", "C", 3, 3));
        history.getRounds().add(round("D", "C", 5, 0));
        MoveEventDTO dto = MoveEventDTO.gameOver(new GameResult(history));
        assertThat(dto.totalScore1).isEqualTo(8);
        assertThat(dto.totalScore2).isEqualTo(3);
    }

    @Test
    void gameOver_moveFieldsAreNullOrZero() {
        MoveEventDTO dto = MoveEventDTO.gameOver(buildGameResult());
        assertThat(dto.roundNumber).isZero();
        assertThat(dto.action1).isNull();
        assertThat(dto.action2).isNull();
    }

    // When the tournament is over

    @Test
    void tournamentOver_typeIsTournamentOver() {
        TournamentResult result = new TournamentResult(List.of(buildGameResult()));
        assertThat(MoveEventDTO.tournamentOver(result).type).isEqualTo("TOURNAMENT_OVER");
    }

    @Test
    void tournamentOver_allOtherFieldsAreNullOrZero() {
        TournamentResult result = new TournamentResult(List.of(buildGameResult()));
        MoveEventDTO dto = MoveEventDTO.tournamentOver(result);
        assertThat(dto.p1Name).isNull();
        assertThat(dto.p2Name).isNull();
        assertThat(dto.winner).isNull();
        assertThat(dto.roundNumber).isZero();
        assertThat(dto.totalScore1).isZero();
        assertThat(dto.totalScore2).isZero();
    }

    // We created some helper methods to build the necessary objects for our tests

    private MoveEvent buildMoveEvent() {
        GameHistory history = new GameHistory("Alice", "Bob");
        RoundResult roundResult = round("COOPERATE", "DEFECT", 3, 5);
        history.getRounds().add(roundResult);
        return new MoveEvent(roundResult, history);
    }

    private GameResult buildGameResult() {
        GameHistory history = new GameHistory("Alice", "Bob");
        history.getRounds().add(round("C", "C", 3, 3));
        return new GameResult(history);
    }

    private RoundResult round(String a1, String a2, int p1, int p2) {
        return new RoundResult(new StringAction(a1), new StringAction(a2), p1, p2);
    }
}
