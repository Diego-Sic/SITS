package sits.networking.dto;

import sits.core.GameResult;
import sits.core.MoveEvent;
import sits.core.TournamentResult;

public class MoveEventDTO {

    public String type;
    public int    roundNumber;
    public String p1Name;
    public String p2Name;
    public String action1;
    public String action2;
    public int    payoff1;
    public int    payoff2;
    public String winner;
    public int    totalScore1;
    public int    totalScore2;

    public MoveEventDTO() {}

    public static MoveEventDTO fromMoveEvent(MoveEvent event) {
        MoveEventDTO dto = new MoveEventDTO();
        dto.type        = "MOVE";
        dto.roundNumber = event.getHistory().getRounds().size();
        dto.p1Name      = event.getHistory().getNameP1();
        dto.p2Name      = event.getHistory().getNameP2();
        dto.action1     = event.getRound().getActionP1().getLabel();
        dto.action2     = event.getRound().getActionP2().getLabel();
        dto.payoff1     = event.getRound().getPayoffP1();
        dto.payoff2     = event.getRound().getPayoffP2();
        return dto;
    }

    public static MoveEventDTO gameOver(GameResult result) {
        MoveEventDTO dto = new MoveEventDTO();
        dto.type        = "GAME_OVER";
        dto.p1Name      = result.getHistory().getNameP1();
        dto.p2Name      = result.getHistory().getNameP2();
        dto.winner      = result.getWinner();
        dto.totalScore1 = result.getTotalScoreP1();
        dto.totalScore2 = result.getTotalScoreP2();
        return dto;
    }

    public static MoveEventDTO tournamentOver(TournamentResult result) {
        MoveEventDTO dto = new MoveEventDTO();
        dto.type = "TOURNAMENT_OVER";
        return dto;
    }
}
