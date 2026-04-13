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
import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.application.Platform;
import sits.networking.dto.MoveEventDTO;

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
