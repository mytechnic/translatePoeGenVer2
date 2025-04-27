// JsonDictionaryComparator.java
package mytechnic.translate.poe.gen.ver2.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 두 개의 JSON 파일 (old, new)을 비교하고, `h`, `p` 이하 데이터를 필터링하여 key 오름차순 정렬 후 비교하는 클래스
 */
public class JsonDictionaryComparator {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 주어진 JSON 파일을 로드하고, `h`와 `p` 필드를 처리하여 정렬된 사전 데이터를 반환
     */
    public static Map<String, String> loadAndProcessDictionary(String filePath) throws IOException {
        log("파일 로딩 중: " + filePath);
        JsonNode root = loadJson(filePath);
        Map<String, String> dictionaryMap = new LinkedHashMap<>();

        // "h"와 "p" 필드 처리
        processField(root, "h", dictionaryMap);
        processField(root, "p", dictionaryMap);

        log("사전 데이터 처리 완료: " + dictionaryMap.size() + " 항목");
        return dictionaryMap;
    }

    /**
     * JSON 파일에서 특정 필드를 처리하여 사전 데이터를 맵에 추가
     */
    private static void processField(JsonNode root, String field, Map<String, String> dictionaryMap) {
        if (root.has(field)) {
            JsonNode fieldNode = root.get(field);
            fieldNode.fields().forEachRemaining(entry ->
                    dictionaryMap.put(entry.getKey().toLowerCase(), entry.getValue().asText())
            );
        }
    }

    /**
     * JSON 파일을 로드하는 메소드
     */
    private static JsonNode loadJson(String path) throws IOException {
        return objectMapper.readTree(new File(path));
    }

    /**
     * 두 사전 데이터를 비교하고, 부족한 항목과 추가된 항목을 출력
     */
    public static void compareDictionaries(Map<String, String> oldDictionary, Map<String, String> newDictionary) {
        log("사전 비교 중...");

        // new 사전에서만 있는 항목과 값이 일치하지 않는 항목들을 출력
        for (Map.Entry<String, String> entry : newDictionary.entrySet()) {
            String key = entry.getKey();
            String newValue = entry.getValue();

            // old 사전에는 없고, new 사전에는 있는 항목
            if (!oldDictionary.containsKey(key)) {
                log("추가됨: " + key + " = " + newValue);
            }
            // old 사전에는 있지만, 값이 일치하지 않는 경우
            else if (!oldDictionary.get(key).equals(newValue)) {
                log("불일치: " + key + "\nOld: " + oldDictionary.get(key) + "\nNew: " + newValue);
            }
        }

        // old 사전에서만 있는 항목을 출력
        for (Map.Entry<String, String> entry : oldDictionary.entrySet()) {
            String key = entry.getKey();
            if (!newDictionary.containsKey(key)) {
                log("누락됨: " + key);
            }
        }
    }

    /**
     * 두 JSON 파일을 비교하는 메소드 (파일 경로를 인자로 받음)
     */
    public static void compareDictionariesFromFiles(String oldDicPath, String newDicPath) {
        log("old와 new 사전 비교 시작...");
        try {
            // old 사전 데이터와 new 사전 데이터 로드
            Map<String, String> oldDictionary = loadAndProcessDictionary(oldDicPath);
            Map<String, String> newDictionary = loadAndProcessDictionary(newDicPath);

            // 사전 데이터를 오름차순으로 정렬
            oldDictionary = sortDictionaryByKey(oldDictionary);
            newDictionary = sortDictionaryByKey(newDictionary);

            // 사전 데이터 비교
            compareDictionaries(oldDictionary, newDictionary);
        } catch (IOException e) {
            System.err.println("파일 처리 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * 주어진 사전 데이터를 키 기준으로 오름차순 정렬
     */
    private static Map<String, String> sortDictionaryByKey(Map<String, String> dictionary) {
        log("사전 키 오름차순 정렬 중...");
        return dictionary.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * 로그 출력 (디버깅용)
     */
    private static void log(String message) {
        System.out.println("[로그] " + message);
    }
}
