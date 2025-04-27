// SaveDictionaryRequest.java
package mytechnic.translate.poe.gen.ver2.api.model;

import lombok.Data;

import java.util.Map;

@Data
public class SaveDictionaryRequest {
    private String mismatchFileName; // 미스매치 파일 이름
    private String dictionaryFileName;     // 저장할 파일 이름
    private String field;            // null 처리할 필드 (예: "name", "text", "type")
    private Map<String, String> mappings; // 클라이언트에서 보내는 영어 -> 한글 매핑 데이터
}
