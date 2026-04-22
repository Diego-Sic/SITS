# Sprint 2 Tickets — Networked Tournament

## Overview

Sprint 2 extends SITS across a network boundary. Work is broken into 12 tickets that build on each other in dependency order. New code lives in three new packages:

- `sits.networking` — shared transport types (`StringAction`, `RemoteParticipant`)
- `sits.networking.dto` — JSON data transfer objects
- `sits.server` — server-side Spring Boot application
- `sits.client` — client-side Spring Boot application
- `sits.participants` — game-agnostic participants (`HumanParticipant`)

**Zero changes** to any existing class in `sits.core`, `sits.games.ipd`, `sits.tournament`, or `sits.logging`.

---

## S2-01 · `StringAction`

**Package:** `sits.networking`

**What it does:**
A minimal `Action` implementation that wraps a raw label string. Used exclusively on the **client side** inside `GameHistoryDTO.toGameHistory()` to satisfy the `Action` type contract when reconstructing a `GameHistory` from JSON. No other code ever instantiates it directly.

**Fields:**

```java
private final String label;
```

**Constructor:**

```java
public StringAction(String label)
```

**Methods:**

```java
@Override public String getLabel() // returns label
```

**Acceptance criteria:**

- Implements `sits.core.Action`
- `getLabel()` returns exactly the string passed to the constructor
- Is immutable (field is final)
- No other methods needed

**Tests to write (`sits.networking.StringActionTest`):**

- `getLabel_returnsConstructorArgument()`
- `implementsAction()`

---

## S2-02 · DTOs — `RoundResultDTO`, `GameHistoryDTO`, `RegistrationRequest`

**Package:** `sits.networking.dto`

**What they do:**
Plain data classes used for JSON serialization/deserialization. Jackson requires a no-arg constructor and public fields (or getters).

### `RoundResultDTO`

```java
public String actionP1;
public String actionP2;
public int payoffP1;
public int payoffP2;
```

All-args constructor + no-arg constructor for Jackson.

### `GameHistoryDTO`

```java
public String nameP1;
public String nameP2;
public List<RoundResultDTO> rounds;
```

Two conversion methods (both reference `StringAction` from S2-01):

```java
// Server side — called before each POST /action
public static GameHistoryDTO fromGameHistory(GameHistory h)

// Client side — called upon receiving POST /action
public GameHistory toGameHistory()
```

`fromGameHistory` iterates `h.getRounds()`, calls `getLabel()` on each action, and builds `RoundResultDTO` entries.
`toGameHistory` iterates `rounds`, wraps each label in `new StringAction(...)`, and builds a real `GameHistory`.

### `RegistrationRequest`

```java
public String name;
public String ip;
public int port;
```

No-arg constructor + all-args constructor.

**Acceptance criteria:**

- All three classes serialize/deserialize with Jackson (no `@JsonProperty` needed if using public fields)
- `GameHistoryDTO.fromGameHistory(h).toGameHistory()` round-trips correctly — player names preserved, round count preserved, labels preserved
- `GameHistory` and `RoundResult` are **never modified**

**Tests to write (`sits.networking.dto.GameHistoryDTOTest`):**

- `fromGameHistory_preservesPlayerNames()`
- `fromGameHistory_preservesRoundCount()`
- `fromGameHistory_preservesActionLabels()`
- `toGameHistory_wrapsLabelsInStringAction()`
- `roundTrip_emptyHistory()`
- `roundTrip_multipleRounds()`

---

## S2-03 · `RemoteParticipant` [COMPLETED]

**Package:** `sits.networking`

**What it does:**
A proxy that implements `Participant`. Callers (e.g., `Game.doRound()`) see it as a normal participant. Underneath, each `chooseAction()` call makes a synchronous HTTP POST to the client machine and converts the returned label string back into a real `Action` using an injected factory.

**Fields:**

