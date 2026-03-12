package sits.networking.dto;

public class RoundResultDTO {

    public String actionP1;
    public String actionP2;
    public int payoffP1;
    public int payoffP2;

    public RoundResultDTO() {}

    public RoundResultDTO(String actionP1, String actionP2, int payoffP1, int payoffP2) {
        this.actionP1 = actionP1;
        this.actionP2 = actionP2;
        this.payoffP1 = payoffP1;
        this.payoffP2 = payoffP2;
    }
}
