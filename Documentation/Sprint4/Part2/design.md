# Replay-SITS — Sprint 4

Diego Sic

## 1. Problem Introduction

Sprint 3 delivered V-SITS, a live viewer that streams every move of a running tournament over Server-Sent Events. That solves the "I want to watch a tournament" problem, but it does nothing for the "I want to study a tournament" problem. Once a move scrolls off the viewer's text area, it is gone. The server keeps a raw `moves.log` file, but that log is line-oriented plaintext with no structure for stepping through, seeking to a round, or rendering in a UI.

For a semester-long project whose entire point is reasoning about strategy, I wanted a way to analyze at a granular level the results:

- A professor demonstrating IPD in class cannot pause on the round where TitForTat first retaliated.
- A student comparing two strategies cannot rewind to the move that flipped the outcome.

Sprint 4 closes this gap by adding **Replay-SITS**, a recording and post-hoc replay subsystem layered on top of the existing stack.

Two requirements define the sprint:

1. **A recorder** attached to every tournament on the server. It captures each move, each game-over, and each tournament-over event as a `ReplayCommand`, and when the tournament finishes it writes the full sequence to a `.replay` file on disk.
2. **A replay screen** added to V-SITS. The user loads a `.replay` file (local or fetched from the server) and gains a scrub bar, step-forward, step-back, play, and pause controls that walk the command log in either direction.

The architectural constraint is the same one that has governed Sprints 2 and 3: extend the system without modifying the Sprint 1 core. `Game`, `RoundRobin`, `GameHistory`, `RoundResult`, and the concrete strategies do not change. The Command pattern is the mechanism that makes this possible — each recorded event is a self-contained object that knows how to both apply and reverse its effect on a private replay state, never touching the live game objects.

## 2. UML Class Diagram

**Figure 1. Server-side recording path.** `ReplayRecorder` implements the existing `GameObserver` interface and lives alongside `ViewerBroadcaster`. As each event fires on the game, the recorder produces a concrete `ReplayCommand` subclass and appends it to an in-memory list. On `onTournamentOver`, it hands the list to `ReplayStore.save()`, which serializes it through `ReplayFile` to a `.replay` file on disk.

**Figure 2. Viewer-side replay path.** `ReplayPlayer` holds a loaded list of `ReplayCommand`s plus a cursor index. Its `stepForward()` applies the next command to a private `ReplayState`; `stepBack()` calls `undo()` on the previous command. `ReplayController` (new FXML controller in V-SITS) drives the player in response to user input and renders the current `ReplayState` into the scrub bar, the round counter, the score panel, and the move log.

## 3. Key New Classes and Behaviors

### ReplayCommand (interface)

`ReplayCommand` is the abstract contract for every replayable event. It has two methods:

```java
void apply(ReplayState state);
void undo(ReplayState state);
```

The contract is: after `apply(s)` followed by `undo(s)`, `s` must equal its pre-`apply` state. This makes step-forward and step-back exact inverses and removes any need for snapshotting. Literally ctrl+z.

### ReplayState

`ReplayState` is the mutable projection that every `ReplayCommand` operates on. It is deliberately not a `GameHistory` — it is a separate data class. Its fields:

- `int currentRound`
- `Map<String, Integer> scores` (by participant name)
- `List<MoveEntry> moveLog` (every move shown so far, each a plain record with round number, both names, both action labels, and both payoffs)
- `List<GameSummary> completedGames`
- `Optional<TournamentSummary> finalResult`

### ReplayRecorder

`ReplayRecorder` is the bridge between the existing game loop and the replay subsystem. It implements `GameObserver` and is constructed inside `NetworkedTournament`, one per tournament. When `start()` is called on the tournament, the recorder is registered on the `Game` instance immediately before the tournament format begins running rounds.

On `onMoveMade(MoveEvent)`, the recorder builds a `MoveCommand` and appends it to an internal `List<ReplayCommand>`. On `onGameOver(GameResult)` it appends a `GameEndCommand`. On `onTournamentOver(TournamentResult)` it appends a `TournamentEndCommand` and then calls `ReplayStore.save(tournamentId, commands)`.

### ReplayStore

`ReplayStore` owns all disk IO for replay files. Its three methods:

- `void save(String tournamentId, List<ReplayCommand> commands)` — serializes and saves.
- `ReplayFile load(Path file)` — reads a replay file and deserializes it.
- `List<ReplayFile.Meta> list()` — lists available replays without loading their full command bodies.

`ReplayStore` lives on the server. The viewer reaches it through two new endpoints: `GET /replays` returns the metadata list; `GET /replays/{id}` returns the full file.

### ReplayPlayer

`ReplayPlayer` is the viewer-side engine that drives replay. The key operations it supports:

