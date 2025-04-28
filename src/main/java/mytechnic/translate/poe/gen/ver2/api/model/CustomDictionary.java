package mytechnic.translate.poe.gen.ver2.api.model;

import lombok.Data;

import java.util.List;

@Data
public class CustomDictionary {
    private List<Entry> data;

    @Data
    public static class Entry {
        private String eng;
        private String kor;
    }
}