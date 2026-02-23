# SITS Implementation Tickets
**Strategic Interaction Tournament Simulator — VibeCoders**

---

## Epic 0: Project Infrastructure

### TICKET-000 — Project Setup & Package Structure
**Priority:** Critical (must be done before all other tickets)
**Pattern:** N/A

Establish a clean Maven project structure tailored to SITS. The current `pom.xml` inherits from Spring Boot and includes JavaFX/Spring Web — none of which are needed for the simulator core. Strip it down to a plain Java 17 project with JUnit 5 and JaCoCo.

---

#### 0.1 — Clean up `pom.xml`

**Remove:**
- `spring-boot-starter-parent` as parent
- `spring-boot-starter-web`
- `spring-boot-devtools`
- `spring-boot-starter-test`
- `spring-boot-maven-plugin`
- `javafx-controls`, `javafx-fxml`, `javafx-maven-plugin`
- `testfx-core`, `testfx-junit5`
- `maven-compiler-plugin` listed as a `<dependency>` (it belongs only in `<plugins>`)

**Keep / update:**
- `groupId`: change to `sits`
- `artifactId`: change to `sits-simulator`
- `name` / `description`: update to reflect SITS
- Java 17 compiler source/target
- `junit-jupiter-api` + `junit-jupiter-engine` (for `mvn test`)
- `assertj-core` (useful for test assertions)
- `jacoco-maven-plugin` (code coverage — already configured correctly)
- `maven-surefire-plugin` configured to run JUnit 5 tests

**Final `pom.xml` dependency list:**
```xml
<!-- Test scope -->
<dependency>junit-jupiter-api</dependency>
<dependency>junit-jupiter-engine</dependency>
<dependency>assertj-core</dependency>

<!-- Build plugins -->
<plugin>maven-compiler-plugin (Java 17)</plugin>
<plugin>maven-surefire-plugin (JUnit Platform)</plugin>
<plugin>jacoco-maven-plugin</plugin>
```

**Acceptance Criteria:**
- [ ] `mvn clean test` runs with zero compilation errors on an empty `src/`
- [ ] `mvn clean verify` generates a JaCoCo report under `target/site/jacoco/`
- [ ] No Spring Boot or JavaFX classes on the classpath
- [ ] `mvn dependency:tree` shows only test + build-tool dependencies

---

#### 0.2 — Define Package Structure

The following directories have been created with `.hold` files to preserve them in git:

```
src/
├── main/java/sits/
│   ├── core/          → Action, Participant, Game, GameObserver,
│   │                     GameHistory, RoundResult, GameResult,
│   │                     TournamentFormat, TournamentResult, MoveEvent
│   ├── games/
│   │   └── ipd/       → IteratedPrisonersDilemma, PrisonerAction
│   ├── tournament/    → RoundRobin
│   └── logging/       → MoveLogger, ScoreLogger
└── test/java/sits/
    ├── core/          → unit tests for data models
    ├── games/
    │   └── ipd/       → IPD-specific tests
    ├── tournament/    → RoundRobin tests
    └── integration/   → end-to-end test (TICKET-018)
```

**Status:** Done — directories created.

**Acceptance Criteria:**
- [x] All packages exist in both `src/main/java/` and `src/test/java/`
- [x] Structure matches the layout above
- [x] Each directory has a `.hold` file for git tracking

---

#### 0.3 — Update `.gitignore`

**Status:** Done — `.gitignore` updated.

```
# Build output
target/

# Compiled classes
*.class

# IDE files
*.iml
.classpath
.project
.settings/

# SITS log output (MoveLogger, ScoreLogger)
logs/
*.log
```

**Acceptance Criteria:**
- [x] `target/` is ignored
- [x] `.class` files are ignored
- [x] Log files produced by `MoveLogger` / `ScoreLogger` are not tracked by git

---

#### 0.4 — Remove leftover IDE artifacts

The repo currently contains `restAPI.iml` and Eclipse `.classpath`/`.project`/`.settings/` from a prior project.

