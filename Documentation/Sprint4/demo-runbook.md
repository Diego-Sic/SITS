# Sprint 4 Part 1 — Demo Runbook

| Machine | Role              |
| ------- | ----------------- |
| **A**   | Tournament server |
| **B**   | Two clients       |
| **C**   | Viewer GUI        |

All commands are `make` targets :D

## Prereqs

```bash
sudo apt install -y openjdk-17-jdk maven curl lsof ufw   # Java 17 to run the apps, Maven to build, curl for the health check, lsof to free a stuck port, ufw to open the firewall
git clone <repo-url> SITS && cd SITS                     # grab the repo so every machine has the same Makefile and code
```

## Machine A — server

```bash
make open-port    # opens TCP 8080 so B and C can reach the server (run once per machine)
make ip           # prints this machine's LAN IP — note it, you'll pass it to B and C as SERVER=<ip>
make server       # launches TournamentServerApp on :8080 and auto-seeds tournament ipd-01; leave this terminal running
```

## Machine B — clients (two terminals)

```bash
make client1 SERVER=<A_ip> PARTICIPANT_HOST=<B_ip>   # registers the first participant (default strategy: TitForTat) with the server on Machine A
make client2 SERVER=<A_ip> PARTICIPANT_HOST=<B_ip>   # registers the second participant (default strategy: AlwaysDefect) — run in a separate terminal so logs stay readable
```

> **Why `PARTICIPANT_HOST`?** On Linux `InetAddress.getLocalHost()` resolves to `127.0.0.1`,
> so without this flag Machine A would store a loopback address for Machine B and fail to reach it during the tournament.
> Use `make ip` on Machine B to get `<B_ip>`.

## Machine C — viewer

```bash
make viewer    # launches the JavaFX ViewerApp (mvn javafx:run); the GUI opens on this machine's display
```

In the GUI: **IP** = Machine A's LAN IP (not `localhost`), **Port** = `8080` → Connect → select `ipd-01` → **Start Tournament**.

## Quick sanity check

From any machine: `make check SERVER=<A_ip>` — should return JSON listing `ipd-01`. If this fails, fix the network first.
