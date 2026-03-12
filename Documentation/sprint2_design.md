# Strategic Interaction Tournament Simulator — Sprint 2
Diego, Mitch, Dewey, Robert

## 1. Problem Introduction

Sprint 2 extends SITS across a network boundary. A robot can now be hosted on one computer while the tournament runs on a separate computer, allowing community members to participate without sharing source code. Less technical users can compete through a human-readable prompt interface.

Three concrete requirements drive the new design:

1. A **Networked Tournament Server** that runs tournaments and accepts client registrations during a dedicated registration phase before the tournament starts.
2. A **Networked Tournament Client** that wraps any `Participant` strategy and exposes it over the network.
3. A **Human Robotic Participant** that prompts a human through standard output and reads their choice from standard input.

The key constraint is that the networking layer must not touch any existing class. The `Participant` interface from Sprint 1 is the exact seam that makes this possible — `RemoteParticipant` simply implements it, and `RoundRobin`, `Game`, and every observer remain completely unaware that some participants live on different machines.


##2. UML Class Diagram

[ See `new_sprint.mmd` ]


##3. New Classes Nuances

The following notes clarify the non-obvious behaviours of the new classes that are not visible from the class diagram alone.

### `StringAction`

`StringAction` is only ever instantiated on the **client side**, inside `GameHistoryDTO.toGameHistory()`. It is never created on the server. Its sole purpose is to satisfy the `Action` type contract when reconstructing a `GameHistory` from JSON — `getLabel()` is the only method that will ever be called on it. It is a transient object that exists just long enough for the local `Participant.chooseAction()` to run and is then discarded.

### `RemoteParticipant`

`getName()` returns the stored name directly — it does **not** make an HTTP call. This matters because `RoundRobin` calls `getName()` frequently when building pairs. The `actionFactory` is injected once at construction time and shared across every call to `chooseAction()` — it is not re-created per round. The HTTP call inside `chooseAction()` is synchronous and blocking by design: `doRound()` cannot proceed until the action arrives, so there is no race condition to manage.

### `NetworkedTournament`

`start()` is a **blocking** call — it does not return until `RoundRobin.run()` completes and the entire tournament is finished. This means the HTTP response to `POST /start` is only sent once all results are ready, which could be a long wait for large tournaments. Additionally, local participants such as `AlwaysCooperate` can be pre-loaded before registration opens — the participant list is intentionally a mix of local and remote entries from the start.

### `ClientApp`

`ClientApp` plays both roles simultaneously: it is a REST server (hosting `ParticipantController` for the tournament server to call) and a REST client (calling `TournamentServerClient` to register). The `@LocalServerPort` injection is only valid after Spring Boot has finished binding the port — reading it in a constructor or `@PostConstruct` will return 0. `ApplicationReadyEvent` is the earliest safe point, which is why registration is triggered there rather than at startup. Because `server.port=0` lets the OS assign any available port, multiple clients can run on the same machine simultaneously with no port conflicts and no manual configuration.


##4. New Design Patterns

### 4.1 Proxy Pattern — `RemoteParticipant`

`RemoteParticipant` implements `Participant` and presents the same interface as `AlwaysCooperate` or `TitForTat`. The caller — `Game.doRound()` — sees none of the HTTP calls happening underneath.

This pattern was chosen because it allows the entire networking concern to be encapsulated in a single class. No other class in the system knows that remote participants exist.

### 4.2 State Pattern — `NetworkedTournament`

`NetworkedTournament` manages a three-state lifecycle: `REGISTERING → RUNNING → COMPLETED`. During `REGISTERING`, new `RemoteParticipant` instances can be added via `addRemoteParticipant()`. When `start()` is called the state moves to `RUNNING`; when `RoundRobin.run()` returns the state moves to `COMPLETED`.

The state machine enforces that registrations are rejected once the tournament has started and that completed tournaments are hidden from the listing endpoint — directly satisfying the user story: "during registration the tournament does not run, but it can accept registrations from clients."


##5. Tricky Relationships and Rationale

### 5.1 `RemoteParticipant` HAS-A `Function<String, Action>` (the `actionFactory`)

`IteratedPrisonersDilemma.getPayoff()` compares actions against `PrisonerAction.COOPERATE` and `PrisonerAction.DEFECT` by enum identity. A label string arriving over the wire cannot satisfy that comparison directly.

The solution is to inject a factory function at tournament setup time. When a networked IPD tournament is created, it provides `PrisonerAction::valueOf` as the factory. `RemoteParticipant.chooseAction()` receives the label `"COOPERATE"` from the client, calls `factory.apply("COOPERATE")`, and returns a real `PrisonerAction.COOPERATE` to the game. The game never sees a string.

This keeps `RemoteParticipant` fully decoupled from any specific game. The same class can serve an RPS tournament by injecting a different factory.

### 5.2 `StringAction` IS-A `Action` (client-side adapter)

`TitForTat` running remotely receives a `GameHistory` populated with `StringAction` objects. Its logic calls `getLastRound().getActionP2()`, which returns a `StringAction`. `TitForTat` returns that object directly. The controller calls `getLabel()` on it to produce `"COOPERATE"`, which travels back to the server where the factory reconstructs the real enum. The chain works end-to-end without modifying `TitForTat` or any other existing participant.

### 5.3 Two REST Servers