```java
private final String name;
private final String clientUrl;          // e.g. "http://192.168.1.5:51234"
private final Function<String, Action> actionFactory;
private final RestTemplate restTemplate;
```

**Constructor:**

```java
public RemoteParticipant(String name, String clientUrl, Function<String, Action> actionFactory)
// Internally creates a new RestTemplate
```

**Methods:**

```java
@Override public String getName()   // returns name — NO HTTP call
@Override public Action chooseAction(GameHistory history) {
    GameHistoryDTO dto = GameHistoryDTO.fromGameHistory(history);
    String label = restTemplate.postForObject(clientUrl + "/action", dto, String.class);
    return actionFactory.apply(label);
}
@Override public void reset() {
    restTemplate.postForObject(clientUrl + "/reset", null, Void.class);
}
```

**Acceptance criteria:**

- Implements `sits.core.Participant`
- `getName()` never calls the network
- `chooseAction()` POSTs to `{clientUrl}/action` with a `GameHistoryDTO` body and applies the factory to the response
- `reset()` POSTs to `{clientUrl}/reset`
- `actionFactory` is injected once at construction and reused across all calls

**Tests to write (`sits.networking.RemoteParticipantTest`):**
Use `MockRestServiceServer` or a stub `RestTemplate` to avoid real HTTP:

- `getName_returnsNameWithoutHttp()`
- `chooseAction_postsToClientUrl()`
- `chooseAction_appliesFactoryToLabel()`
- `chooseAction_serializesHistoryAsDTO()`
- `reset_postsToResetEndpoint()`

---

## S2-04 · `HumanParticipant` [COMPLETED]

**Package:** `sits.participants`

**What it does:**
Prompts a human through `System.out` and reads their choice from `System.in`. Works with any game — it prints the available labels from the last round (if any) and asks the human to type one.

**Fields:**

```java
private final String name;
private final Scanner scanner;
```

**Constructor:**

```java
public HumanParticipant(String name)         // uses System.in
public HumanParticipant(String name, InputStream in)  // injectable for tests
```

**Methods:**

```java
@Override public String getName()
@Override public Action chooseAction(GameHistory history) {
    // Print last round if history is non-empty
    // Print prompt: "Enter action: "
    // Read a line and return new StringAction(line.trim())
}
@Override public void reset() { /* no-op */ }
```

**Acceptance criteria:**

- Implements `sits.core.Participant`
- `chooseAction()` returns a `StringAction` wrapping whatever the user types
- Accepts an injectable `InputStream` so tests do not block on stdin
- `reset()` is a no-op

**Tests to write (`sits.participants.HumanParticipantTest`):**

- `getName_returnsName()`
- `chooseAction_returnsStringActionFromInput()`
- `chooseAction_trimsWhitespace()`
- `reset_doesNotThrow()`

---

## S2-05 · `TournamentStatus` + `NetworkedTournament` [COMPLETED]

**Package:** `sits.server`

### `TournamentStatus`

```java
public enum TournamentStatus { REGISTERING, RUNNING, COMPLETED }
```

### `NetworkedTournament`

Manages a three-state lifecycle. Wraps any `TournamentFormat` + `Game` combination and exposes registration and start operations.

**Fields:**

```java
private final String id;
private final String name;
private final TournamentFormat format;
private final Game game;
private final List<Participant> participants;   // mutable list — add remote participants during REGISTERING
private TournamentStatus status;               // starts as REGISTERING
```

**Constructor:**

```java
public NetworkedTournament(String id, String name, TournamentFormat format, Game game, List<Participant> initialParticipants)
// status = REGISTERING; participants list is a new ArrayList initialized from initialParticipants
```

**Methods:**

```java
public String getId()
public String getName()
public TournamentStatus getStatus()

public void addRemoteParticipant(RegistrationRequest req) {
    // Throws IllegalStateException if status != REGISTERING
    // Creates RemoteParticipant with PrisonerAction::valueOf as factory (or injected factory)
    // Appends to participants list
}

public TournamentResult start() {
    // Throws IllegalStateException if status != REGISTERING
    // status = RUNNING
    // TournamentResult result = format.run(participants, game)
    // status = COMPLETED
    // return result
}
```

