# Sprint 3 — Implementation Tickets

V-SITS · Diego, Mitch, Dewey, Robert

Tickets are ordered by dependency — complete them top to bottom and you will never hit a missing class.

---

## S3-01 · `MoveEventDTO`

**File to create:** `src/main/java/sits/networking/dto/MoveEventDTO.java`

Create a plain Java class (no Spring annotations needed). All fields are `public` so Jackson can serialize/deserialize them without getters.

**Fields:**
```java
public String type;          // "MOVE" | "GAME_OVER" | "TOURNAMENT_OVER"
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
```

**Three static factory methods — the only way to build instances:**
```java
public static MoveEventDTO fromMoveEvent(MoveEvent event)         // type = "MOVE"
public static MoveEventDTO gameOver(GameResult result)             // type = "GAME_OVER"
public static MoveEventDTO tournamentOver(TournamentResult result) // type = "TOURNAMENT_OVER"
```

**Rules:**
- Do NOT import `PrisonerAction` or any game-specific type. Get action labels via `action.getLabel()`.
- Private no-arg constructor is fine; only the static factories should be used.

**Depends on:** nothing new — only existing `sits.core` classes.

---

## S3-02 · `TournamentRegistry.listActive()`

**File to modify:** `src/main/java/sits/server/TournamentRegistry.java`

Add one method alongside the existing `listRegistering()`:

```java
public List<NetworkedTournament> listActive() {
    return tournaments.values().stream()
        .filter(t -> t.getStatus() == TournamentStatus.REGISTERING
                  || t.getStatus() == TournamentStatus.RUNNING)
        .toList();
}
```

**Depends on:** nothing new.

---

## S3-03 · `ViewerBroadcaster`

**File to create:** `src/main/java/sits/server/ViewerBroadcaster.java`

Implements `GameObserver`. One instance lives inside each `NetworkedTournament`.

**Fields:**
```java
private final long delayMs;
private final List<SseEmitter> emitters;  // use CopyOnWriteArrayList
private final ObjectMapper mapper;
```

**Constructor:**
```java
public ViewerBroadcaster(long delayMs) {
    this.delayMs = delayMs;
    this.emitters = new CopyOnWriteArrayList<>();
    this.mapper   = new ObjectMapper();
}
```

**`addEmitter(SseEmitter emitter)`** — call this when a viewer connects:
```java
emitter.onCompletion(() -> emitters.remove(emitter));
emitter.onTimeout(()    -> emitters.remove(emitter));
emitters.add(emitter);
```

**`onMoveMade(MoveEvent event)`** — send + pace:
```java
// 1. serialize to MoveEventDTO JSON
// 2. loop emitters, call emitter.send(SseEmitter.event().data(json))
// 3. collect failed emitters in a dead list, remove after loop
// 4. Thread.sleep(delayMs) after all sends
```

**`onGameOver(GameResult result)`** — serialize a GAME_OVER dto and push to all emitters.

**`onTournamentOver(TournamentResult result)`** — serialize a TOURNAMENT_OVER dto, push, then call `emitter.complete()` on every emitter and clear the list.

**Depends on:** S3-01 (`MoveEventDTO`).

---

## S3-04 · Update `NetworkedTournament`

**File to modify:** `src/main/java/sits/server/NetworkedTournament.java`

**1. Add fields:**
```java
private final long             delayMs;
private final ViewerBroadcaster broadcaster;
```

**2. Update the constructor** to accept `long delayMs` as the last parameter, create the broadcaster:
```java
this.delayMs     = delayMs;
this.broadcaster = new ViewerBroadcaster(delayMs);
```

**3. Add a convenience overload** that passes `delayMs = 0` so existing tests don't break:
```java
public NetworkedTournament(String id, String name, TournamentFormat format,
                            Game game, List<Participant> initialParticipants,
                            Function<String, Action> actionFactory) {
    this(id, name, format, game, initialParticipants, actionFactory, 0L);
}
```

**4. In `start()`**, register the broadcaster just before running:
```java
game.addObserver(broadcaster);   // NEW — add this line
TournamentResult result = format.run(participants, game);
```

**5. Add getter:**
```java
public ViewerBroadcaster getBroadcaster() { return broadcaster; }
```

**Depends on:** S3-03 (`ViewerBroadcaster`).

---

## S3-05 · Update `TournamentServerController`

**File to modify:** `src/main/java/sits/server/TournamentServerController.java`

**Three changes:**

**a) Inject `ExecutorService`** — add it as a constructor parameter (Spring will inject the bean you add in S3-06):
```java
private final ExecutorService executor;
```

