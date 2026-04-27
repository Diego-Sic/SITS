package sits.viewer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import javafx.application.Platform;
import sits.networking.dto.MoveEventDTO;
import sits.replay.ReplayCommand;
import sits.replay.ReplayCommandDeserializer;
import sits.replay.ReplayFile;

public class ServerConnection {

    private final String baseUrl;
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final Consumer<Runnable> dispatcher;

    public ServerConnection(String baseUrl) {
        this(baseUrl, HttpClient.newHttpClient(), new ObjectMapper(), Platform::runLater);
    }

    // Package-private constructor for testing
    ServerConnection(String baseUrl, HttpClient client, ObjectMapper mapper,
            Consumer<Runnable> dispatcher) {
        this.baseUrl = baseUrl;
        this.client = client;
        this.mapper = mapper;
        this.dispatcher = dispatcher;
    }

    // Calls GET /tournaments and deserializes the JSON array into TournamentInfo
    // objects.
    public List<TournamentInfo> fetchTournaments() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/tournaments"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return mapper.readValue(response.body(),
                mapper.getTypeFactory().constructCollectionType(List.class, TournamentInfo.class));
    }

    // Sends POST /tournaments/{id}/start (async on server; returns as soon as 202 is received).
    public void startTournament(String tournamentId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/tournaments/" + tournamentId + "/start"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        client.send(request, HttpResponse.BodyHandlers.discarding());
    }

    public List<ReplayFile.Meta> fetchReplays() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/replays"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return mapper.readValue(response.body(),
                mapper.getTypeFactory().constructCollectionType(List.class, ReplayFile.Meta.class));
    }

    public ReplayFile fetchReplayFile(String id) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/replays/" + id))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return buildReplayMapper().readValue(response.body(), ReplayFile.class);
    }

    private static ObjectMapper buildReplayMapper() {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(ReplayCommand.class, new ReplayCommandDeserializer());
        return new ObjectMapper()
                .registerModule(module)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    // Opens a persistent SSE connection to GET /tournaments/{id}/stream.
    // Each incoming data: line is deserialized to a MoveEventDTO and passed to
    // onEvent.
    public CompletableFuture<Void> streamMoves(String tournamentId,
            Consumer<MoveEventDTO> onEvent,
            Runnable onDone) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/tournaments/" + tournamentId + "/stream"))
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofLines())
                .thenAccept(response -> {
                    response.body()
                            .filter(line -> line.startsWith("data:"))
                            .map(line -> line.substring(5).trim())
                            .forEach(json -> {
                                try {
                                    MoveEventDTO dto = mapper.readValue(json, MoveEventDTO.class);
                                    dispatcher.accept(() -> onEvent.accept(dto));
                                } catch (JsonProcessingException ignored) {
                                }
                            });
                    dispatcher.accept(onDone);
                });
    }
}