- `stepForward()` — applies `commands.get(nextIndex)` to the state, increments the cursor.
- `stepBack()` — decrements the cursor, calls `undo()` on `commands.get(nextIndex)`.
- `seekTo(int commandIndex)` — walks forward or backward one command at a time until the cursor matches.
- `playAsync(long perStepDelayMs, Runnable onEachStep)` — schedules `stepForward` calls at a fixed rate and returns a `CompletableFuture<Void>` that completes when the log is exhausted or the user pauses.

### CommandTypeRegistry

`CommandTypeRegistry` is a small (~20-line) map from type-string to concrete class:

```java
public final class CommandTypeRegistry {
    private static final Map<String, Class<? extends ReplayCommand>> REGISTRY =
        Map.of(
            "MOVE",            MoveCommand.class,
            "GAME_END",        GameEndCommand.class,
            "TOURNAMENT_END",  TournamentEndCommand.class
        );

    public static Class<? extends ReplayCommand> get(String type) {
        Class<? extends ReplayCommand> cls = REGISTRY.get(type);
        if (cls == null) throw new IllegalArgumentException("Unknown command type: " + type);
        return cls;
    }
}
```

Without this, every new `ReplayCommand` subclass requires touching the `@JsonTypeInfo` annotation on `ReplayFile` — a violation of the Open/Closed Principle. With the registry, adding a new event type is one entry in one map. `ReplayFile`'s deserializer delegates to `CommandTypeRegistry.get(typeField)` instead of relying on a static annotation listing. This is the concrete hook that makes the extension points in §9 feasible without touching existing classes.

## 4. Design Patterns

### Command — the ReplayCommand hierarchy

Command is the submitted pattern for Sprint 4. Every tournament event — move, game-over, tournament-over — is encapsulated in a `ReplayCommand` object that knows how to apply itself and reverse itself. I gained inspiration from here: https://refactoring.guru/design-patterns/command

The scrub bar is, conceptually, a sequence of invocations. Forward and backward are symmetric. That symmetry is exactly what Command with `undo()` gives us, and it is why there is no snapshot-and-restore mechanism anywhere in the design.

### Why this is the Command pattern

The Command pattern turns a request into a standalone object that carries everything needed to execute — and reverse — that request. That is exactly what `ReplayCommand` does: each subclass packages the data for one tournament event (`MoveCommand` holds both player names, both actions, both payoffs) and exposes `apply` and `undo` so the invoker (`ReplayPlayer`) never has to know what kind of event it is dealing with. The `.replay` file is nothing more than a serialized command log — the classic Command pattern use case of storing operations for later execution. And the scrub bar is a cursor over that log: stepping forward means invoking the next command, stepping backward means un-invoking the previous one. The pattern fits because the problem is literally a sequence of invertible requests.

## 5. Tricky Relationships and Rationale

### Why ReplayState is a separate projection, not a reused Game

At first glance we could use `Game` on the replay side: reconstruct participants from the log, then drive `Game.play()` with the recorded actions. However, this fails in three places:

1. `Game.play()` is a final Template Method in Sprint 1; it cannot be externally driven move-by-move.
2. Recreating the real participants is fragile. `RemoteParticipant` pointed at a URL that no longer answers. `HumanParticipant` would prompt for input on replay.
3. `Game` has no concept of stepping backward. A replay scrubbed to round 50 from round 100 has no meaningful semantics against a real `Game` instance.

`ReplayState` sidesteps all three. It is a plain data class. Commands mutate it directly. Forward means apply; backward means undo; seek means walk one command at a time until the cursor matches. The real `Game`, `GameHistory`, and `RoundResult` are never instantiated during replay. This keeps the philosophy of extensibility without modifying the original code.

### Why undo() is safe without snapshotting

Every `ReplayCommand` in this design is a pure additive mutation with a computable inverse. `MoveCommand.apply` appends one entry to `moveLog` and adds two payoffs to two score entries. `MoveCommand.undo` pops the last entry and subtracts those same two payoffs. There is no dependency on prior state beyond what the command itself carries.

This is the property that lets us skip the Memento pattern entirely. Commands hold enough data to reverse themselves.

### Why the recorder flushes exactly once at tournament end

The recorder could, in principle, incrementally append each command to the `.replay` file as it is produced, the same way `ViewerBroadcaster` incrementally pushes SSE events to live viewers. It deliberately does not:

- A replay is only useful if the tournament it describes actually finished.
- The commands list is small (on the order of 10–100 KB per tournament). Holding it in memory for the duration is trivial.
- A single write at `onTournamentOver` keeps file-format evolution simple; an incremental format would force a header/trailer scheme and partial-read parsing.

If the server crashes mid-tournament, no `.replay` file is produced. That is the correct behavior for a post-hoc replay artifact.

### Why the recorder is registered in start(), not the constructor

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

Considering the sizes of tournaments and the specs of computers, I consider it okay not to get fancy and have a linear algorithm.

## 7. Sequence Diagram