**b) `GET /tournaments` — return REGISTERING + RUNNING with status field:**
```java
// Update TournamentSummary record to include status:
record TournamentSummary(String id, String name, TournamentStatus status) {}

// Change getTournaments() to use listActive():
return registry.listActive().stream()
    .map(t -> new TournamentSummary(t.getId(), t.getName(), t.getStatus()))
    .toList();
```

**c) `POST /{id}/start` — make async, return 202:**
```java
@PostMapping("/{id}/start")
public ResponseEntity<Void> start(@PathVariable String id) {
    NetworkedTournament t = registry.get(id);
    if (t == null) return ResponseEntity.notFound().build();
    executor.submit(t::start);
    return ResponseEntity.accepted().build();
}
```

**d) New `GET /{id}/stream` — SSE endpoint:**
```java
@GetMapping(value = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter streamMoves(@PathVariable String id) {
    NetworkedTournament t = registry.get(id);
    if (t == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    SseEmitter emitter = new SseEmitter(0L);   // 0L = no timeout
    t.getBroadcaster().addEmitter(emitter);
    return emitter;
}
```

**Depends on:** S3-02, S3-03, S3-04.

---

## S3-06 · Update `TournamentServerApp`

**File to modify:** `src/main/java/sits/server/TournamentServerApp.java`

**1. Add `ExecutorService` bean:**
```java
@Bean
public ExecutorService executorService() {
    return Executors.newCachedThreadPool();
}
```

**2. Update the seed tournament** to pass `delayMs = 1000` so a viewer can actually see moves:
```java
registry.add(new NetworkedTournament(
    "ipd-01", "IPD Tournament",
    new RoundRobin(),
    new IteratedPrisonersDilemma(10),
    List.of(new AlwaysCooperate(), new AlwaysDefect()),
    PrisonerAction::valueOf,
    1000L   // 1-second delay between moves
));
```

**Depends on:** S3-04, S3-05.

---

## S3-07 · `TournamentInfo`

**File to create:** `src/main/java/sits/viewer/TournamentInfo.java`

Plain data class — no annotations needed:

```java
package sits.viewer;

public class TournamentInfo {
    public String id;
    public String name;
    public String status;   // "REGISTERING" or "RUNNING"
}
```

**Depends on:** nothing.

---

## S3-08 · `ServerConnection`

**File to create:** `src/main/java/sits/viewer/ServerConnection.java`

Wraps `java.net.http.HttpClient`. No Spring — plain Java.

**Constructor:**
```java
public ServerConnection(String baseUrl) {
    this.baseUrl = baseUrl;
    this.client  = HttpClient.newHttpClient();
    this.mapper  = new ObjectMapper();
}
```

**`fetchTournaments()`** — GET /tournaments:
```java
public List<TournamentInfo> fetchTournaments() throws IOException, InterruptedException {
    HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + "/tournaments"))
        .GET().build();
    HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
    return mapper.readValue(res.body(),
        mapper.getTypeFactory().constructCollectionType(List.class, TournamentInfo.class));
}
```

**`streamMoves(tournamentId, onEvent, onDone)`** — SSE subscription:
```java
public CompletableFuture<Void> streamMoves(String tournamentId,
                                            Consumer<MoveEventDTO> onEvent,
                                            Runnable onDone) {
    HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + "/tournaments/" + tournamentId + "/stream"))
        .GET().build();

    return client.sendAsync(req, HttpResponse.BodyHandlers.ofLines())
        .thenAccept(response -> {
            response.body()
                .filter(line -> line.startsWith("data:"))
                .map(line -> line.substring(5).trim())
                .forEach(json -> {
                    try {
                        MoveEventDTO dto = mapper.readValue(json, MoveEventDTO.class);
                        Platform.runLater(() -> onEvent.accept(dto));
                    } catch (JsonProcessingException ignored) {}
                });
            Platform.runLater(onDone);
        });
}
```

**Depends on:** S3-01 (`MoveEventDTO`), S3-07 (`TournamentInfo`).

---

## S3-09 · `ViewerApp`

**File to create:** `src/main/java/sits/viewer/ViewerApp.java`

Standalone JavaFX entry point — no Spring Boot.

```java
package sits.viewer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ViewerApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/connect.fxml"));
        stage.setScene(new Scene(loader.load()));
        stage.setTitle("V-SITS Viewer");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

**Depends on:** S3-10 (needs `connect.fxml` to exist before running).

---

## S3-10 · Connect screen

**Files to create:**
- `src/main/java/sits/viewer/ConnectController.java`
- `src/main/resources/fxml/connect.fxml`

**`connect.fxml` layout — two fields + one button:**
- `TextField` with `fx:id="ipField"` (placeholder: `192.168.1.1`)
- `TextField` with `fx:id="portField"` (placeholder: `8080`)
- `Button` with text `Connect`, `onAction="#connect"`

**`ConnectController`:**
```java
@FXML private TextField ipField;
@FXML private TextField portField;

