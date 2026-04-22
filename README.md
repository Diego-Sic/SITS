# SITS — Spectator-Interactive Tournament Simulator

Hola, here are the instructions to run the SITS :3

## Default Demo Address

| Component | IP        | Port | Full URL                |
| --------- | --------- | ---- | ----------------------- |
| Server    | localhost | 8080 | `http://localhost:8080` |

## Pre-seeded Tournament

The server starts with one IPD tournament already registered so you can demo immediately:

| Field         | Value                       |
| ------------- | --------------------------- |
| Tournament ID | `ipd-01`                    |
| Game          | Iterated Prisoner's Dilemma |

## Running the project

#### 1. Import the project

1. `File → Import… → Maven → Existing Maven Projects`.
2. Select the `SITS` root folder (the one containing `pom.xml`) and click **Finish**.
3. Wait for Eclipse to resolve dependencies (`Project → Build Automatically` should be on).
4. Confirm the JRE is set to Java 17: right-click the project → `Properties → Java Build Path → Libraries → JRE System Library`.

#### 2. Start the tournament server

1. In the Package Explorer open `src/main/java/sits/server/TournamentServerApp.java`.
2. Right-click the file → `Run As → Java Application`.
3. The console should log `Started TournamentServerApp` and expose the REST API on `http://localhost:8080`. Tournament `ipd-01` is auto-seeded.

#### 3. Start the viewer (JavaFX GUI)

1. Open `src/main/java/sits/viewer/ViewerApp.java`.
2. Right-click the file → `Run As → Java Application`.
3. If JavaFX complains about missing modules, use `Run → Run Configurations… → Arguments → VM arguments` and add:
   ```
   --module-path "${M2_REPO}/org/openjfx" --add-modules javafx.controls,javafx.fxml
   ```
4. On the **Connect** screen enter the demo values:
   - **IP:** `localhost`
   - **Port:** `8080`
5. Click **Connect** to move to the lobby, then **Start Tournament** once two participants have registered.

#### 4. Register remote participants

1. Open `src/main/java/sits/client/ClientApp.java`.
2. Right-click → `Run As → Run Configurations…` and create a new **Java Application** configuration.
3. On the **Arguments** tab, under **Program arguments**, enter:
   ```
   --tournament.id=ipd-01 --participant.name=TitForTat --server.port=0
   ```
4. Click **Run**. The client boots on a random local port and auto-registers with the server.
5. Duplicate the run configuration and change `participant.name` (e.g. `AlwaysDefect`) to register the second participant. The tournament needs at least **two registered participants** before the viewer can start it.
