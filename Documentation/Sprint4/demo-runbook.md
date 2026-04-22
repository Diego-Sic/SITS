# Sprint 4 Part 1 — Networked Demo Runbook

Three machines run the same repository on branch `sprint4-part1` (or `main` after merge). Each machine plays one role. No code differs per machine — only the launch command and the IPs you type in.

---

## 0. Role-to-machine mapping

| Machine | Role                   | Entry point                         | Represents |
| ------- | ---------------------- | ----------------------------------- | ---------- |
| **A**   | Tournament server      | `sits.server.TournamentServerApp`   | Sprint 2 — networked tournament host |
| **B**   | Remote participants    | `sits.client.ClientApp` (×2 runs)   | Sprint 1 — IPD strategies (TitForTat, AlwaysDefect) running remotely via the `RemoteParticipant` proxy |
| **C**   | Viewer GUI             | `sits.viewer.ViewerApp`             | Sprint 3 — JavaFX live viewer over SSE |

Demo narrative: "Sprint 1 strategies on Machine B play an IPD tournament hosted by the Sprint 2 server on Machine A, while a Sprint 3 viewer on Machine C observes the moves live."

---

## 1. Pre-class checklist (do this BEFORE demo day)

On **every** machine:

1. Clone/pull the repo to a known path.
2. Checkout the correct branch: `git checkout sprint4-part1` (or `main` post-merge).
3. Verify Java 17: `java -version`.
4. Verify Maven build succeeds: `mvn -q -DskipTests package`.
5. Open the project in Eclipse and let it build once.

Only on **Machine A** (server):

6. Allow inbound TCP on port **8080**:
   - **macOS:** System Settings → Network → Firewall → Options → allow `java`.
   - **Windows:** `New-NetFirewallRule -DisplayName "SITS 8080" -Direction Inbound -LocalPort 8080 -Protocol TCP -Action Allow` (admin PowerShell).
   - **Linux:** `sudo ufw allow 8080/tcp`.

Only on **Machine B** (clients):

7. The server calls back into each client on a random high port. Relax the firewall for `java` **outbound+inbound** or just disable the firewall for the demo's short duration. If your OS silently drops inbound, the server will hang waiting for the client's `/action` response.

---

## 2. Discover the server's LAN IP (Machine A)

Run on Machine A and note the value — you'll type it on B and C.

- **macOS:** `ipconfig getifaddr en0` (Ethernet) or `ipconfig getifaddr en1` (Wi-Fi). If both fail, `ifconfig | grep "inet " | grep -v 127.0.0.1`.
- **Windows:** `ipconfig` → look under the active adapter for "IPv4 Address".
- **Linux:** `hostname -I | awk '{print $1}'`.

Example value used below: `192.168.1.42`. Replace everywhere.

Sanity check from Machine B: `ping 192.168.1.42` should respond. If not, you're on different subnets or the firewall is blocking ICMP — the demo will fail, fix the network first.

---

## 3. Launch sequence

Start in this order: **A → B → C**. The server must be up before clients register; clients must be registered before the viewer starts the tournament.

### 3.1 Machine A — Tournament Server

In Eclipse: right-click `sits.server.TournamentServerApp` → Run As → Java Application.

Or from terminal at repo root:

```bash
mvn spring-boot:run -Dspring-boot.run.mainClass=sits.server.TournamentServerApp
```

Wait for console output:

```
Started TournamentServerApp in X.X seconds
Tomcat started on port(s): 8080
```

Tournament `ipd-01` is auto-seeded and auto-started by `seedTournaments` in `TournamentServerApp`. It is in `REGISTERING` state waiting for participants.

Verify from Machine A: `curl http://localhost:8080/tournaments` → should return a JSON array containing `ipd-01`.
Verify from Machine B: `curl http://192.168.1.42:8080/tournaments` → same result. **If this fails, stop and fix the network/firewall before going further.**

### 3.2 Machine B — Remote participants (run TWICE)

In Eclipse: right-click `sits.client.ClientApp` → Run As → Run Configurations → create a new Java Application config.

**Client 1 — program arguments:**

