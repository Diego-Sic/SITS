package sits.viewer;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import sits.networking.dto.MoveEventDTO;

@ExtendWith(MockitoExtension.class)
class ServerConnectionTest {

        private HttpClient client;
        private ServerConnection connection;

        @BeforeEach
        void setUp() {
                client = mock(HttpClient.class);
                // Use Runnable::run as dispatcher so callbacks execute synchronously in tests
                // (no JavaFX toolkit needed)
                connection = new ServerConnection("http://localhost:8080", client,
                                new ObjectMapper(), Runnable::run);
        }

        // fetchTournaments tests :D

        @Test
        @SuppressWarnings("unchecked")
        void fetchTournaments_returnsEmptyList() throws Exception {
                HttpResponse<String> response = mock(HttpResponse.class);
                when(response.body()).thenReturn("[]");
                when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                                .thenReturn(response);

                List<TournamentInfo> result = connection.fetchTournaments();

                assertThat(result).isEmpty();
        }

        @Test
        @SuppressWarnings("unchecked")
        void fetchTournaments_parsesIdNameAndStatus() throws Exception {
                String json = "[{\"id\":\"t1\",\"name\":\"IPD Tournament\",\"status\":\"REGISTERING\"}]";
                HttpResponse<String> response = mock(HttpResponse.class);
                when(response.body()).thenReturn(json);
                when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                                .thenReturn(response);

                List<TournamentInfo> result = connection.fetchTournaments();

                assertThat(result).hasSize(1);
                assertThat(result.get(0).id).isEqualTo("t1");
                assertThat(result.get(0).name).isEqualTo("IPD Tournament");
                assertThat(result.get(0).status).isEqualTo("REGISTERING");
        }

        // startTournament tests :D

        @Test
        @SuppressWarnings("unchecked")
        void startTournament_returnsNormally_onSuccess() throws Exception {
                HttpResponse<Void> response = mock(HttpResponse.class);
                when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                                .thenReturn(response);

                connection.startTournament("t1");
                // No exception = pass; body is discarded so nothing else to assert here.
        }

        @Test
        @SuppressWarnings("unchecked")
        void startTournament_sendsPostToCorrectUrl() throws Exception {
                HttpResponse<Void> response = mock(HttpResponse.class);
                when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                                .thenReturn(response);

                connection.startTournament("t1");

                ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
                verify(client).send(captor.capture(), any(HttpResponse.BodyHandler.class));
                HttpRequest sent = captor.getValue();
                assertThat(sent.uri().toString()).isEqualTo("http://localhost:8080/tournaments/t1/start");
                assertThat(sent.method()).isEqualTo("POST");
        }

        @Test
        @SuppressWarnings("unchecked")
        void startTournament_propagatesIOException() throws Exception {
                when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                                .thenThrow(new IOException("boom"));

                assertThatThrownBy(() -> connection.startTournament("t1"))
                                .isInstanceOf(IOException.class)
                                .hasMessage("boom");
        }

        @Test
        @SuppressWarnings("unchecked")
        void startTournament_propagatesInterruptedException() throws Exception {
                when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                                .thenThrow(new InterruptedException("interrupted"));

                assertThatThrownBy(() -> connection.startTournament("t1"))
                                .isInstanceOf(InterruptedException.class)
                                .hasMessage("interrupted");
        }

        // tests to see if it communincates the moves

        @Test
        @SuppressWarnings("unchecked")
        void streamMoves_callsOnEventForMoveLines() throws Exception {
                // This example was done using ai because I didn't want to write so much
                String line = "data: {\"type\":\"MOVE\",\"roundNumber\":1," +
                                "\"p1Name\":\"Alice\",\"p2Name\":\"Bob\"," +
                                "\"action1\":\"COOPERATE\",\"action2\":\"DEFECT\"," +
                                "\"payoff1\":3,\"payoff2\":5}";

                HttpResponse<Stream<String>> response = mock(HttpResponse.class);
                when(response.body()).thenReturn(Stream.of(line));
                when(client.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                                .thenReturn(CompletableFuture.completedFuture(response));

                List<MoveEventDTO> received = new ArrayList<>();
                connection.streamMoves("t1", received::add, () -> {
                }).get();

                assertThat(received).hasSize(1);
                assertThat(received.get(0).type).isEqualTo("MOVE");
                assertThat(received.get(0).p1Name).isEqualTo("Alice");
                assertThat(received.get(0).roundNumber).isEqualTo(1);
        }

        @Test
        @SuppressWarnings("unchecked")
        void streamMoves_callsOnDoneWhenStreamCloses() throws Exception {
                HttpResponse<Stream<String>> response = mock(HttpResponse.class);
                when(response.body()).thenReturn(Stream.of());
                when(client.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                                .thenReturn(CompletableFuture.completedFuture(response));

                AtomicBoolean doneCalled = new AtomicBoolean(false);
                connection.streamMoves("t1", dto -> {
                }, () -> doneCalled.set(true)).get();

                assertThat(doneCalled.get()).isTrue();
        }

        @Test
        @SuppressWarnings("unchecked")
        void streamMoves_skipsUnparsableLines() throws Exception {
                HttpResponse<Stream<String>> response = mock(HttpResponse.class);
                when(response.body()).thenReturn(Stream.of("data: not-valid-json"));
                when(client.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                                .thenReturn(CompletableFuture.completedFuture(response));

                List<MoveEventDTO> received = new ArrayList<>();
                connection.streamMoves("t1", received::add, () -> {
                }).get();

                assertThat(received).isEmpty();
        }
}