**Note on `actionFactory`:** To keep `NetworkedTournament` game-agnostic, inject the factory:

```java
public NetworkedTournament(String id, String name, TournamentFormat format, Game game,
                           List<Participant> initialParticipants,
                           Function<String, Action> actionFactory)
```

**Acceptance criteria:**

- Starts in `REGISTERING` state
- `addRemoteParticipant()` throws `IllegalStateException` if not `REGISTERING`
- `start()` throws `IllegalStateException` if not `REGISTERING`
- `start()` transitions `REGISTERING → RUNNING → COMPLETED` and returns the result
- Local (pre-loaded) participants run alongside remote ones

**Tests to write (`sits.server.NetworkedTournamentTest`):**

- `initialStatus_isRegistering()`
- `addRemoteParticipant_addsParticipant()`
- `addRemoteParticipant_throwsWhenRunning()`
- `addRemoteParticipant_throwsWhenCompleted()`
- `start_throwsWhenAlreadyRunning()` (use a second call)
- `start_transitionsToCompleted()`
- `start_includesLocalAndRemoteParticipants()`

---

## S2-06 · `TournamentRegistry`

**Package:** `sits.server`

**What it does:**
An in-memory registry of all `NetworkedTournament` instances, keyed by ID.

**Fields:**

```java
private final Map<String, NetworkedTournament> tournaments = new LinkedHashMap<>();
```

**Methods:**

```java
public void add(NetworkedTournament t)
public NetworkedTournament get(String id)              // returns null if not found
public List<NetworkedTournament> listRegistering()     // only REGISTERING tournaments
```

**Acceptance criteria:**

- `add()` stores the tournament under its `getId()` key
- `get()` retrieves by ID
- `listRegistering()` excludes `RUNNING` and `COMPLETED` tournaments
- Spring `@Component` so it can be injected

**Tests to write (`sits.server.TournamentRegistryTest`):**

- `add_and_get_byId()`
- `get_unknownId_returnsNull()`
- `listRegistering_excludesRunning()`
- `listRegistering_excludesCompleted()`
- `listRegistering_includesRegistering()`

---

## S2-07 · `TournamentServerController` [COMPLETED]

**Package:** `sits.server`

**What it does:**
Spring `@RestController` that exposes three endpoints for the tournament server.

**Endpoints:**

| Method | Path                         | Description                           |
| ------ | ---------------------------- | ------------------------------------- |
| `GET`  | `/tournaments`               | Returns all `REGISTERING` tournaments |
| `POST` | `/tournaments/{id}/register` | Registers a remote participant        |
| `POST` | `/tournaments/{id}/start`    | Starts the tournament (blocking)      |

**Implementation:**

```java
@RestController
public class TournamentServerController {

    private final TournamentRegistry registry;

    // Constructor injection

    @GetMapping("/tournaments")
    public List<NetworkedTournament> getTournaments() {
        return registry.listRegistering();
    }

    @PostMapping("/tournaments/{id}/register")
    public ResponseEntity<Void> register(@PathVariable String id,
                                         @RequestBody RegistrationRequest body) {
        NetworkedTournament t = registry.get(id);
        if (t == null) return ResponseEntity.notFound().build();
        t.addRemoteParticipant(body);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/tournaments/{id}/start")
    public ResponseEntity<TournamentResult> start(@PathVariable String id) {
        NetworkedTournament t = registry.get(id);
        if (t == null) return ResponseEntity.notFound().build();
        TournamentResult result = t.start();
        return ResponseEntity.ok(result);
    }
}
```

**Acceptance criteria:**