**Actions:**
- Delete `restAPI.iml`
- Delete `.classpath`, `.project`, and `.settings/`
- Re-import the project in IntelliJ from the updated `pom.xml`

**Acceptance Criteria:**
- [ ] No IDE config files referencing `restAPI` remain in the repo
- [ ] IntelliJ can open the project cleanly from `pom.xml`

---

**Definition of Done for TICKET-000:**
- `mvn clean test` exits with `BUILD SUCCESS`
- `mvn clean verify` produces a JaCoCo HTML report
- Package skeleton is in place matching Section 0.2
- `.gitignore` covers build output and log files
- No leftover Spring Boot / JavaFX / restAPI artifacts

---

## Epic 1: Core Interfaces & Data Models

### TICKET-001 — Create `Action` Interface
**Priority:** Critical (blocks everything)
**Pattern:** N/A
**Status:** Done — `src/main/java/sits/core/Action.java`

Define the base interface for all game moves.

```java
public interface Action {
    String getLabel();
}
```

**Acceptance Criteria:**
- [x] Interface lives in its own file (`sits/core/Action.java`)
- [x] Single method `getLabel()` returns a `String`
- [x] No game-specific logic inside the interface
- [x] Tests in `sits/core/ActionTest.java`

---

### TICKET-002 — Create `RoundResult` Class
**Priority:** Critical
**Depends on:** TICKET-001
**Status:** Done — `src/main/java/sits/core/RoundResult.java`

Stores the outcome of a single game round between two participants.

**Fields:**
- `actionP1 : Action`
- `actionP2 : Action`
- `payoffP1 : int`
- `payoffP2 : int`

**Acceptance Criteria:**
- [x] Holds `Action` (interface type, not concrete enum) for both players
- [x] Holds payoff scores for both players
- [x] Getters for all fields
- [x] No game-specific types imported
- [x] Tests in `sits/core/RoundResultTest.java`

---

### TICKET-003 — Create `GameHistory` Class
**Priority:** Critical
**Depends on:** TICKET-002
**Status:** Done — `src/main/java/sits/core/GameHistory.java`

Accumulates all `RoundResult` objects for a single game instance.

**Fields:**
- `nameP1 : String`
- `nameP2 : String`
- `rounds : List<RoundResult>`

**Methods:**
- `getRounds()` — returns the full list
- `getLastRound()` — returns the last `RoundResult` (used by TitForTat)

**Design note:** `getLastRound()` returns `null` when the list is empty. Callers must
check `getRounds().isEmpty()` first — consistent with how `TitForTat` uses it.

**Acceptance Criteria:**
- [x] `getLastRound()` returns `null` when list is empty (documented above)
- [x] `getRounds()` returns a mutable list (rounds are added during play)
- [x] Constructor accepts both player names
- [x] Tests in `sits/core/GameHistoryTest.java`

---

### TICKET-004 — Create `GameResult` Class
**Priority:** High
**Depends on:** TICKET-003
**Status:** Done — `src/main/java/sits/core/GameResult.java`

Holds the final outcome of a completed game.

**Fields:**
- `history : GameHistory`
- `winner : String` (player name, or `"DRAW"`)
- `totalScoreP1 : int`
- `totalScoreP2 : int`

**Acceptance Criteria:**
- [x] Constructed from a completed `GameHistory`
- [x] Winner determined by comparing total scores
- [x] Supports draw case
- [x] Tests in `sits/core/GameResultTest.java`

---

### TICKET-005 — Create `TournamentResult` Class
**Priority:** High
**Depends on:** TICKET-004
**Status:** Done — `src/main/java/sits/core/TournamentResult.java`

Aggregates all `GameResult` objects from a completed tournament.

**Fields:**
- `results : List<GameResult>`

**Methods:**
- `getResults()` — returns an unmodifiable view of all game results
- `getSummary()` — returns total accumulated score per player across all games

**Acceptance Criteria:**
- [x] Stores all game results from a tournament run
- [x] Can be returned by `TournamentFormat.run()`
- [x] Tests in `sits/core/TournamentResultTest.java`

---

## Epic 2: Observer Pattern

