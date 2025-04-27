// mytechnic/translate/poe/gen/ver2/tool/JsonStructureConverter.java
package mytechnic.translate.poe.gen.ver2.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Slf4j
public class JsonStructureConverter {

    public static void main(String[] args) throws IOException {
        // 메인 함수에서는 convertJsonStructure 메소드만 호출
        convertJsonStructure("input.json", "output.json");
    }

    /**
     * 주어진 JSON 파일을 변환하여 새로운 형식으로 저장하는 메소드
     *
     * @param inputFilePath  원본 JSON 파일 경로
     * @param outputFilePath 변환된 JSON 파일을 저장할 경로
     * @throws IOException 파일 입출력 예외
     */
    public static void convertJsonStructure(String inputFilePath, String outputFilePath) throws IOException {
        // ObjectMapper 초기화
        ObjectMapper mapper = new ObjectMapper();

        // 원본 JSON 파일 읽기
        JsonNode rootNode = mapper.readTree(new File(inputFilePath));

        // 변환된 JSON을 저장할 배열
        ArrayNode resultArray = mapper.createArrayNode();

        // result 배열을 순회하면서 변환
        for (JsonNode item : rootNode.get("result")) {
            ObjectNode newItem = createItemNode(item, mapper);
            resultArray.add(newItem);
        }

        // 변환된 JSON을 출력 및 저장
        saveJsonToFile(resultArray, outputFilePath, mapper);
    }

    /**
     * "result" 항목을 변환하여 ObjectNode로 반환하는 메소드
     *
     * @param item   원본 JSON 항목
     * @param mapper ObjectMapper
     * @return 변환된 ObjectNode
     */
    private static ObjectNode createItemNode(JsonNode item, ObjectMapper mapper) {
        ObjectNode newItem = mapper.createObjectNode();
        newItem.put("id", safeGetText(item, "id"));
        newItem.put("label", safeGetText(item, "label"));

        // entries 배열 변환
        ObjectNode entriesObject = createEntriesObject(item.get("entries"), mapper);
        newItem.set("entries", entriesObject);

        return newItem;
    }

    /**
     * "entries" 항목을 변환하여 ArrayNode로 반환하는 메소드
     *
     * @param entries "entries" JSON 배열
     * @param mapper  ObjectMapper
     * @return 변환된 ArrayNode
     */
    private static ObjectNode createEntriesObject(JsonNode entries, ObjectMapper mapper) {

        Set<String> fields = new HashSet<>();
        for (JsonNode entryItem : entries) {
            Iterator<String> fieldNames = entryItem.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                if ("image".equals(fieldName)) {
                    continue;
                }

                JsonNode fieldValue = entryItem.get(fieldName);
                if (!fieldValue.isTextual()) {
                    continue;
                }

                fields.add(fieldName);
            }
        }

        ObjectNode newEntries = mapper.createObjectNode();
        for (String field : fields) {
            ArrayNode entriesArray = mapper.createArrayNode();
            for (JsonNode entryItem : entries) {
                if (!entryItem.has(field)) {
                    entriesArray.addNull();
                    continue;
                }

                JsonNode fieldValue = entryItem.get(field);
                if (fieldValue.isTextual()) {
                    entriesArray.add(fieldValue);
                }
            }

            newEntries.putIfAbsent(field, entriesArray);
        }

        ArrayNode optionsNode = mapper.createArrayNode();
        for (JsonNode entryItem : entries) {
            if (!entryItem.has("text") || !entryItem.has("option") || !entryItem.get("option").has("options")) {
                optionsNode.addNull();
                continue;
            }

            JsonNode options = entryItem.get("option").get("options");
            if (!options.isArray()) {
                optionsNode.addNull();
                continue;
            }

            String textValue = entryItem.get("text").textValue();
            if (textValue.contains("#")) {
                ArrayNode entriesArray = mapper.createArrayNode();
                for (JsonNode option : options) {
                    String optionText = option.get("text").textValue();
                    entriesArray.add(optionText);
                }
                optionsNode.add(entriesArray);
            }
        }
        if (!optionsNode.isEmpty()) {
            newEntries.putIfAbsent("options", optionsNode);
        }

        return newEntries;
    }

    /**
     * 주어진 JsonNode에서 특정 필드를 안전하게 가져오는 메소드
     *
     * @param node      JsonNode
     * @param fieldName 가져올 필드 이름
     * @return 필드 값 (존재하지 않으면 null)
     */
    private static String safeGetText(JsonNode node, String fieldName) {
        if (node != null && node.has(fieldName)) {
            JsonNode fieldNode = node.get(fieldName);
            return fieldNode != null && !fieldNode.isNull() ? fieldNode.asText() : null;
        }
        return null;
    }

    /**
     * 변환된 JSON을 파일로 저장하는 메소드
     *
     * @param jsonNode       변환된 JSON
     * @param outputFilePath 저장할 파일 경로
     * @param mapper         ObjectMapper
     * @throws IOException 파일 입출력 예외
     */
    private static void saveJsonToFile(JsonNode jsonNode, String outputFilePath, ObjectMapper mapper) throws
            IOException {
        File outputFile = new File(outputFilePath);
        mapper.writeValue(outputFile, jsonNode);
    }
}
