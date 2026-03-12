package sits.networking.dto;

import sits.core.GameHistory;
import sits.core.RoundResult;
import sits.networking.StringAction;

import java.util.ArrayList;
import java.util.List;

public class GameHistoryDTO {

    public String nameP1;
    public String nameP2;
    public List<RoundResultDTO> rounds;

    public GameHistoryDTO() {}

    public GameHistoryDTO(String nameP1, String nameP2, List<RoundResultDTO> rounds) {
        this.nameP1 = nameP1;
        this.nameP2 = nameP2;
        this.rounds = rounds;
    }

    public static GameHistoryDTO fromGameHistory(GameHistory h) {
        List<RoundResultDTO> dtos = new ArrayList<>();
        for (RoundResult r : h.getRounds()) {
            dtos.add(new RoundResultDTO(
                r.getActionP1().getLabel(),
                r.getActionP2().getLabel(),
                r.getPayoffP1(),
                r.getPayoffP2()
            ));
        }
        return new GameHistoryDTO(h.getNameP1(), h.getNameP2(), dtos);
    }

    public GameHistory toGameHistory() {
        GameHistory h = new GameHistory(nameP1, nameP2);
        for (RoundResultDTO r : rounds) {
            h.getRounds().add(new RoundResult(
                new StringAction(r.actionP1),
                new StringAction(r.actionP2),
                r.payoffP1,
                r.payoffP2
            ));
        }
        return h;
    }
}