```
--tournament.server.url=http://192.168.1.42:8080 --tournament.id=ipd-01 --participant.name=TitForTat --server.port=0
```

**Client 2 — program arguments (duplicate the config):**

```
--tournament.server.url=http://192.168.1.42:8080 --tournament.id=ipd-01 --participant.name=AlwaysDefect --server.port=0
```

From terminal equivalent:

```bash
mvn spring-boot:run \
  -Dspring-boot.run.mainClass=sits.client.ClientApp \
  -Dspring-boot.run.arguments="--tournament.server.url=http://192.168.1.42:8080,--tournament.id=ipd-01,--participant.name=TitForTat,--server.port=0"
```

On successful registration the server's console logs the participant. Confirm from Machine A:

```bash
curl http://localhost:8080/tournaments
# look for "participants":["TitForTat","AlwaysDefect"]  (plus the seeded strategies)
```

> ⚠️ **Known ClientApp caveat** — `ClientApp.onReady()` uses `InetAddress.getLocalHost().getHostAddress()` to report the client's IP to the server. If that call returns `127.0.0.1` (common on macOS with certain `/etc/hosts` setups) the server will try to call back to its own loopback and the tournament will stall silently. If you see the server hang after clients register, override the client's advertised host — see "Mitigation" at the bottom of this file.

### 3.3 Machine C — Viewer

In Eclipse: right-click `sits.viewer.ViewerApp` → Run As → Java Application.

If JavaFX modules are missing, add VM arguments:

```
--module-path "${M2_REPO}/org/openjfx" --add-modules javafx.controls,javafx.fxml
```

On the **Connect** screen:

- **IP:** `192.168.1.42`  ← the server's LAN IP, NOT `localhost`
- **Port:** `8080`

Click **Connect** → Lobby → select `ipd-01` → **Start Tournament**.

Live moves should stream into the viewer within ~1 second.

---

## 4. Demo-day order of operations (5-minute window)

1. **0:00** — Boot Machine A server. (Pre-started is fine.)
2. **0:30** — Register both ClientApps on Machine B.
3. **1:00** — Connect ViewerApp on Machine C, land on lobby.
4. **1:15** — Click "Start Tournament" in viewer.
5. **1:15–4:30** — Narrate: Sprint 1 strategies (on B), Sprint 2 server/registry/broadcaster (on A), Sprint 3 SSE viewer (on C).
6. **4:30** — Point out tournament completion; optionally `curl /tournaments` from a fourth terminal to show server state transitioned `REGISTERING → RUNNING → COMPLETED`.

Keep a terminal on each machine open with the launch command in history (`↑ ↑ Enter`) so restarts during handoff are fast.

---

## 5. Troubleshooting quick-reference

| Symptom | Likely cause | Fix |
|---|---|---|
| Viewer "Failed to load lobby screen" | Server unreachable from Machine C | Re-check LAN IP, firewall, ping |
| Clients register but tournament never progresses moves | Server cannot call back to client (see ClientApp caveat §3.2) | Check client's reported IP in server log; use `--participant.host=<lan-ip>` override (requires the ClientApp patch — see §6) |
| Viewer shows empty lobby | Tournament not seeded, or wrong tournament.id | Restart server; confirm `curl /tournaments` returns `ipd-01` |
| `Address already in use` on server | Port 8080 taken by prior run | Kill old JVM: `lsof -ti:8080 \| xargs kill -9` (macOS/Linux) |
| Viewer connects but no moves stream | SSE blocked by corporate proxy | Use phone hotspot or a dedicated switch |

---

## 6. Mitigation: ClientApp host override (proposed follow-up)

`src/main/java/sits/client/ClientApp.java:34` currently hard-calls `InetAddress.getLocalHost().getHostAddress()`. Proposed patch (not yet applied):

- Add `@Value("${participant.host:}")` field.
- If non-empty, use it verbatim; otherwise fall back to the current auto-discovery.

Callers on Machine B would then add `--participant.host=192.168.1.57` (Machine B's own LAN IP) to each client run config, removing the macOS loopback risk entirely.

This change is tracked as a Sprint 4 Part 1 follow-up ticket; apply before demo day if possible.