**Recording flow.** `Game.doRound()` fires `notifyMoveMade` → `ReplayRecorder.onMoveMade` constructs a `MoveCommand` and appends it to the internal list → this repeats for every round of every game → on `notifyTournamentOver`, the recorder appends a `TournamentEndCommand` and calls `ReplayStore.save` → file on disk.

**Replay flow.** User selects a file on the Replay screen → `ReplayController` calls `ReplayStore.load` → the resulting `List<ReplayCommand>` is wrapped in a new `ReplayPlayer` with an empty `ReplayState` → user clicks step-forward → `ReplayController.onStepForward` calls `player.stepForward()` → `command.apply(state)` → controller re-renders from the (now mutated) state → scrub bar position updates via the bound `nextIndex`.

## 8. Alternatives Considered

### 8.1 Live scrubbing during a running tournament

The viewer could pause the live SSE stream, scrub back through moves already received, and resume "live" once the user un-pauses. Rejected because it forces the viewer to maintain two concurrent states — the stream as it arrives and the projected state at the user's cursor — plus a catch-up mechanism when the live stream has advanced past the cursor. Also there would likely be problems managing sync and async states simultaneously.

### 8.2 Memento pattern for fast seeking

Every K commands, snapshot the full `ReplayState` and make `seekTo` jump to the nearest snapshot before walking forward. Rejected because the worst realistic SITS tournament (50 participants, round-robin, 200 rounds) is ~245,000 moves, and applying that many pure-function commands completes in under 20 ms — not perceptible on the scrub bar.

## 9. Extension Points

The design is intentionally open for extension. Every extension listed below is a new class or a one-line registry entry — nothing in the existing recorder, player, or store needs to change.

### 9.1 New event types

The most immediate extension is new `ReplayCommand` subclasses for events the current recorder does not capture:

- `TimeoutCommand` — a participant exceeded its allowed think-time in a future timed-game mode.
- `DisqualificationCommand` — a participant was removed mid-tournament.
- `RoundSkippedCommand` — a future tournament format skips rounds under certain conditions.

Each new type implements `apply` and `undo`, adds one entry to `CommandTypeRegistry`, and is automatically handled by `ReplayPlayer`, `ReplayStore`, and the scrub bar without any other changes. This is the Open/Closed Principle in action: the system is open for extension (new command types) and closed for modification (existing classes untouched).

### 9.2 Annotation commands

A professor or student could attach a text note to any point in the replay:

```java
public final class AnnotationCommand implements ReplayCommand {
    private final String text;

    @Override public void apply(ReplayState s)  { s.annotations.add(text); }
    @Override public void undo(ReplayState s)   { s.annotations.remove(s.annotations.size() - 1); }
}
```

`ReplayState` gains one new `List<String> annotations` field. The viewer renders the annotation overlay whenever one is present at the current cursor position. No other class changes.

### 9.3 Branching / "what-if" replay

Because `ReplayState` is a plain data class and commands are invertible, the viewer can seek to any point, inject a hypothetical `MoveCommand` with a different action, and play forward from there. The `.replay` file on disk is never touched — the branch exists only in memory. This would be powerful for teaching: "what would have happened if TitForTat cooperated here instead?"

Implementing it requires only a `fork()` method on `ReplayPlayer` that deep-copies the current `ReplayState` and the command list up to the cursor. No new infrastructure needed.

### 9.4 Composite commands (game-level scrubbing)

The scrub bar currently steps one `MoveCommand` at a time. A `CompositeCommand` wrapping all moves in a single game lets the scrub bar step game-by-game:

```java
public final class CompositeCommand implements ReplayCommand {
    private final List<ReplayCommand> children;

    @Override public void apply(ReplayState s) { children.forEach(c -> c.apply(s)); }
    @Override public void undo(ReplayState s)  {
        ListIterator<ReplayCommand> it = children.listIterator(children.size());
        while (it.hasPrevious()) it.previous().undo(s);
    }
}
```

`ReplayRecorder` would wrap each game's commands in a `CompositeCommand` on `onGameOver`. The viewer gets a second scrub bar for game-level navigation at zero cost to the player logic.

### 9.5 Export pipeline

Because every `ReplayCommand` is a first-class object, a separate export path requires no changes to the existing classes:

- **CSV export** — walk the command list, write one row per `MoveCommand`.
- **Video export** — render each step to a frame buffer, encode with ffmpeg.
- **Statistics export** — accumulate aggregates (cooperation rate, score variance) by walking `apply` on a fresh `ReplayState`.

All three share the same traversal loop that `ReplayPlayer.seekTo` already uses. The export classes never touch `Game`, `GameHistory`, or any Sprint 1 class.

### 9.6 What to add now vs. later

`CommandTypeRegistry` (§3) is the one addition worth making before submission. It is the mechanical prerequisite for all of the above: without it, every new `ReplayCommand` requires a change to `ReplayFile`'s annotation, which defeats the extensibility argument. Everything else in this section — annotations, branching, composites, export — is speculative; the registry is foundational.