### TICKET-006 — Create `MoveEvent` Class
**Priority:** High
**Depends on:** TICKET-002, TICKET-003
**Status:** Done — `src/main/java/sits/core/MoveEvent.java`

Event object fired after every round.

**Fields:**
- `round : RoundResult`
- `history : GameHistory` (full history at the moment the event fires)

**Acceptance Criteria:**
- [x] Immutable once constructed
- [x] Carries a complete, up-to-date `GameHistory` (not just the latest round)
- [x] Tests in `sits/core/MoveEventTest.java`

---

### TICKET-007 — Create `GameObserver` Interface
**Priority:** High
**Depends on:** TICKET-004, TICKET-006

Observer interface for reacting to game events.

```java
public interface GameObserver {
    void onMoveMade(MoveEvent event);
    void onGameOver(GameResult result);
}
```

**Acceptance Criteria:**
- [ ] Two methods: `onMoveMade` and `onGameOver`
- [ ] No coupling to any specific game type

---

### TICKET-008 — Implement `MoveLogger`
**Priority:** Medium
**Depends on:** TICKET-007

Concrete observer that writes each round's move to a file.

**Behavior:**
- `onMoveMade(event)` — writes player names, actions, and round payoffs to a log file
- `onGameOver(result)` — no-op (or writes separator)

**Acceptance Criteria:**
- [ ] Uses `action.getLabel()` — no imports of concrete action enums
- [ ] Writes to a configurable file path
- [ ] Appends to file (supports multiple games per session)

---

### TICKET-009 — Implement `ScoreLogger`
**Priority:** Medium
**Depends on:** TICKET-007

Concrete observer that writes final game scores to a file.

**Behavior:**
- `onMoveMade(event)` — no-op
- `onGameOver(result)` — writes winner and total scores

**Acceptance Criteria:**
- [ ] Only writes when game ends
- [ ] Output is human-readable
- [ ] Writes to a configurable file path

---

## Epic 3: Abstract Game (Template Method)

### TICKET-010 — Create Abstract `Game` Class
**Priority:** Critical
**Depends on:** TICKET-003, TICKET-004, TICKET-007

The heart of the Template Method pattern. Defines a fixed game loop with abstract hooks.

**Abstract methods (subclasses implement):**
- `doRound(p1, p2, history) : RoundResult`
- `isOver(history) : boolean`
- `computeFinalResult(history) : GameResult`

**Concrete method (never overridden):**
```java
public GameResult play(Participant p1, Participant p2) {
    GameHistory history = new GameHistory(p1.getName(), p2.getName());
    while (!isOver(history)) {
        RoundResult round = doRound(p1, p2, history);
        history.getRounds().add(round);
        notifyMoveMade(new MoveEvent(round, history));
    }
    GameResult result = computeFinalResult(history);
    notifyGameOver(result);
    return result;
}
```

**Observer management:**
- `addObserver(GameObserver o)`
- `removeObserver(GameObserver o)`
- `notifyMoveMade(MoveEvent e)` — private/protected
- `notifyGameOver(GameResult r)` — private/protected

**Acceptance Criteria:**
- [ ] `play()` is `final` — cannot be overridden
- [ ] Observer list is an aggregation (observers not destroyed with game)
- [ ] Notification fires after each round is added to history

---

## Epic 4: Participant (Strategy Pattern)

### TICKET-011 — Create `Participant` Interface
**Priority:** Critical
**Depends on:** TICKET-003, TICKET-001

Strategy interface for decision-making agents.

```java
public interface Participant {
    String getName();
    Action chooseAction(GameHistory history);
    void reset();
}
```

**Acceptance Criteria:**
- [ ] `chooseAction` receives full `GameHistory`, not just last move
- [ ] `reset()` clears any internal state (makes participants reusable across matches)

---

### TICKET-012 — Implement `AlwaysCooperate` Participant
**Priority:** High
**Depends on:** TICKET-011

Simplest possible strategy — always returns `PrisonerAction.COOPERATE`.

**Acceptance Criteria:**
- [ ] Ignores `GameHistory` entirely
- [ ] `reset()` is a no-op
- [ ] Name returns `"AlwaysCooperate"` (or configurable)

