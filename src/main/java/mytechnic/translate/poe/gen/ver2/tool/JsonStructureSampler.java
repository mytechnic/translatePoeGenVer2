package mytechnic.translate.poe.gen.ver2.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class JsonStructureSampler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public static void process(String inputPath, String outputPath) throws Exception {
        JsonStructureSampler sampler = new JsonStructureSampler();
        sampler.extractSamples(inputPath, outputPath);
    }

    public void extractSamples(String inputFilePath, String outputFilePath) throws IOException {
        JsonNode root = objectMapper.readTree(new File(inputFilePath));
        JsonNode outputRoot = processNode(root);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputFilePath), outputRoot);
    }

    private JsonNode processNode(JsonNode node) {
        if (node.isArray()) {
            ArrayNode newArray = objectMapper.createArrayNode();
            Set<String> seen = new HashSet<>();
            for (JsonNode element : node) {
                String key = getStructureKey(element);
                if (seen.add(key)) {
                    newArray.add(processNode(element));
                }
            }
            return newArray;
        } else if (node.isObject()) {
            ObjectNode newObject = objectMapper.createObjectNode();
            node.fieldNames().forEachRemaining(field -> {
                newObject.set(field, processNode(node.get(field)));
            });
            return newObject;
        } else {
            return node;
        }
    }

    private String getStructureKey(JsonNode node) {
        if (node.isObject()) {
            List<String> fields = new ArrayList<>();
            node.fieldNames().forEachRemaining(field -> {
                String valueStructure = getStructureKey(node.get(field));
                fields.add(field + ":" + valueStructure);
            });
            Collections.sort(fields);
            return "{" + String.join(",", fields) + "}";
        } else if (node.isArray()) {
            List<String> elementStructures = new ArrayList<>();
            for (JsonNode element : node) {
                elementStructures.add(getStructureKey(element));
            }
            Collections.sort(elementStructures);
            return "[" + String.join(",", elementStructures) + "]";
        } else {
            return node.getNodeType().name();
        }
    }
}