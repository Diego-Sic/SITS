package sits.participants;

import java.io.InputStream;
import java.util.Scanner;

import sits.core.Action;
import sits.core.GameHistory;
import sits.core.Participant;
import sits.networking.StringAction;

public class HumanParticipant implements Participant {

    private final String name;
    private final Scanner scanner;

    // Primary constructor for production use
    public HumanParticipant(String name) {
        this(name, System.in);
    }

    // Injectable constructor for tests
    public HumanParticipant(String name, InputStream in) {
        this.name = name;
        this.scanner = new Scanner(in);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Action chooseAction(GameHistory history) {
        if (!history.getRounds().isEmpty()) {
            var last = history.getLastRound();
            System.out.println("Last round: " + last.getActionP1().getLabel()
                    + " vs " + last.getActionP2().getLabel());
        }
        System.out.print("Enter action: ");
        return new StringAction(scanner.nextLine().trim());
    }

    @Override
    public void reset() {
        // no-op: this game isn't complicated enough ( yet :0 )
    }
}
