// TranslationMapper.java
package mytechnic.translate.poe.gen.ver2.tool;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import mytechnic.translate.poe.gen.ver2.model.DataStructureDto;
import mytechnic.translate.poe.gen.ver2.model.DataStructureEntry;
import mytechnic.translate.poe.gen.ver2.model.MismatchLanguageEntry;
import mytechnic.translate.poe.gen.ver2.model.MismatchStructureDto;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class TranslationMapper {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public static void mapTranslation(String engPath, String korPath, String outputPath, String mismatchPath) {
        try {
            List<DataStructureDto> engList = objectMapper.readValue(new File(engPath), new TypeReference<>() {
            });
            List<DataStructureDto> korList = objectMapper.readValue(new File(korPath), new TypeReference<>() {
            });

            Map<String, String> translationMap = new LinkedHashMap<>();
            List<MismatchStructureDto> mismatchList = new ArrayList<>();

            Map<String, DataStructureDto> korMap = korList.stream()
                    .filter(k -> k.getId() != null)
                    .collect(Collectors.toMap(DataStructureDto::getId, k -> k, (a, b) -> a));

            for (DataStructureDto engData : engList) {
                if (engData.getId() == null) {
                    log.error("영어 DataStructureDto에 id 없음: label={}", engData.getLabel());
                    continue;
                }

                DataStructureDto korData = korMap.get(engData.getId());
                if (korData == null) {
                    log.error("id={} 에 매칭되는 한글 DataStructureDto 없음: label={}", engData.getId(), engData.getLabel());
                    continue;
                }

                processEntries(engData, korData, translationMap, mismatchList);
            }

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputPath), translationMap);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(mismatchPath), mismatchList);
        } catch (IOException e) {
            log.error("파일 처리 중 오류 발생", e);
        }
    }

    private static void processEntries(DataStructureDto engData, DataStructureDto korData,
                                       Map<String, String> translationMap, List<MismatchStructureDto> mismatchList) {
        DataStructureEntry engEntry = engData.getEntries();
        DataStructureEntry korEntry = korData.getEntries();

        if (engEntry == null || korEntry == null) {
            log.error("entries 없음: {}, return 수행", engData.getLabel());
            return;
        }

        Map<String, List<Integer>> engIdMap = buildIdMap(engEntry);
        Map<String, List<Integer>> korIdMap = buildIdMap(korEntry);

        if (!engIdMap.isEmpty()) {
            for (Map.Entry<String, List<Integer>> entry : engIdMap.entrySet()) {
                String id = entry.getKey();
                List<Integer> engIndexes = entry.getValue();
                List<Integer> korIndexes = korIdMap.getOrDefault(id, new ArrayList<>());

                int min = Math.min(engIndexes.size(), korIndexes.size());
                for (int j = 0; j < min; j++) {
                    matchByIndex(engEntry, korEntry, engIndexes.get(j), korIndexes.get(j), translationMap);
                }
                if (engIndexes.size() != korIndexes.size()) {
                    for (int j = min; j < engIndexes.size(); j++) {
                        addMismatchEntry(engData, engEntry, korEntry, engIndexes.get(j), -1, mismatchList);
                    }
                    for (int j = min; j < korIndexes.size(); j++) {
                        addMismatchEntry(engData, engEntry, korEntry, -1, korIndexes.get(j), mismatchList);
                    }
                    logMismatch(engData.getId(), "ID 중복 매칭 실패: " + id);
                }
            }
        } else {
            matchSequentially(engData, engEntry, korEntry, translationMap, mismatchList);
        }
    }

    private static Map<String, List<Integer>> buildIdMap(DataStructureEntry entry) {
        Map<String, List<Integer>> map = new HashMap<>();
        if (entry.getId() != null) {
            for (int i = 0; i < entry.getId().size(); i++) {
                String id = entry.getId().get(i);
                if (id != null) {
                    map.computeIfAbsent(id, k -> new ArrayList<>()).add(i);
                }
            }
        }
        return map;
    }

    private static void matchByIndex(DataStructureEntry engEntry, DataStructureEntry korEntry, int engIdx, int korIdx, Map<String, String> translationMap) {
        matchField(engEntry.getName(), korEntry.getName(), engIdx, korIdx, translationMap);
        matchField(engEntry.getText(), korEntry.getText(), engIdx, korIdx, translationMap);
        matchField(engEntry.getType(), korEntry.getType(), engIdx, korIdx, translationMap);
        matchOptions(engEntry.getText(), korEntry.getText(), engEntry.getOptions(), korEntry.getOptions(), engIdx, korIdx, translationMap);
    }

    private static void matchField(List<String> engList, List<String> korList, int engIdx, int korIdx, Map<String, String> translationMap) {
        if (engList != null && korList != null && engIdx < engList.size() && korIdx < korList.size()) {
            String eng = engList.get(engIdx);
            String kor = korList.get(korIdx);
            saveTranslationMap(eng, kor, translationMap);
        }
    }

    private static void matchOptions(List<String> engText, List<String> korText, List<List<String>> engOptions, List<List<String>> korOptions,
                                     int engIdx, int korIdx, Map<String, String> translationMap) {
        if (engText == null || korText == null || engOptions == null || korOptions == null) {
            return;
        }
        if (engIdx >= engText.size() || korIdx >= korText.size()) {
            return;
        }
        if (engIdx >= engOptions.size() || korIdx >= korOptions.size()) {
            return;
        }

        List<String> engOptionList = engOptions.get(engIdx);
        List<String> korOptionList = korOptions.get(korIdx);

        if (engOptionList == null || korOptionList == null) {
            return;
        }

        String engTemplate = engText.get(engIdx);
        String korTemplate = korText.get(korIdx);

        if (engOptionList.size() != korOptionList.size()) {
            return;
        }

        for (int i = 0; i < engOptionList.size(); i++) {
            String eng = engTemplate.replace("#", engOptionList.get(i));
            String kor = korTemplate.replace("#", korOptionList.get(i));
            saveTranslationMap(eng, kor, translationMap);
        }

        for (int i = 0; i < engOptionList.size(); i++) {
            String eng = engOptionList.get(i);
            String kor = korOptionList.get(i);
            saveTranslationMap(eng, kor, translationMap);
        }
    }

    private static void saveTranslationMap(String eng, String kor, Map<String, String> translationMap) {
        if (eng == null || kor == null || eng.isBlank() || kor.isBlank()) {
            return;
        }

        eng = eng.trim();
        kor = kor.trim();
        if (eng.equals(kor)) {
            return;
        }

        eng = eng.toLowerCase();
        translationMap.put(eng, kor);
    }

    private static void matchSequentially(DataStructureDto engData, DataStructureEntry engEntry, DataStructureEntry korEntry,
                                          Map<String, String> translationMap, List<MismatchStructureDto> mismatchList) {
        if (engEntry.getName() != null && korEntry.getName() != null) {
            matchList("name", engData, engEntry.getName(), korEntry.getName(), translationMap, mismatchList);
        }
        if (engEntry.getText() != null && korEntry.getText() != null) {
            matchList("text", engData, engEntry.getText(), korEntry.getText(), translationMap, mismatchList);
        }
        if (engEntry.getType() != null && korEntry.getType() != null) {
            matchList("type", engData, engEntry.getType(), korEntry.getType(), translationMap, mismatchList);
        }
    }

    private static void matchList(String name, DataStructureDto engData, List<String> engList, List<String> korList,
                                  Map<String, String> translationMap, List<MismatchStructureDto> mismatchList) {

        if (engList.size() != korList.size()) {
            logMismatch(engData.getId(), "순서 매칭 실패");
            addMismatch(name, engData, engList, korList, mismatchList);
            return;
        }

        int size = engList.size();
        for (int i = 0; i < size; i++) {
            String eng = engList.get(i);
            String kor = korList.get(i);
            saveTranslationMap(eng, kor, translationMap);
        }
    }

    private static void addMismatch(String name, DataStructureDto engData, List<String> engList, List<String> korList,
                                    List<MismatchStructureDto> mismatchList) {

        int index = IntStream.range(0, mismatchList.size())
                .filter(i -> engData.getId().equals(mismatchList.get(i).getId()))
                .findFirst()
                .orElse(-1);

        if (index >= 0) {
            if ("name".equals(name)) {
                mismatchList.get(index).getEntries().getEngEntry().setName(engList);
                mismatchList.get(index).getEntries().getKorEntry().setName(korList);
            } else if ("text".equals(name)) {
                mismatchList.get(index).getEntries().getEngEntry().setText(engList);
                mismatchList.get(index).getEntries().getKorEntry().setText(korList);
            } else if ("type".equals(name)) {
                mismatchList.get(index).getEntries().getEngEntry().setType(engList);
                mismatchList.get(index).getEntries().getKorEntry().setType(korList);
            }
            return;
        }


        DataStructureEntry engEntry = new DataStructureEntry();
        engEntry.setName(engList);

        DataStructureEntry korEntry = new DataStructureEntry();
        korEntry.setName(korList);

        MismatchLanguageEntry mismatchEntry = new MismatchLanguageEntry();
        mismatchEntry.setEngEntry(engEntry);
        mismatchEntry.setKorEntry(korEntry);

        MismatchStructureDto mismatch = new MismatchStructureDto();
        mismatch.setId(engData.getId());
        mismatch.setLabel(engData.getLabel());
        mismatch.setReason("entries 매칭 실패");
        mismatch.setEntries(mismatchEntry);

        mismatchList.add(mismatch);
    }

    private static void addMismatchEntry(DataStructureDto engData, DataStructureEntry engEntry, DataStructureEntry korEntry,
                                         int engIdx, int korIdx, List<MismatchStructureDto> mismatchList) {
        DataStructureEntry mismatchEngEntry = new DataStructureEntry();
        DataStructureEntry mismatchKorEntry = new DataStructureEntry();

        if (engIdx >= 0) {
            mismatchEngEntry.setName(singletonListSafe(engEntry.getName(), engIdx));
            mismatchEngEntry.setText(singletonListSafe(engEntry.getText(), engIdx));
            mismatchEngEntry.setType(singletonListSafe(engEntry.getType(), engIdx));
        }
        if (korIdx >= 0) {
            mismatchKorEntry.setName(singletonListSafe(korEntry.getName(), korIdx));
            mismatchKorEntry.setText(singletonListSafe(korEntry.getText(), korIdx));
            mismatchKorEntry.setType(singletonListSafe(korEntry.getType(), korIdx));
        }

        MismatchLanguageEntry mismatchLanguageEntry = new MismatchLanguageEntry();
        mismatchLanguageEntry.setEngEntry(mismatchEngEntry);
        mismatchLanguageEntry.setKorEntry(mismatchKorEntry);

        MismatchStructureDto mismatch = new MismatchStructureDto();
        mismatch.setId(engData.getId());
        mismatch.setLabel(engData.getLabel());
        mismatch.setReason("ID 매칭 실패");
        mismatch.setEntries(mismatchLanguageEntry);

        mismatchList.add(mismatch);
    }

    private static List<String> singletonListSafe(List<String> list, int idx) {
        if (list == null || idx >= list.size()) return null;
        return Collections.singletonList(list.get(idx));
    }

    private static void logMismatch(String id, String reason) {
        log.error("[Mismatch] id={} reason={}", id, reason);
    }
}
