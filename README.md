# SITS — Spectator-Interactive Tournament Simulator

Hola, here are the instructions to run the SITS :3

## Running the project

If use in the terminal :

### 1. Start the tournament server

```bash
mvn spring-boot:run
```

Starts the REST server on `http://localhost:8080`. Comes pre-seeded with one IPD tournament (`ipd-01`).

### 2. Start the viewer (JavaFX GUI)

```bash
mvn clean javafx:run
```

Opens the viewer client. Connect to `localhost:8080` to browse tournaments and watch games in real time.

### 3. Register a remote participant

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--tournament.id=ipd-01 --participant.name=TitForTat"
```

Registers a strategy client with the running server. Run this command twice (with different names) to have two participants before starting a tournament.
