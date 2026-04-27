package sits.replay;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class ReplayStore {

    private final Path directory;
    private final ObjectMapper mapper;

    public ReplayStore(Path directory) {
        this.directory = directory;
        this.mapper = buildMapper();
    }

    public void save(String tournamentId, List<ReplayCommand> commands) throws IOException {
        ReplayFile.Meta meta = new ReplayFile.Meta(tournamentId, Instant.now().toString(), commands.size());
        ReplayFile file = new ReplayFile(meta, commands);
        Path dest = directory.resolve(tournamentId + ".replay");
        mapper.writerWithDefaultPrettyPrinter().writeValue(dest.toFile(), file);
    }

    public ReplayFile load(Path file) throws IOException {
        return mapper.readValue(file.toFile(), ReplayFile.class);
    }

    public List<ReplayFile.Meta> list() throws IOException {
        if (!Files.exists(directory))
            return List.of();
        try (Stream<Path> files = Files.list(directory)) {
            return files
                    .filter(p -> p.getFileName().toString().endsWith(".replay"))
                    .map(this::readMeta)
                    .filter(m -> m != null)
                    .toList();
        }
    }

    private ReplayFile.Meta readMeta(Path file) {
        try {
            JsonNode root = mapper.readTree(file.toFile());
            return mapper.treeToValue(root.get("meta"), ReplayFile.Meta.class);
        } catch (IOException e) {
            return null;
        }
    }

    private static ObjectMapper buildMapper() {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(ReplayCommand.class, new ReplayCommandDeserializer());
        return new ObjectMapper()
                .registerModule(module)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
