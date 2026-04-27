package sits.replay;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class ReplayCommandDeserializer extends StdDeserializer<ReplayCommand> {

    public ReplayCommandDeserializer() {
        super(ReplayCommand.class);
    }

    @Override
    public ReplayCommand deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode node = mapper.readTree(p);

        JsonNode typeNode = node.get("type");
        if (typeNode == null || typeNode.isNull()) {
            throw new IOException("Missing 'type' field in ReplayCommand JSON");
        }

        Class<? extends ReplayCommand> cls = CommandTypeRegistry.get(typeNode.asText());
        return mapper.treeToValue(node, cls);
    }
}
