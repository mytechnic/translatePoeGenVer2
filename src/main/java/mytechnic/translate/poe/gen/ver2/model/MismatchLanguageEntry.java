package mytechnic.translate.poe.gen.ver2.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class MismatchLanguageEntry {
    private DataStructureEntry engEntry;
    private DataStructureEntry korEntry;
}