A participant client **registering** with a tournament initiates the request — the tournament machine answers. But when the tournament server **requests an action** during a game, the tournament machine initiates the request — the participant machine answers. These are two opposite directions of request.

A single REST server on either machine cannot satisfy both. Both `TournamentServerApp` and `ClientApp` are Spring Boot applications that each host a REST server and also make outbound HTTP calls as REST clients.


##6. Tricky Code

### 6.1 `GameHistoryDTO` — Conversion in Both Directions

The `GameHistory` object cannot cross the network directly because it contains `Action` interface references that JSON cannot reconstruct. `GameHistoryDTO` translates in both directions by reducing actions to their labels on the way out and wrapping them in `StringAction` on the way in.

```java
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
            r.payoffP1, r.payoffP2
        ));
    }
    return h;
}
```

`fromGameHistory` runs on the server side before each POST to `/action`. `toGameHistory` runs on the client side upon receiving that request. The core classes `GameHistory` and `RoundResult` are never modified.

### 6.2 `RemoteParticipant.chooseAction()` and the Full Round-Trip

```java
@Override
public Action chooseAction(GameHistory history) {
    GameHistoryDTO dto = GameHistoryDTO.fromGameHistory(history);
    String label = restTemplate.postForObject(
        clientUrl + "/action", dto, String.class
    );
    return actionFactory.apply(label);
}
```

Three responsibilities in three lines: serialize, call, deserialize.

### 6.3 `ClientApp` — Port Discovery and Self-Registration

```java
@SpringBootApplication
public class ClientApp {

    @LocalServerPort
    private int port;

    @Value("${tournament.server.url}")
    private String serverUrl;

    @Value("${tournament.id}")
    private String tournamentId;

    @Value("${participant.name}")
    private String participantName;

    @Autowired
    private TournamentServerClient client;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() throws UnknownHostException {
        String ip = InetAddress.getLocalHost().getHostAddress();
        client.register(tournamentId, participantName, ip, port);
    }
}
```

The `application.properties` for the client sets `server.port=0` and supplies the tournament server URL, tournament ID, and participant name as the only required configuration.


##7. Sequence Diagram

The diagram below covers the three flows that make up the full system interaction: the registration phase where `ClientApp` auto-discovers its port and registers with the tournament server; the round-by-round execution where each `RemoteParticipant.chooseAction()` triggers an HTTP round-trip to the client machine; and the reset that fires for each participant before every new match.

[ See the next page ]


##8. Changes to Existing Architecture

### 8.1 `pom.xml` — Spring Boot Retained

The Sprint 1 ticket proposed removing Spring Boot from the build. For Sprint 2, Spring Boot is retained because it provides the embedded REST server, Jackson JSON serialization, and `@LocalServerPort` injection with minimal configuration.

**Was this inevitable?** Partially. If the original build had been kept as plain Java 17, Spring Boot would have had to be re-added here. Retaining it avoids that round-trip.

### 8.2 No Changes to Core Classes

`Game`, `Participant`, `TournamentFormat`, `RoundRobin`, `GameHistory`, `RoundResult`, `GameObserver`, `MoveLogger`, and `ScoreLogger` are all unchanged. The DTO classes wrap the domain objects for transport without touching them. `RemoteParticipant` plugs into the `Participant` contract without the contract knowing.

**Was this inevitable?** Yes, given the Sprint 1 design. Defining `Participant` and `TournamentFormat` as pure interfaces with no concrete dependencies was exactly the right call.

### 8.3 `IteratedPrisonersDilemma.getPayoff()` — No Changes

Because `RemoteParticipant` injects an `actionFactory` and returns a real `PrisonerAction` to the game, `getPayoff()` never sees a `StringAction`. The enum identity comparisons remain valid.

**Was this inevitable?** Not entirely. If `getPayoff()` had originally compared by `getLabel()` strings instead of enum identity, no factory would be needed. However, string comparison sacrifices compile-time safety, so the factory approach is the better trade-off.


##9. Alternatives Considered

### 9.1 A Single REST Server

Running only one REST server on the tournament machine with the client polling for instructions was the simplest option. This was rejected because REST is stateless and polling introduces arbitrary latency between when the game needs an action and when the client checks. The two-server model lets the tournament server push requests to the client exactly when needed, keeping the synchronous `play()` loop intact.

### 9.2 Hard-Coded or Manually Configured Client Port

An earlier draft required the operator to manually specify the client port in `application.properties`. This was replaced with `server.port=0` and `@LocalServerPort` because a fixed port creates collisions when multiple clients run on the same machine and introduces a configuration step that fails silently if the chosen port is already in use.

### 9.3 Sending Enum Values Directly Over the Wire

Serializing `PrisonerAction` enum values as typed JSON objects would eliminate `StringAction` entirely. This was rejected because it would couple `ParticipantController` to game-specific types — a client running any participant would need to import `PrisonerAction`, binding the transport layer to a specific game. The label-string approach keeps the client fully game-agnostic.

### 9.4 Modifying `getPayoff()` to Compare by Label

Changing `getPayoff()` to use `a1.getLabel().equals("COOPERATE")` instead of enum identity would remove the need for `actionFactory` — `RemoteParticipant` could return a `StringAction` directly. This was rejected because a typo in a label string would produce wrong scores silently at runtime rather than a compile-time error. The factory preserves the type guarantees of the original design.
