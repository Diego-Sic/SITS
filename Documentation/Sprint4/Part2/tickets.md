# Replay-SITS — Sprint 4 Tickets

## Ticket 1 — Define ReplayCommand interface and ReplayState data class

**Package:** `sits.replay`

Create the two foundational types for the replay subsystem. No Spring or Jackson dependencies — pure domain objects.

**ReplayCommand** interface:
- `void apply(ReplayState state)`
- `void undo(ReplayState state)`
- Contract: `apply` followed by `undo` must leave state identical to pre-apply

**ReplayState** data class:
- `int currentRound`
- `Map<String, Integer> scores` (by participant name)
- `List<MoveEntry> moveLog`
- `List<GameSummary> completedGames`
- `Optional<TournamentSummary> finalResult`

`MoveEntry` is a plain record: `roundNumber, nameP1, nameP2, actionP1, actionP2, payoffP1, payoffP2`

**Tests:** verify ReplayState starts empty and all fields are accessible.

---

## Ticket 2 — Implement MoveCommand, GameEndCommand, TournamentEndCommand

**Package:** `sits.replay.commands`  
**Depends on:** Ticket 1

Three concrete `ReplayCommand` implementations.

**MoveCommand:**
- Fields: `roundNumber, nameP1, nameP2, actionP1, actionP2, payoffP1, payoffP2`
- `apply`: add `MoveEntry` to `moveLog`, merge both payoffs into `scores`, set `currentRound`
- `undo`: remove last `MoveEntry`, subtract both payoffs, restore `currentRound` from new last entry (or 0 if empty)

**GameEndCommand:**
- Field: `GameSummary` (winner name, per-game scores)
- `apply`: append to `completedGames`
- `undo`: remove last from `completedGames`

**TournamentEndCommand:**
- Field: `TournamentSummary` (final ranking)
- `apply`: set `finalResult`
- `undo`: clear `finalResult`

**Tests:** for each command verify that `apply` then `undo` leaves `ReplayState` identical to its pre-apply value.

---

## Ticket 3 — Implement CommandTypeRegistry

**Package:** `sits.replay`  
**Depends on:** Ticket 2

Small (~20-line) registry mapping type-string → concrete class. This is the Open/Closed hook: adding a new `ReplayCommand` subclass requires only one entry here, not a change to any existing class.

```java
private static final Map<String, Class<? extends ReplayCommand>> REGISTRY = Map.of(
    "MOVE",           MoveCommand.class,
    "GAME_END",       GameEndCommand.class,
    "TOURNAMENT_END", TournamentEndCommand.class
);

public static Class<? extends ReplayCommand> get(String type) {
    Class<? extends ReplayCommand> cls = REGISTRY.get(type);
    if (cls == null) throw new IllegalArgumentException("Unknown command type: " + type);
    return cls;
}
```

`ReplayFile`'s deserializer must delegate to this registry rather than relying on a static `@JsonTypeInfo` listing.

**Tests:** known types return the correct class; unknown type throws `IllegalArgumentException`.

---

## Ticket 4 — Implement ReplayFile and ReplayStore

**Package:** `sits.replay`  
**Depends on:** Tickets 2, 3

Owns all disk I/O for `.replay` files.

**ReplayFile:**
- Serializable container: metadata header + `List<ReplayCommand>`
- Uses Jackson with a custom deserializer that delegates to `CommandTypeRegistry.get(typeField)` instead of a static `@JsonTypeInfo` annotation

**ReplayStore:**
- `void save(String tournamentId, List<ReplayCommand> commands)` — serialize and write to disk
- `ReplayFile load(Path file)` — read and deserialize
- `List<ReplayFile.Meta> list()` — list available replays (metadata only, no full body load)

**Tests:** round-trip a list of all three command types through `save` → `load` and assert equality. Verify `list()` returns metadata without loading command bodies.

---

## Ticket 5 — Implement ReplayRecorder

**Package:** `sits.replay`  
**Depends on:** Tickets 1, 2, 4

Server-side bridge between the existing game loop and the replay subsystem. Implements `GameObserver` — no changes to `Game`, `RoundRobin`, or any Sprint 1 class.

- `onMoveMade(MoveEvent)` → build `MoveCommand`, append to internal `List<ReplayCommand>`
- `onGameOver(GameResult)` → build `GameEndCommand`, append
- `onTournamentOver(TournamentResult)` → build `TournamentEndCommand`, append, then call `ReplayStore.save(tournamentId, commands)`. A failed write logs the error but does not propagate — the tournament has already finished.

**Registration:** `ReplayRecorder` must be registered inside `NetworkedTournament.start()` (same pattern as `ViewerBroadcaster`) — after participants are final, before the first round runs. Add one line: `game.addObserver(recorder)` immediately before `format.run(...)`.

**Tests:** feed synthetic `MoveEvent` / `GameResult` / `TournamentResult` calls and assert the recorder produces the correct command sequence.

---

## Ticket 6 — Add replay server endpoints

**Package:** `sits.server`  
**Depends on:** Ticket 4

Two new endpoints in `TournamentServerController` (or a new `ReplayController`):

- `GET /replays` — returns `List<ReplayFile.Meta>` (metadata list from `ReplayStore.list()`)
- `GET /replays/{id}` — returns the full `ReplayFile` for the given tournament id; 404 if not found

**Tests:** mock `ReplayStore`, verify correct HTTP status codes and response bodies.

---

## Ticket 7 — Implement ReplayPlayer

**Package:** `sits.replay`  
**Depends on:** Tickets 1, 2

Viewer-side engine. Holds a loaded `List<ReplayCommand>` and a cursor `nextIndex` over a private `ReplayState`.

- `stepForward()` — apply `commands.get(nextIndex)`, increment cursor
- `stepBack()` — decrement cursor, call `undo()` on `commands.get(nextIndex)`
- `seekTo(int targetIndex)` — walk forward or backward one command at a time until cursor matches; linear by design (worst case ~245k ops, <20 ms)
- `playAsync(long perStepDelayMs, Runnable onEachStep)` — schedule `stepForward` calls at a fixed rate; returns `CompletableFuture<Void>` that completes when log exhausted or user pauses

**Tests:** load a known command list, verify state after `stepForward`, `stepBack`, and `seekTo`. Verify `seekTo` on already-at-target is a no-op.

---

## Ticket 8 — Implement ReplayController and replay FXML screen

**Package:** `sits.viewer`  
**Depends on:** Tickets 6, 7

New screen in V-SITS for post-hoc replay.

**ReplayController (FXML controller):**
- On load: call `GET /replays` to populate a list of available replays; user picks one
- On selection: call `GET /replays/{id}`, deserialize, wrap in `ReplayPlayer` with fresh `ReplayState`
- Controls: step-forward button, step-back button, play/pause toggle, scrub bar (slider bound to `nextIndex`)
- On each state change: re-render round counter, score panel, and move log from the current `ReplayState`

**replay.fxml:**
- `ListView` or `ComboBox` for replay selection
- `Slider` scrub bar
- Step-back / play-pause / step-forward buttons
- `Label` for round counter
- `TableView` or `TextArea` for move log
- Score panel (participant name → score)

**Navigation:** add a "Replay" button to `LobbyController` that opens the replay screen. No changes to any Sprint 1 class.

**Tests:** unit-test controller state transitions (mock `ReplayPlayer`); verify play/pause toggles the async future correctly.
