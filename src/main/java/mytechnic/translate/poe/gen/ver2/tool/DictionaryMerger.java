package mytechnic.translate.poe.gen.ver2.tool;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class DictionaryMerger {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static void process(String inputPath, String outputPath) throws IOException {
        // 텍스트 파일을 읽어 | 구분자로 데이터 파싱 (inputPath)
        Map<String, String> customDictionary = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(inputPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 2) {
                    customDictionary.put(parts[0].trim().toLowerCase(), parts[1].trim());
                }
            }
        }

        // outputPath가 존재하는 경우 기존 파일 읽기
        Map<String, Map<String, String>> outputData = new LinkedHashMap<>();
        File outputFile = new File(outputPath);
        if (outputFile.exists()) {
            outputData = mapper.readValue(outputFile, Map.class);
        }

        // 결과를 저장할 맵 준비
        Map<String, String> pMap = outputData.getOrDefault("p", new LinkedHashMap<>());
        Map<String, String> hMap = outputData.getOrDefault("h", new LinkedHashMap<>());

        // 커스터마이즈 데이터 병합
        customDictionary.forEach((key, value) -> {
            if (key.contains("#") || value.contains("#")) {
                pMap.put(key, value); // pMap에 저장
            } else {
                hMap.put(key, value); // hMap에 저장
            }
        });

        // 결과를 outputData에 추가
        outputData.put("p", pMap);
        outputData.put("h", hMap);

        // 결과를 outputPath에 저장
        mapper.writeValue(outputFile, outputData); // 결과 파일에 저장
    }
}