@FXML
public void connect() {
    String baseUrl = "http://" + ipField.getText() + ":" + portField.getText();
    ServerConnection conn = new ServerConnection(baseUrl);

    // load lobby.fxml and pass conn to LobbyController
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/lobby.fxml"));
    Parent root = loader.load();
    LobbyController lobby = loader.getController();
    lobby.init(conn);

    Stage stage = (Stage) ipField.getScene().getWindow();
    stage.setScene(new Scene(root));
}
```

**Depends on:** S3-08 (`ServerConnection`), S3-11 (`LobbyController` must exist).

---

## S3-11 · Lobby screen

**Files to create:**
- `src/main/java/sits/viewer/LobbyController.java`
- `src/main/resources/fxml/lobby.fxml`

**`lobby.fxml` layout:**
- `ListView` with `fx:id="tournamentList"`
- `Button` Refresh, `onAction="#refresh"`
- `Button` Watch, `onAction="#watchSelected"` — disabled by default

**`LobbyController`:**
```java
@FXML private ListView<TournamentInfo> tournamentList;
@FXML private Button watchButton;
private ServerConnection connection;

public void init(ServerConnection connection) {
    this.connection = connection;
    // disable Watch until a RUNNING tournament is selected
    watchButton.setDisable(true);
    tournamentList.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) ->
        watchButton.setDisable(sel == null || !"RUNNING".equals(sel.status))
    );
    refresh();
}

@FXML
public void refresh() {
    // call connection.fetchTournaments() and populate the list
    // wrap in try/catch, show an alert on error
}

@FXML
public void watchSelected() {
    TournamentInfo selected = tournamentList.getSelectionModel().getSelectedItem();
    // load live_game.fxml, pass connection + selected.id to LiveGameController
}
```

**Depends on:** S3-07 (`TournamentInfo`), S3-08 (`ServerConnection`), S3-12.

---

## S3-12 · Live Game screen

**Files to create:**
- `src/main/java/sits/viewer/LiveGameController.java`
- `src/main/resources/fxml/live_game.fxml`

**`live_game.fxml` layout:**
- `TextArea` with `fx:id="feedArea"` (editable=false, wrapText=true)
- `Button` Back, `onAction="#back"`

**`LiveGameController`:**
```java
@FXML private TextArea feedArea;
private ServerConnection connection;
private String tournamentId;
private CompletableFuture<Void> stream;

public void init(ServerConnection connection, String tournamentId) {
    this.connection    = connection;
    this.tournamentId  = tournamentId;
}

@FXML
public void initialize() {
    // called by FXMLLoader — start streaming once init() provides the fields
}

public void startStream() {
    stream = connection.streamMoves(tournamentId,
        dto -> {
            switch (dto.type) {
                case "MOVE"            -> feedArea.appendText(
                    "Round " + dto.roundNumber + ": " + dto.p1Name +
                    " played " + dto.action1 + " | " + dto.p2Name +
                    " played " + dto.action2 + "\n");
                case "GAME_OVER"       -> feedArea.appendText(
                    "--- Game over. Winner: " + dto.winner + " ---\n");
                case "TOURNAMENT_OVER" -> feedArea.appendText(
                    "=== Tournament complete ===\n");
            }
        },
        () -> feedArea.appendText("Stream closed.\n")
    );
}

@FXML
public void back() {
    if (stream != null) stream.cancel(true);
    // navigate back to lobby.fxml
}
```

**Note:** call `startStream()` from `LobbyController` after calling `init()`, before showing the scene.

**Depends on:** S3-08 (`ServerConnection`), S3-01 (`MoveEventDTO`).

---

## Dependency Order Summary

```
S3-01  MoveEventDTO
S3-02  TournamentRegistry.listActive()
S3-03  ViewerBroadcaster          ← needs S3-01
S3-04  NetworkedTournament update ← needs S3-03
S3-05  TournamentServerController ← needs S3-02, S3-03, S3-04
S3-06  TournamentServerApp update ← needs S3-04, S3-05
S3-07  TournamentInfo
S3-08  ServerConnection           ← needs S3-01, S3-07
S3-09  ViewerApp
S3-10  ConnectController          ← needs S3-08, S3-11
S3-11  LobbyController            ← needs S3-07, S3-08, S3-12
S3-12  LiveGameController         ← needs S3-01, S3-08
```
