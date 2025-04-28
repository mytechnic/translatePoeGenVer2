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
    private static final String DATA_DIRECTORY = "data"; // 미스매치 및 사전 파일 디렉토리
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    // 데이터 디렉토리 내 파일 목록 읽기
    public List<String> loadFileList(String prefix) {
        try (Stream<Path> paths = Files.list(Paths.get(DATA_DIRECTORY))) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(f -> f.startsWith(prefix))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("파일 목록 읽기 실패", e);
        }
    }

    // 미스매치 파일 목록 읽기
    public List<String> loadMismatchFileList() {
        return loadFileList("mismatch");
    }

    // 사전 파일 목록 읽기
    public List<String> loadDictionaryFileList() {
        return loadFileList("dictionary");
    }

    // 미스매치 파일 읽기
    public List<MismatchStructureDto> loadMismatchFile(String mismatchFileName) {
        File file = new File(DATA_DIRECTORY + "/" + mismatchFileName);

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

        mismatchStructureList.forEach(this::cleanEntries);

        mismatchStructureList = mismatchStructureList.stream().filter(row -> row.getEntries() != null)
                .toList();

        return mismatchStructureList;
    }

    // 사전 파일 읽기
    private Map<String, String> loadDictionaryFile(String dictionaryFileName) {
        File dictFile = new File(DATA_DIRECTORY, dictionaryFileName);
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

    // 엔트리 데이터 정리
    private void cleanEntries(MismatchStructureDto mismatchStructureDto) {
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

    // 빈 값 필터링
    private List<String> filterEmptyValues(List<String> list) {
        return list.stream()
                .filter(s -> !ObjectUtils.isEmpty(s))
                .collect(Collectors.toList());
    }

    // 사전 데이터 저장
    public void saveDictionary(SaveDictionaryRequest request) {
        String mismatchFileName = request.getMismatchFileName();
        String dictionaryFileName = request.getDictionaryFileName();
        String fieldToNull = request.getField();

        File mismatchFile = new File(DATA_DIRECTORY, mismatchFileName);
        if (!mismatchFile.exists()) {
            throw new RuntimeException("미스매치 파일을 찾을 수 없습니다.");
        }

        Map<String, String> dictionaryMap = loadDictionaryFile(dictionaryFileName);

        try {
            MismatchStructureDto[] mismatchData = objectMapper.readValue(mismatchFile, MismatchStructureDto[].class);

            processFieldNull(mismatchData, fieldToNull);

            addMappingsToDictionary(dictionaryMap, request.getMappings());

            writeDictionaryFile(dictionaryFileName, dictionaryMap);
            writeMismatchFile(mismatchFileName, mismatchData);

        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패", e);
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

    // 사전 데이터 병합
    private void addMappingsToDictionary(Map<String, String> existingMap, Map<String, String> mappings) {
        mappings.forEach((key, value) -> existingMap.put(key.toLowerCase(), value));
    }

    // 미스매치 파일 저장
    private void writeMismatchFile(String mismatchFileName, MismatchStructureDto[] mismatchData) throws IOException {
        objectMapper.writeValue(new File(DATA_DIRECTORY, mismatchFileName), mismatchData);
    }

    // 사전 파일 저장
    private void writeDictionaryFile(String dictionaryFileName, Map<String, String> dictionaryMap) throws IOException {
        objectMapper.writeValue(new File(DATA_DIRECTORY, dictionaryFileName), dictionaryMap);
    }
}