---

### TICKET-013 — Implement `TitForTat` Participant
**Priority:** High
**Depends on:** TICKET-011

Mirrors opponent's last action; cooperates on the first round.

```java
public Action chooseAction(GameHistory history) {
    if (history.getRounds().isEmpty()) {
        return PrisonerAction.COOPERATE;
    }
    return history.getLastRound().getActionP2();
}
```

**Acceptance Criteria:**
- [ ] First round always returns `COOPERATE`
- [ ] Subsequent rounds return opponent's last action
- [ ] `reset()` is a no-op (state is fully derived from `GameHistory`)

---

## Epic 5: Game Implementation — Iterated Prisoner's Dilemma

### TICKET-014 — Create `PrisonerAction` Enum
**Priority:** High
**Depends on:** TICKET-001

Game-specific action type for the Prisoner's Dilemma.

```java
public enum PrisonerAction implements Action {
    COOPERATE, DEFECT;

    @Override
    public String getLabel() { return name(); }
}
```

**Acceptance Criteria:**
- [ ] Implements `Action` interface
- [ ] Lives in the game-specific package (not core)
- [ ] Only two values: `COOPERATE`, `DEFECT`

---

### TICKET-015 — Implement `IteratedPrisonersDilemma` Game
**Priority:** High
**Depends on:** TICKET-010, TICKET-014, TICKET-011

Concrete game extending `Game`. Plays a fixed number of rounds.

**Constructor:**
```java
public IteratedPrisonersDilemma(int rounds);
```

**Payoff matrix (`getPayoff`):**
| P1 \ P2 | COOPERATE | DEFECT |
|---------|-----------|--------|
| COOPERATE | (3, 3) | (0, 5) |
| DEFECT | (5, 0) | (1, 1) |

**Methods to implement:**
- `doRound(p1, p2, history)` — calls `chooseAction` on both, computes payoff, returns `RoundResult`
- `isOver(history)` — returns `true` when `history.getRounds().size() >= roundLimit`
- `computeFinalResult(history)` — sums payoffs, determines winner

**Acceptance Criteria:**
- [ ] `getPayoff()` throws `IllegalArgumentException` on unrecognized action types (fail loudly)
- [ ] `getPayoff()` is the **only** place `PrisonerAction` is referenced by name
- [ ] Round count is configurable via constructor

---

## Epic 6: Tournament Format (Strategy Pattern)

### TICKET-016 — Create `TournamentFormat` Interface
**Priority:** High
**Depends on:** TICKET-011, TICKET-005

Strategy interface for tournament organization.

```java
public interface TournamentFormat {
    TournamentResult run(List<Participant> participants, Game game);
}
```

**Acceptance Criteria:**
- [ ] Single method `run()`
- [ ] Returns `TournamentResult`
- [ ] Accepts any `Game` instance

---

### TICKET-017 — Implement `RoundRobin` Tournament Format
**Priority:** High
**Depends on:** TICKET-016, TICKET-010

Every unique pair of participants plays one match.

**Behavior:**
1. Generate all unique pairs from the participant list
2. For each pair, call `participant.reset()` then `game.play(p1, p2)`
3. Collect all `GameResult` objects
4. Return a `TournamentResult`

**Acceptance Criteria:**
- [ ] Each pair plays exactly once (no mirror matches e.g. A vs B and B vs A)
- [ ] `reset()` is called on both participants before each match
- [ ] Works for any number of participants ≥ 2
- [ ] `TournamentResult` contains all `GameResult` objects

---

## Epic 7: Integration & Wiring

### TICKET-018 — Write End-to-End Integration Test / Runner
**Priority:** Medium
**Depends on:** All previous tickets

Verify the full workflow described in Section 6 of the spec.

**Scenario:**
1. Create `IteratedPrisonersDilemma(200)`
2. Create participants: `AlwaysCooperate`, `TitForTat` (at least 3 total for round-robin)
3. Create `MoveLogger` and `ScoreLogger`, register via `addObserver()`
4. Create `RoundRobin`, call `run(participants, game)`
5. Inspect output log files

