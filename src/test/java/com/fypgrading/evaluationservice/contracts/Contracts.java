package com.fypgrading.evaluationservice.contracts;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Checks this service against the payload fixtures in {@code src/test/resources/contracts}. The same fixture
 * files are in every service that sends or reads the payload (keep them identical: the docker-compose repo's
 * {@code scripts/check-shared-code.sh} compares them). A producer must emit exactly the fixture's fields; a
 * consumer must read the fixture.
 */
final class Contracts {

    static final JsonMapper JSON = JsonMapper.builder().build();

    private Contracts() {}

    static JsonNode fixture(String name) {
        try (InputStream in = Contracts.class.getResourceAsStream("/contracts/" + name + ".json")) {
            return JSON.readTree(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static <T> T read(String name, Class<T> type) {
        return JSON.treeToValue(fixture(name), type);
    }

    /** Every field path the producer writes is in the fixture, and the other way round. */
    static void assertProducesExactly(String name, Object produced) {
        assertThat(paths(JSON.valueToTree(produced), ""))
                .as("fields of %s", name)
                .isEqualTo(paths(fixture(name), ""));
    }

    private static TreeSet<String> paths(JsonNode node, String prefix) {
        TreeSet<String> out = new TreeSet<>();
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.properties().iterator();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String path = prefix + field.getKey();
                out.add(path);
                out.addAll(paths(field.getValue(), path + "."));
            }
        } else if (node.isArray() && !node.isEmpty()) {
            out.addAll(paths(node.get(0), prefix + "[]."));
        }
        return out;
    }
}