- Returns 404 if tournament ID is unknown
- `POST /register` delegates to `NetworkedTournament.addRemoteParticipant()`
- `POST /start` is blocking — response is only sent after the full tournament completes
- Uses constructor injection (no field injection)

**Tests to write (`sits.server.TournamentServerControllerTest`):**
Use `MockMvc` with `@WebMvcTest`:

- `getTournaments_returnsRegisteringList()`
- `register_unknownId_returns404()`
- `register_validId_returns200()`
- `start_unknownId_returns404()`
- `start_validId_returnsTournamentResult()`

---

## S2-08 · `ParticipantController` [COMPLETED]

**Package:** `sits.client`

**What it does:**
Spring `@RestController` hosted on the **client machine**. The tournament server calls these endpoints when it needs an action or a reset.

**Endpoints:**

| Method | Path      | Description                                                      |
| ------ | --------- | ---------------------------------------------------------------- |
| `GET`  | `/name`   | Returns participant name                                         |
| `POST` | `/action` | Accepts `GameHistoryDTO`, returns action label as plain `String` |
| `POST` | `/reset`  | Resets participant state                                         |

**Implementation:**

```java
@RestController
public class ParticipantController {

    private final Participant participant;

    // Constructor injection

    @GetMapping("/name")
    public String getName() {
        return participant.getName();
    }

    @PostMapping("/action")
    public String getAction(@RequestBody GameHistoryDTO dto) {
        GameHistory history = dto.toGameHistory();
        Action action = participant.chooseAction(history);
        return action.getLabel();
    }

    @PostMapping("/reset")
    public void reset() {
        participant.reset();
    }
}
```

**Acceptance criteria:**

- `POST /action` deserializes `GameHistoryDTO`, calls `participant.chooseAction()`, returns the label string
- `POST /reset` calls `participant.reset()`
- Works with any `Participant` implementation (strategy-agnostic)
- Uses constructor injection

**Tests to write (`sits.client.ParticipantControllerTest`):**
Use `MockMvc` with `@WebMvcTest`:

- `getName_returnsParticipantName()`
- `getAction_callsChooseActionWithHistory()`
- `getAction_returnsActionLabel()`
- `reset_callsParticipantReset()`

---

## S2-09 · `TournamentServerClient` [COMPLETED]

**Package:** `sits.client`

**What it does:**
A thin REST client (Spring `@Component`) used by `ClientApp` to communicate with the tournament server. Wraps `RestTemplate` calls.

**Fields:**

```java
private final String serverUrl;
private final RestTemplate restTemplate;
```

**Constructor:**

```java
public TournamentServerClient(@Value("${tournament.server.url}") String serverUrl)
```

**Methods:**

```java
public List<NetworkedTournament> listTournaments() {
    // GET {serverUrl}/tournaments
}

public void register(String tournamentId, String name, String ip, int port) {
    RegistrationRequest req = new RegistrationRequest(name, ip, port);
    // POST {serverUrl}/tournaments/{tournamentId}/register
}
```

**Acceptance criteria:**

- `listTournaments()` GETs `/tournaments` and returns the list
- `register()` POSTs a `RegistrationRequest` to `/tournaments/{id}/register`
- `serverUrl` is injected from `application.properties`

**Tests to write (`sits.client.TournamentServerClientTest`):**
Use `MockRestServiceServer`:

- `listTournaments_callsCorrectUrl()`
- `register_postsToCorrectUrl()`
- `register_sendsCorrectBody()`

---

## S2-10 · `ClientApp` [COMPLETADO]

**Package:** `sits.client`

**What it does:**
Spring Boot entry point for the participant client. Starts an embedded server on a random port (`server.port=0`), then at `ApplicationReadyEvent` discovers its own port via `@LocalServerPort`, resolves the local machine IP, and registers with the tournament server.

**Implementation sketch:**

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

**Required `application.properties` (client):**

```properties
server.port=0
tournament.server.url=http://<server-ip>:8080
tournament.id=<id>
participant.name=<name>
```