**Acceptance Criteria:**
- [ ] Log files are created with correct content
- [ ] `MoveLogger` never imports `PrisonerAction`
- [ ] `ScoreLogger` records correct winner for each match
- [ ] No exceptions thrown for standard inputs

---

### TICKET-019 — Add `notifyTournamentOver()` to Observer Chain
**Priority:** Low
**Depends on:** TICKET-007, TICKET-017

Section 6 of the spec mentions `notifyTournamentOver()` firing after all pairs have played.

**Changes needed:**
- Add `onTournamentOver(TournamentResult result)` to `GameObserver`
- `RoundRobin.run()` fires it after all matches complete
- `MoveLogger` and `ScoreLogger` implement the method (no-op or final summary)

**Acceptance Criteria:**
- [ ] Observer interface updated with the new method
- [ ] `RoundRobin` calls it exactly once, at the end
- [ ] Existing observers compile without breaking

---

## Epic 8: Extension Points (Post-MVP)

### TICKET-020 — Add Rock-Paper-Scissors Game
**Priority:** Low
**Depends on:** TICKET-001, TICKET-010, TICKET-011

Demonstrates the open/closed design: no core framework changes needed.

**Deliverables:**
- `RPSAction` enum implementing `Action` (ROCK, PAPER, SCISSORS)
- `RockPaperScissors` extends `Game`
- At least one RPS `Participant` strategy

**Acceptance Criteria:**
- [ ] Zero changes to any file in the core framework
- [ ] Runs through `RoundRobin` without modification
- [ ] `MoveLogger` logs moves using `getLabel()` correctly

---

### TICKET-021 — Add Double Elimination Tournament Format
**Priority:** Low
**Depends on:** TICKET-016

Second implementation of `TournamentFormat`.

**Acceptance Criteria:**
- [ ] Implements `TournamentFormat`
- [ ] Zero changes to `Game`, `Participant`, or observer code
- [ ] Returns a valid `TournamentResult`

---

### TICKET-022 — GUI Observer
**Priority:** Low
**Depends on:** TICKET-007

A GUI component that registers as a `GameObserver` and displays results in real time.

**Acceptance Criteria:**
- [ ] Implements `GameObserver` interface
- [ ] Registered via `addObserver()` — no game code changes
- [ ] Displays round-by-round moves and final scores

---

## Summary Table

| Ticket | Description                     | Priority | Depends On    |
|--------|---------------------------------|----------|---------------|
| 000    | Project setup & package structure | Critical | —           |
| 001    | `Action` interface              | Critical | 000           |
| 002    | `RoundResult` class             | Critical | 001           |
| 003    | `GameHistory` class             | Critical | 002           |
| 004    | `GameResult` class              | High     | 003           |
| 005    | `TournamentResult` class        | High     | 004           |
| 006    | `MoveEvent` class               | High     | 002, 003      |
| 007    | `GameObserver` interface        | High     | 004, 006      |
| 008    | `MoveLogger` implementation     | Medium   | 007           |
| 009    | `ScoreLogger` implementation    | Medium   | 007           |
| 010    | Abstract `Game` class           | Critical | 003, 004, 007 |
| 011    | `Participant` interface         | Critical | 003, 001      |
| 012    | `AlwaysCooperate` participant   | High     | 011           |
| 013    | `TitForTat` participant         | High     | 011           |
| 014    | `PrisonerAction` enum           | High     | 001           |
| 015    | `IteratedPrisonersDilemma` game | High     | 010, 014, 011 |
| 016    | `TournamentFormat` interface    | High     | 011, 005      |
| 017    | `RoundRobin` tournament         | High     | 016, 010      |
| 018    | End-to-end integration test     | Medium   | All           |
| 019    | `notifyTournamentOver()`        | Low      | 007, 017      |
| 020    | Rock-Paper-Scissors game        | Low      | 001, 010, 011 |
| 021    | Double Elimination format       | Low      | 016           |
| 022    | GUI Observer                    | Low      | 007           |
