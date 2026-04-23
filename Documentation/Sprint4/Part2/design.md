# Replay-SITS — Sprint 4 Part 2

Diego Sic (solo sprint per the Sprint 4 requirements)

## 1. Problem Introduction

Sprint 3 delivered V-SITS, a live viewer that streams every move of a running tournament over Server-Sent Events. That solves the "I want to watch a tournament" problem, but it does nothing for the "I want to study a tournament" problem. Once a move scrolls off the viewer's text area, it is gone. The server keeps a raw `moves.log` file, but that log is line-oriented plaintext with no structure for stepping through, seeking to a round, or rendering in a UI.

For a semester-long project whose entire point is reasoning about strategy, that gap is material:

- A professor demonstrating IPD in class cannot pause on the round where TitForTat first retaliated.
- A student comparing two strategies cannot rewind to the move that flipped the outcome.
- After a tournament ends, there is no artifact anyone can open to reconstruct what happened beyond the final scores.

Sprint 4 Part 2 closes this gap by adding **Replay-SITS**, a recording and post-hoc replay subsystem layered on top of the existing stack.

Two requirements define the sprint:

1. **A recorder** attached to every tournament on the server. It captures each move, each game-over, and each tournament-over event as a `ReplayCommand`, and when the tournament finishes it writes the full sequence to a `.replay` file on disk.
2. **A replay screen** added to V-SITS. The user loads a `.replay` file (local or fetched from the server) and gains a scrub bar, step-forward, step-back, play, and pause controls that walk the command log in either direction.

The architectural constraint is the same one that has governed Sprints 2 and 3: extend the system without modifying the Sprint 1 core. `Game`, `RoundRobin`, `GameHistory`, `RoundResult`, and the concrete strategies do not change. The Command pattern is the mechanism that makes this possible — each recorded event is a self-contained object that knows how to both apply and reverse its effect on a private replay state, never touching the live game objects.

## 2. UML Class Diagram

