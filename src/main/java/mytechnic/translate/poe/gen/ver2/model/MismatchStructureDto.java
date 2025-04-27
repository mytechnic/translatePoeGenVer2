package mytechnic.translate.poe.gen.ver2.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class MismatchStructureDto {
    private String id;
    private String label;
    private String reason;
    private MismatchLanguageEntry entries;
}