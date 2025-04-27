package mytechnic.translate.poe.gen.ver2.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class DataStructureEntry {
    private List<String> id;
    private List<String> name;
    private List<String> text;
    private List<String> type;
    private List<List<String>> options;
}