The class diagram splits into two views for readability. Each diagram lives in its own Mermaid source file in this folder; render with any Mermaid tool (`mermaid-cli`, VS Code Mermaid extension, or paste into [mermaid.live](https://mermaid.live)).

- **Figure 1 — Server-side recording path:** [`figure1-recording.mmd`](./figure1-recording.mmd)
- **Figure 2 — Viewer-side replay path:** [`figure2-replay.mmd`](./figure2-replay.mmd)

Existing classes from Sprints 1–3 are marked `<<existing>>` so the reviewer can tell at a glance what is net-new this sprint.

**Figure 1 description.** `ReplayRecorder` implements the existing `GameObserver` interface and lives alongside `ViewerBroadcaster`. As each event fires on the game, the recorder produces a concrete `ReplayCommand` subclass — `MoveCommand`, `GameEndCommand`, or `TournamentEndCommand` — and appends it to an in-memory list. On `onTournamentOver`, it hands the list to `ReplayStore.save()`, which serializes it through `ReplayFile` to a `.replay` file on disk.

**Figure 2 description.** `ReplayPlayer` holds a loaded list of `ReplayCommand`s plus a cursor index. Its `stepForward()` applies the next command to a private `ReplayState`; `stepBack()` calls `undo()` on the previous command. `ReplayController` (new FXML controller in V-SITS) drives the player in response to user input and renders the current `ReplayState` into the scrub bar, the round counter, the score panel, and the move log. `ServerConnection` (from Sprint 3) gains two new methods so the viewer can enumerate and download replays hosted on the server.

## 3. Key New Classes and Behaviors

### ReplayCommand (interface)

`ReplayCommand` is the abstract contract for every replayable event. It has two methods:

```java
void apply(ReplayState state);
void undo(ReplayState state);
```

The contract is: after `apply(s)` followed by `undo(s)`, `s` must equal its pre-`apply` state. This makes step-forward and step-back exact inverses and removes any need for snapshotting.

Three concrete implementations cover every event in a tournament:

- `MoveCommand` — one participant's move in one round. `apply` appends a `RoundResult`-shaped entry to `ReplayState.moveLog` and updates both scores; `undo` pops the entry and subtracts the payoffs.
- `GameEndCommand` — the end of a single game within a tournament. `apply` marks the current game as completed and records the winner; `undo` reverts that marker.
- `TournamentEndCommand` — the final tournament-level result. `apply` records the final standings; `undo` clears them.

### ReplayState

`ReplayState` is the mutable projection that every `ReplayCommand` operates on. It is deliberately not a `GameHistory` — it is a separate data class. Its fields:

- `int currentRound`
- `Map<String, Integer> scores` (by participant name)
- `List<MoveEntry> moveLog` (every move shown so far, each a plain record with round number, both names, both action labels, and both payoffs)
- `List<GameSummary> completedGames`
- `Optional<TournamentSummary> finalResult`

No `Action` interface references live in `ReplayState`; moves are stored by their label strings, reusing the same transport-safe design that `GameHistoryDTO` introduced in Sprint 2.

### ReplayRecorder

`ReplayRecorder` is the bridge between the existing game loop and the replay subsystem. It implements `GameObserver` and is constructed inside `NetworkedTournament`, one per tournament, exactly parallel to `ViewerBroadcaster`. When `start()` is called on the tournament, the recorder is registered on the `Game` instance immediately before the tournament format begins running rounds.

On `onMoveMade(MoveEvent)`, the recorder builds a `MoveCommand` and appends it to an internal `List<ReplayCommand>`. On `onGameOver(GameResult)` it appends a `GameEndCommand`. On `onTournamentOver(TournamentResult)` it appends a `TournamentEndCommand` and then calls `ReplayStore.save(tournamentId, commands)`.

The recorder holds the commands in memory for the lifetime of the tournament and flushes exactly once. This is intentional — see §5 for why incremental writes are not used.

### ReplayStore

`ReplayStore` owns all disk IO for replay files. Its three methods:

- `void save(String tournamentId, List<ReplayCommand> commands)` — serializes through `ReplayFile` and writes `replays/{tournamentId}-{timestamp}.replay`.
- `ReplayFile load(Path file)` — reads a replay file and deserializes it.
- `List<ReplayFile.Meta> list()` — lists available replays without loading their full command bodies.

`ReplayStore` lives on the server. The viewer reaches it through two new endpoints: `GET /replays` returns the metadata list; `GET /replays/{id}` returns the full file.

### ReplayFile

`ReplayFile` is the JSON-serializable envelope for a saved replay. Its fields: `tournamentId`, `tournamentName`, `createdAt`, `participants`, and `commands`. The `commands` list is polymorphic — each entry has a `type` discriminator field (`MOVE`, `GAME_END`, `TOURNAMENT_END`) and the remaining fields specific to that type. Jackson's `@JsonTypeInfo(use = NAME, property = "type")` handles the polymorphic round-trip.

This mirrors the design choice `MoveEventDTO` made in Sprint 3: one envelope class covers all event types, no game-specific action type is imported, and actions travel by their `getLabel()` string.

### ReplayPlayer

`ReplayPlayer` is the viewer-side engine that drives replay. It holds:

- the immutable `List<ReplayCommand>` loaded from a file,
- a mutable `ReplayState`,
- a cursor `int nextIndex` (the index of the next command that would be applied by `stepForward`).

Its operations:

- `stepForward()` — applies `commands.get(nextIndex)` to the state, increments the cursor.
- `stepBack()` — decrements the cursor, calls `undo()` on `commands.get(nextIndex)`.
- `seekTo(int commandIndex)` — walks forward or backward one command at a time until the cursor matches. Linear cost; see §5 on why this is fine.
- `playAsync(long perStepDelayMs, Runnable onEachStep)` — schedules `stepForward` calls at a fixed rate and returns a `CompletableFuture<Void>` that completes when the log is exhausted or the user pauses.

`ReplayPlayer` is pure logic; it has no JavaFX dependency. The controller is responsible for any UI thread marshalling.

### ReplayController (viewer)

`ReplayController` is the new V-SITS FXML controller. It owns:

- a file picker (or dropdown populated from `GET /replays`),
- a scrub bar bound to `ReplayPlayer.nextIndex`,
- step-forward, step-back, play, and pause buttons,
- a speed slider (100 ms to 2000 ms per step),
- the same move-log text area V-SITS already uses for live games (reused for visual continuity),
- a score panel that re-renders from `ReplayState` on every step.

Navigation: the Lobby screen gets a new "Browse Replays…" button next to its existing Refresh/Watch buttons. Clicking it replaces the lobby scene with `replay.fxml`.

## 4. Design Patterns

### Command — the ReplayCommand hierarchy

Command is the submitted pattern for Sprint 4 Part 2. Every tournament event — move, game-over, tournament-over — is encapsulated in a `ReplayCommand` object that knows how to apply itself and reverse itself.

This satisfies the sprint's "future-proof" requirement cleanly. Adding a new recordable event type (say, a "timeout" event, or a "participant disqualified" event) is a one-class addition: create a new `ReplayCommand` subclass, implement `apply` and `undo`, and the recorder, player, scrub bar, and `.replay` format all pick it up without modification. Nothing in the viewer or player code switches on event type at runtime; both operate on the interface.

It also matches the problem's structure. The scrub bar is, conceptually, a sequence of invocations. Forward and backward are symmetric. That symmetry is exactly what Command with `undo()` gives you, and it is why there is no snapshot-and-restore mechanism anywhere in the design.

## 5. Tricky Relationships and Rationale

### Why ReplayState is a separate projection, not a reused Game

The naive design would reuse `Game` on the replay side: reconstruct participants from the log, then drive `Game.play()` with the recorded actions. This fails in three places:

1. `Game.play()` is a final Template Method in Sprint 1; it cannot be externally driven move-by-move.
2. Recreating the real participants is fragile. `RemoteParticipant` pointed at a URL that no longer answers. `HumanParticipant` would prompt for input on replay. `AlwaysDefect` is fine, but relying on that is not a design.
3. `Game` has no concept of stepping backward. A replay scrubbed to round 50 from round 100 has no meaningful semantics against a real `Game` instance.

`ReplayState` sidesteps all three. It is a plain data class. Commands mutate it directly. Forward means apply; backward means undo; seek means walk one command at a time until the cursor matches. The real `Game`, `GameHistory`, and `RoundResult` are never instantiated during replay.

### Why undo() is safe without snapshotting

Every `ReplayCommand` in this design is a pure additive mutation with a computable inverse. `MoveCommand.apply` appends one entry to `moveLog` and adds two payoffs to two score entries. `MoveCommand.undo` pops the last entry and subtracts those same two payoffs. There is no dependency on prior state beyond what the command itself carries.

This is the property that lets us skip the Memento pattern entirely. Commands hold enough data to reverse themselves; `ReplayState` never needs to be saved because it can always be rebuilt from a prefix of the command list. See §8.2 for the Memento alternative and why it was rejected.

### Why the recorder flushes exactly once at tournament end

The recorder could, in principle, incrementally append each command to the `.replay` file as it is produced, the same way `ViewerBroadcaster` incrementally pushes SSE events to live viewers. It deliberately does not:

- A replay is only useful if the tournament it describes actually finished. Writing partial files would require a recovery step on load ("this replay ends mid-tournament — open it anyway?") that adds UX and test surface.
- The commands list is small (on the order of 10–100 KB per tournament). Holding it in memory for the duration is trivial.
- A single write at `onTournamentOver` keeps file-format evolution simple; an incremental format would force a header/trailer scheme and partial-read parsing.

If the server crashes mid-tournament, no `.replay` file is produced. That is the correct behavior for a post-hoc replay artifact.

### Why the recorder is registered in `start()`, not the constructor

This is the same constraint that applied to `ViewerBroadcaster` in Sprint 3. A `GameObserver` registered before any game exists receives no events and then `onTournamentOver` would fire on a broadcaster whose collaborators are not yet wired. Registering inside `NetworkedTournament.start()` guarantees the recorder is attached after participants are final and before the first round runs.

## 6. Tricky Code

### MoveCommand.apply and MoveCommand.undo

```java
public final class MoveCommand implements ReplayCommand {
    private final int roundNumber;
    private final String nameP1, nameP2;
    private final String actionP1, actionP2;
    private final int payoffP1, payoffP2;

    @Override
    public void apply(ReplayState s) {
        s.moveLog.add(new MoveEntry(roundNumber, nameP1, nameP2,
                                    actionP1, actionP2, payoffP1, payoffP2));
        s.scores.merge(nameP1, payoffP1, Integer::sum);
        s.scores.merge(nameP2, payoffP2, Integer::sum);
        s.currentRound = roundNumber;
    }

    @Override
    public void undo(ReplayState s) {
        s.moveLog.remove(s.moveLog.size() - 1);
        s.scores.merge(nameP1, -payoffP1, Integer::sum);
        s.scores.merge(nameP2, -payoffP2, Integer::sum);
        s.currentRound = s.moveLog.isEmpty() ? 0
                        : s.moveLog.get(s.moveLog.size() - 1).roundNumber();
    }
}
```

The invariant `state_before ≡ undo(apply(state_before))` is what the unit tests assert on every command type. Any command that cannot uphold it belongs outside the `ReplayCommand` hierarchy.

### ReplayRecorder.onTournamentOver — the flush

```java
@Override
public void onTournamentOver(TournamentResult result) {
    commands.add(new TournamentEndCommand(result));
    try {
        store.save(tournamentId, Collections.unmodifiableList(commands));
    } catch (IOException e) {
        log.error("Failed to write replay for {}", tournamentId, e);
    }
}
```

A failed write does not propagate. The tournament has already finished; losing the replay artifact is recoverable (the raw `moves.log` still exists), but failing the tournament would be user-visible.

### ReplayPlayer.seekTo — linear by design

```java
public void seekTo(int targetIndex) {
    if (targetIndex < 0 || targetIndex > commands.size()) {
        throw new IndexOutOfBoundsException();
    }
    while (nextIndex < targetIndex) stepForward();
    while (nextIndex > targetIndex) stepBack();
}
```

This is O(|Δ|) in command applications. For SITS workloads (hundreds of thousands of moves at the very top end) it completes in single-digit milliseconds on the JavaFX Application Thread. No snapshots needed. See §8.2 for the Memento alternative.

## 7. Sequence Diagram

Two flows are traced, each in its own Mermaid source file in this folder.

- **Figure 3 — Recording flow:** [`figure3-sequence-recording.mmd`](./figure3-sequence-recording.mmd)
- **Figure 4 — Replay flow:** [`figure4-sequence-replay.mmd`](./figure4-sequence-replay.mmd)

**Figure 3 description — Recording flow.** Inside `NetworkedTournament.start()`, a fresh `ReplayRecorder` is constructed and registered on the `Game` as a `GameObserver`. For every round of every game, `Game.doRound()` calls `notifyMoveMade`, the recorder builds a `MoveCommand` and appends it to its internal list. At the end of each game, `notifyGameOver` produces a `GameEndCommand`. At the end of the tournament, `notifyTournamentOver` produces a `TournamentEndCommand` and the recorder hands the full command list to `ReplayStore.save()`, which writes a timestamped `.replay` file.

**Figure 4 description — Replay flow.** The user clicks "Browse Replays…" on the lobby screen; `ReplayController` asks `ReplayStore.list()` for the catalog. On file selection, `ReplayStore.load()` returns a `ReplayFile`, and the controller wraps its command list in a new `ReplayPlayer` initialized with an empty `ReplayState`. Three user interactions are traced: **step-forward** calls `stepForward()` → `command.apply(state)` → re-render; **step-back** calls `stepBack()` → `command.undo(state)` → re-render; **scrub** calls `seekTo(targetIndex)`, which walks the command list one step at a time (apply or undo depending on direction) until the cursor matches.

The key observation spans both diagrams: the recording flow invokes zero new methods on `Game`, `GameHistory`, or `RoundResult`, and the replay flow invokes zero methods on them either. The Command pattern is the entire abstraction barrier — the real game objects exist only during recording, and the `ReplayState` projection exists only during playback.

## 8. Alternatives Considered

### 8.1 Live scrubbing during a running tournament

The viewer could pause the live SSE stream, scrub back through moves already received, and resume "live" once the user un-pauses. Rejected because it forces the viewer to maintain two concurrent states — the stream as it arrives and the projected state at the user's cursor — plus a catch-up mechanism when the live stream has advanced past the cursor. That roughly doubles the controller's code surface without adding pattern coverage; post-hoc replay already exercises Command end-to-end, and the 5-minute presentation has room for one story, not two.

### 8.2 Memento pattern for fast seeking

Every K commands, snapshot the full `ReplayState` and make `seekTo` jump to the nearest snapshot before walking forward. Rejected because the worst realistic SITS tournament (50 participants, round-robin, 200 rounds) is ~245,000 moves, and applying that many pure-function commands completes in under 20 ms — not perceptible on the scrub bar. Adding Memento would also split the submission between two patterns, whereas V-SITS and Remote-SITS each led on a single dominant pattern; this design keeps that convention.
