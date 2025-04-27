package mytechnic.translate.poe.gen.ver2.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import mytechnic.translate.poe.gen.ver2.api.model.SaveDictionaryRequest;
import mytechnic.translate.poe.gen.ver2.model.DataStructureEntry;
import mytechnic.translate.poe.gen.ver2.model.MismatchStructureDto;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class MismatchService {
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    // 미스매치 파일 목록 읽기
    public List<String> loadMismatchFileList() {
        try (Stream<Path> paths = Files.list(Paths.get("data"))) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(row -> !loadMismatchFile(row.getFileName().toString()).isEmpty())
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(f -> f.startsWith("mismatch"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("파일 목록 읽기 실패", e);
        }
    }

    // 미스매치 파일 읽기
    public List<MismatchStructureDto> loadMismatchFile(String mismatchFileName) {
        File file = new File("data/" + mismatchFileName);

        if (!file.exists()) {
            return Collections.emptyList();
        }

        List<MismatchStructureDto> mismatchStructureList = null;
        try {
            mismatchStructureList = objectMapper.readValue(file,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, MismatchStructureDto.class));
        } catch (IOException e) {
            return Collections.emptyList();
        }

        // 중복 코드 리팩토링: 데이터 정리 함수 호출
        mismatchStructureList.forEach(this::cleanEntries);

        mismatchStructureList = mismatchStructureList.stream().filter(row -> row.getEntries() != null)
                .toList();

        return mismatchStructureList;
    }

    // 엔트리 데이터 정리
    private void cleanEntries(MismatchStructureDto mismatchStructureDto) {
        // 영어와 한국어 엔트리 각각 정리
        mismatchStructureDto.getEntries().setEngEntry(cleanEntry(mismatchStructureDto.getEntries().getEngEntry()));
        mismatchStructureDto.getEntries().setKorEntry(cleanEntry(mismatchStructureDto.getEntries().getKorEntry()));

        if (mismatchStructureDto.getEntries().getEngEntry() == null
                && mismatchStructureDto.getEntries().getKorEntry() == null) {
            mismatchStructureDto.setEntries(null);
        }
    }

    // 개별 엔트리 데이터 정리
    private DataStructureEntry cleanEntry(DataStructureEntry entry) {
        if (entry != null) {
            if (entry.getName() != null) {
                entry.setName(filterEmptyValues(entry.getName()));
            }
            if (entry.getText() != null) {
                entry.setText(filterEmptyValues(entry.getText()));
            }
            if (entry.getType() != null) {
                entry.setType(filterEmptyValues(entry.getType()));
            }

            if (ObjectUtils.isEmpty(entry.getName())
                    && ObjectUtils.isEmpty(entry.getText())
                    && ObjectUtils.isEmpty(entry.getType())) {
                return null;
            }
        }
        return entry;
    }

    // 빈 값 제거
    private List<String> filterEmptyValues(List<String> list) {
        return list.stream()
                .filter(s -> !ObjectUtils.isEmpty(s))
                .collect(Collectors.toList());
    }

    // 사전 파일 읽기
    public List<MismatchStructureDto> getMismatchFile(String mismatchFileName) throws IOException {
        Path path = Paths.get("data", mismatchFileName);
        return Files.exists(path) ? loadMismatchFile(mismatchFileName) : Collections.emptyList();
    }

    // 저장 처리
    public void saveDictionary(SaveDictionaryRequest request) {
        String mismatchFileName = request.getMismatchFileName();
        String dictionaryFileName = request.getDictionaryFileName();
        String fieldToNull = request.getField(); // 클라이언트에서 null 처리할 필드 가져오기

        File mismatchFile = new File("data", mismatchFileName);
        if (!mismatchFile.exists()) {
            throw new RuntimeException("미스매치 파일을 찾을 수 없습니다.");
        }

        // 사전 데이터 파일이 존재하면 읽어들임
        Map<String, String> dictionaryMap = loadDictionaryFile(dictionaryFileName);

        try {
            // 미스매치 데이터 처리
            MismatchStructureDto[] mismatchData = objectMapper.readValue(mismatchFile, MismatchStructureDto[].class);

            // 미스매치 항목의 특정 필드를 null 처리
            processFieldNull(mismatchData, fieldToNull);

            // 클라이언트에서 전달된 mappings 데이터를 기존 사전 데이터에 추가
            addMappingsToDictionary(dictionaryMap, request.getMappings());

            // 파일 덮어쓰기
            writeDictionaryFile(dictionaryFileName, dictionaryMap);
            writeMismatchFile(mismatchFileName, mismatchData);

        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패", e);
        }
    }

    // 사전 파일 읽기
    private Map<String, String> loadDictionaryFile(String dictionaryFileName) {
        File dictFile = new File(dictionaryFileName);
        if (!dictFile.exists()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(dictFile, new TypeReference<Map<String, String>>() {
            });
        } catch (IOException e) {
            throw new RuntimeException("사전 데이터 파일 읽기 실패", e);
        }
    }

    // 필드 null 처리
    private void processFieldNull(MismatchStructureDto[] mismatchData, String fieldToNull) {
        for (MismatchStructureDto mismatch : mismatchData) {
            switch (fieldToNull) {
                case "name":
                    setFieldsNull(mismatch, "name");
                    break;
                case "text":
                    setFieldsNull(mismatch, "text");
                    break;
                case "type":
                    setFieldsNull(mismatch, "type");
                    break;
            }
        }
    }

    // 특정 필드를 null 처리
    private void setFieldsNull(MismatchStructureDto mismatch, String field) {
        if ("name".equals(field)) {
            mismatch.getEntries().getEngEntry().setName(null);
            mismatch.getEntries().getKorEntry().setName(null);
        } else if ("text".equals(field)) {
            mismatch.getEntries().getEngEntry().setText(null);
            mismatch.getEntries().getKorEntry().setText(null);
        } else if ("type".equals(field)) {
            mismatch.getEntries().getEngEntry().setType(null);
            mismatch.getEntries().getKorEntry().setType(null);
        }
    }

    // 사전 데이터 덧붙이기
    private void addMappingsToDictionary(Map<String, String> existingMap, Map<String, String> mappings) {
        mappings.forEach((key, value) -> existingMap.put(key.toLowerCase(), value));  // 영어를 소문자로 변환하여 저장
    }

    // 미스매치 파일 덮어쓰기
    private void writeMismatchFile(String mismatchFileName, MismatchStructureDto[] mismatchData) throws IOException {
        objectMapper.writeValue(new File("data", mismatchFileName), mismatchData);
    }

    // 사전 파일 덮어쓰기
    private void writeDictionaryFile(String dictionaryFileName, Map<String, String> dictionaryMap) throws IOException {
        objectMapper.writeValue(new File("data", dictionaryFileName), dictionaryMap);
    }
}
