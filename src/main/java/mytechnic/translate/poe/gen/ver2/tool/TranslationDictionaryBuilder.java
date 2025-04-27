package mytechnic.translate.poe.gen.ver2.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TranslationDictionaryBuilder {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static void buildFromFiles(List<String> inputFilePaths, String outputFilePath) throws IOException {
        Map<String, String> pMap = new LinkedHashMap<>();
        Map<String, String> hMap = new LinkedHashMap<>();

        File outputFile = new File(outputFilePath);
        if (outputFile.exists()) {
            Map<String, Map<String, String>> existingData = mapper.readValue(outputFile, new TypeReference<>() {
            });
            if (existingData.containsKey("p")) {
                pMap.putAll(existingData.get("p"));
            }
            if (existingData.containsKey("h")) {
                hMap.putAll(existingData.get("h"));
            }
        }

        for (String path : inputFilePaths) {
            Map<String, String> inputMap = mapper.readValue(new File(path), new TypeReference<>() {
            });
            for (Map.Entry<String, String> entry : inputMap.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if (key.contains("#") || value.contains("#")) {
                    if (pMap.containsKey(key) && !pMap.get(key).equals(value)) {
                        System.out.println("[충돌] pMap 중복 key: " + key);
                        System.out.println("  기존 = " + pMap.get(key));
                        System.out.println("  새값 = " + value);
                    }
                    pMap.put(key, value);
                } else {
                    if (hMap.containsKey(key) && !hMap.get(key).equals(value)) {
                        System.out.println("[충돌] hMap 중복 key: " + key);
                        System.out.println("  기존 = " + hMap.get(key));
                        System.out.println("  새값 = " + value);
                    }
                    hMap.put(key, value);
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("h", hMap);
        result.put("p", pMap);

        mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, result);
    }
}
