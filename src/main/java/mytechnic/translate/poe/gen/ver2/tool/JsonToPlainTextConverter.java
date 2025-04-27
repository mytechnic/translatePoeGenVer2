package mytechnic.translate.poe.gen.ver2.tool;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class JsonToPlainTextConverter {

    public static void convert(String inputPath, String outputPath) throws IOException {
        File inputJson = new File(inputPath);
        File outputFile = new File(outputPath);

        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> map = mapper.readValue(inputJson, Map.class);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                writer.write(entry.getKey() + "|" + entry.getValue());
                writer.newLine();
            }
        }
    }
}