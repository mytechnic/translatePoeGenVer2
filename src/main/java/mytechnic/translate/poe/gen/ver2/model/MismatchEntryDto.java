// MismatchEntryDto.java
package mytechnic.translate.poe.gen.ver2.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MismatchEntryDto {
    private String field;           // 필드명 (ex: name, text)
    private List<String> engTexts;  // 영어 값 리스트
    private List<String> korTexts;  // 한글 값 리스트
}