**`@Bean` configuration (in the same class or a separate `ClientConfig`):**

```java
@Bean
public Participant participant() {
    return new TitForTat();   // swap strategy here
}

@Bean
public TournamentServerClient tournamentServerClient(
        @Value("${tournament.server.url}") String url) {
    return new TournamentServerClient(url);
}
```

**Acceptance criteria:**

- `@LocalServerPort` is only read at `ApplicationReadyEvent` (not in constructor)
- Multiple `ClientApp` instances can run on the same machine simultaneously (random port)
- The `Participant` bean is injectable into `ParticipantController`

**Tests to write (`sits.client.ClientAppIntegrationTest`):**
Use `@SpringBootTest(webEnvironment = RANDOM_PORT)` with a mock server:

- `onReady_registersWithTournamentServer()`
- `portIsNonZeroAfterStartup()`

---

## S2-11 · `TournamentServerApp` [COMPLETADO]

**Package:** `sits.server`

**What it does:**
Spring Boot entry point for the tournament server. Bootstraps the application context, which includes `TournamentRegistry` and `TournamentServerController`. Pre-loads `NetworkedTournament` instances (with any local participants) into the registry via a `CommandLineRunner` or `@PostConstruct` bean.

**Implementation sketch:**

```java
@SpringBootApplication
public class TournamentServerApp {
    public static void main(String[] args) {
        SpringApplication.run(TournamentServerApp.class, args);
    }

    @Bean
    public CommandLineRunner seedTournaments(TournamentRegistry registry) {
        return args -> {
            Function<String, Action> factory = PrisonerAction::valueOf;
            List<Participant> locals = List.of(new AlwaysCooperate(), new AlwaysDefect());
            NetworkedTournament t = new NetworkedTournament(
                "ipd-01", "IPD Tournament",
                new RoundRobin(), new IteratedPrisonersDilemma(10),
                locals, factory
            );
            registry.add(t);
        };
    }
}
```

**Required `application.properties` (server):**

```properties
server.port=8080
```

**Acceptance criteria:**

- Server starts on port 8080
- At least one tournament is pre-seeded and appears in `GET /tournaments`
- `TournamentRegistry` is a singleton `@Component`

**Tests to write (`sits.server.TournamentServerAppTest`):**

- `context_loads()` — `@SpringBootTest` smoke test
- `seedTournaments_registersOneTournament()`

---

## S2-12 · End-to-End Integration Test

**Package:** `sits.integration`
**Class:** `NetworkedTournamentIntegrationTest`

**What it does:**
Spins up a full `TournamentServerApp` on a random port, starts two `ClientApp` instances pointing at it (both on random ports), verifies registration, starts the tournament, and asserts the result.

**Test outline:**

```java
@SpringBootTest(classes = TournamentServerApp.class, webEnvironment = RANDOM_PORT)
class NetworkedTournamentIntegrationTest {

    @LocalServerPort int serverPort;

    @Test
    void twoRemoteClientsCompleteIPDTournament() {
        // 1. Verify GET /tournaments returns one REGISTERING tournament
        // 2. Register two remote participants via POST /tournaments/{id}/register
        //    (use stub HTTP servers or real ClientApp instances)
        // 3. POST /tournaments/{id}/start
        // 4. Assert TournamentResult has game results for all participant pairs
        // 5. Assert tournament status is COMPLETED
    }
}
```

**Acceptance criteria:**

- Tournament reaches `COMPLETED` status
- `TournamentResult` contains results for every participant pair (local + remote)
- No existing test breaks (all Sprint 1 tests still pass)

---

## Constraints

- **No changes to any existing class.** If you find yourself touching `Game`, `RoundRobin`, `GameHistory`, `RoundResult`, `TitForTat`, etc., stop and redesign.
- **TDD.** Write the failing test before writing the implementation for each ticket.
- **No field injection.** Use constructor injection throughout.
- All new production code must have corresponding unit tests.
