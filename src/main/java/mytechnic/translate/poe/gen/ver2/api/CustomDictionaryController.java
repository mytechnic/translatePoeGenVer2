package mytechnic.translate.poe.gen.ver2.api;

import mytechnic.translate.poe.gen.ver2.api.model.CustomDictionary;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dictionary")
public class CustomDictionaryController {

    private static final String DICTIONARY_FILE_PATH = "data/custom_dic.txt";

    // 사전 파일 읽기
    @GetMapping
    public ResponseEntity<CustomDictionary> getDictionary() throws IOException {
        Path path = Paths.get(DICTIONARY_FILE_PATH);
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }

        List<CustomDictionary.Entry> entries = Files.readAllLines(path).stream()
                .map(line -> {
                    String[] parts = line.split("\\|", 2);
                    if (parts.length == 2) {
                        CustomDictionary.Entry entry = new CustomDictionary.Entry();
                        entry.setEng(parts[0].toLowerCase());
                        entry.setKor(parts[1]);
                        return entry;
                    }
                    return null; // 잘못된 형식의 라인은 무시
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // key(eng) 기준으로 오름차순 정렬
        entries.sort(Comparator.comparing(CustomDictionary.Entry::getEng));

        CustomDictionary customDictionary = new CustomDictionary();
        customDictionary.setData(entries);

        return ResponseEntity.ok(customDictionary);
    }

    // 사전 파일 저장
    @PostMapping
    public ResponseEntity<Void> saveDictionary(@RequestBody CustomDictionary customDictionary) throws IOException {
        Path path = Paths.get(DICTIONARY_FILE_PATH);

        // CustomDictionary 데이터를 key|value 형식으로 변환
        List<String> processedLines = customDictionary.getData().stream()
                .map(entry -> entry.getEng().toLowerCase() + "|" + entry.getKor())
                .collect(Collectors.toList());

        Files.write(path, processedLines);
        return ResponseEntity.ok().build();
    }
}