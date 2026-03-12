package sits.networking.dto;

import org.junit.jupiter.api.Test;
import sits.core.GameHistory;
import sits.core.RoundResult;
import sits.networking.StringAction;

import static org.assertj.core.api.Assertions.assertThat;

class GameHistoryDTOTest {

    @Test
    void fromGameHistory_preservesPlayerNames() {
        GameHistory h = new GameHistory("Alice", "Bob");
        GameHistoryDTO dto = GameHistoryDTO.fromGameHistory(h);
        assertThat(dto.nameP1).isEqualTo("Alice");
        assertThat(dto.nameP2).isEqualTo("Bob");
    }

    @Test
    void fromGameHistory_preservesRoundCount() {
        GameHistory h = new GameHistory("Alice", "Bob");
        h.getRounds().add(new RoundResult(new StringAction("COOPERATE"), new StringAction("DEFECT"), 3, 5));
        h.getRounds().add(new RoundResult(new StringAction("DEFECT"), new StringAction("DEFECT"), 1, 1));
        GameHistoryDTO dto = GameHistoryDTO.fromGameHistory(h);
        assertThat(dto.rounds).hasSize(2);
    }

    @Test
    void fromGameHistory_preservesActionLabels() {
        GameHistory h = new GameHistory("Alice", "Bob");
        h.getRounds().add(new RoundResult(new StringAction("COOPERATE"), new StringAction("DEFECT"), 3, 5));
        GameHistoryDTO dto = GameHistoryDTO.fromGameHistory(h);
        assertThat(dto.rounds.get(0).actionP1).isEqualTo("COOPERATE");
        assertThat(dto.rounds.get(0).actionP2).isEqualTo("DEFECT");
    }

    @Test
    void toGameHistory_wrapsLabelsInStringAction() {
        GameHistoryDTO dto = new GameHistoryDTO();
        dto.nameP1 = "Alice";
        dto.nameP2 = "Bob";
        RoundResultDTO r = new RoundResultDTO("COOPERATE", "DEFECT", 3, 5);
        dto.rounds = java.util.List.of(r);

        GameHistory h = dto.toGameHistory();
        assertThat(h.getRounds().get(0).getActionP1().getLabel()).isEqualTo("COOPERATE");
        assertThat(h.getRounds().get(0).getActionP2().getLabel()).isEqualTo("DEFECT");
    }

    @Test
    void roundTrip_emptyHistory() {
        GameHistory original = new GameHistory("Alice", "Bob");
        GameHistory result = GameHistoryDTO.fromGameHistory(original).toGameHistory();
        assertThat(result.getNameP1()).isEqualTo("Alice");
        assertThat(result.getNameP2()).isEqualTo("Bob");
        assertThat(result.getRounds()).isEmpty();
    }

    @Test
    void roundTrip_multipleRounds() {
        GameHistory original = new GameHistory("Alice", "Bob");
        original.getRounds().add(new RoundResult(new StringAction("COOPERATE"), new StringAction("COOPERATE"), 3, 3));
        original.getRounds().add(new RoundResult(new StringAction("DEFECT"), new StringAction("COOPERATE"), 5, 0));

        GameHistory result = GameHistoryDTO.fromGameHistory(original).toGameHistory();
        assertThat(result.getRounds()).hasSize(2);
        assertThat(result.getRounds().get(0).getActionP1().getLabel()).isEqualTo("COOPERATE");
        assertThat(result.getRounds().get(1).getActionP1().getLabel()).isEqualTo("DEFECT");
        assertThat(result.getRounds().get(1).getPayoffP1()).isEqualTo(5);
        assertThat(result.getRounds().get(1).getPayoffP2()).isEqualTo(0);
    }
}
