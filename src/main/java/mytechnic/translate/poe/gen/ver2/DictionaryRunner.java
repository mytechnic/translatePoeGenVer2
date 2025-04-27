package mytechnic.translate.poe.gen.ver2;

import lombok.extern.slf4j.Slf4j;
import mytechnic.translate.poe.gen.ver2.tool.*;

import java.util.List;

@Slf4j
public class DictionaryRunner {

    public static void main(String[] args) throws Exception {
        JsonDownloader.download("https://www.pathofexile.com/api/trade/data/items", "data/raw_eng_items.json");
        JsonDownloader.download("https://www.pathofexile.com/api/trade/data/stats", "data/raw_eng_stats.json");
        JsonDownloader.download("https://www.pathofexile.com/api/trade/data/static", "data/raw_eng_static.json");
        JsonDownloader.download("https://poe.game.daum.net/api/trade/data/items", "data/raw_kor_items.json");
        JsonDownloader.download("https://poe.game.daum.net/api/trade/data/stats", "data/raw_kor_stats.json");
        JsonDownloader.download("https://poe.game.daum.net/api/trade/data/static", "data/raw_kor_static.json");

        JsonStructureSampler.process("data/raw_eng_items.json", "data/sample_raw_eng_items.json");
        JsonStructureSampler.process("data/raw_eng_stats.json", "data/sample_raw_eng_stats.json");
        JsonStructureSampler.process("data/raw_eng_static.json", "data/sample_raw_eng_static.json");
        JsonStructureSampler.process("data/raw_kor_items.json", "data/sample_raw_kor_items.json");
        JsonStructureSampler.process("data/raw_kor_stats.json", "data/sample_raw_kor_stats.json");
        JsonStructureSampler.process("data/raw_kor_static.json", "data/sample_raw_kor_static.json");

        JsonStructureConverter.convertJsonStructure("data/raw_eng_items.json", "data/data_eng_items.json");
        JsonStructureConverter.convertJsonStructure("data/raw_eng_stats.json", "data/data_eng_stats.json");
        JsonStructureConverter.convertJsonStructure("data/raw_eng_static.json", "data/data_eng_static.json");
        JsonStructureConverter.convertJsonStructure("data/raw_kor_items.json", "data/data_kor_items.json");
        JsonStructureConverter.convertJsonStructure("data/raw_kor_stats.json", "data/data_kor_stats.json");
        JsonStructureConverter.convertJsonStructure("data/raw_kor_static.json", "data/data_kor_static.json");

        JsonStructureSampler.process("data/data_eng_items.json", "data/sample_data_eng_items.json");
        JsonStructureSampler.process("data/data_eng_stats.json", "data/sample_data_eng_stats.json");
        JsonStructureSampler.process("data/data_eng_static.json", "data/sample_data_eng_static.json");
        JsonStructureSampler.process("data/data_kor_items.json", "data/sample_data_kor_items.json");
        JsonStructureSampler.process("data/data_kor_stats.json", "data/sample_data_kor_stats.json");
        JsonStructureSampler.process("data/data_kor_static.json", "data/sample_data_kor_static.json");

        TranslationMapper.mapTranslation(
                "data/data_eng_items.json",
                "data/data_kor_items.json",
                "data/dic_items.json",
                "data/mismatch_items.json");

        TranslationMapper.mapTranslation(
                "data/data_eng_stats.json",
                "data/data_kor_stats.json",
                "data/dic_stats.json",
                "data/mismatch_stats.json");

        TranslationMapper.mapTranslation(
                "data/data_eng_static.json",
                "data/data_kor_static.json",
                "data/dic_static.json",
                "data/mismatch_static.json");

        JsonStructureSampler.process("data/mismatch_items.json", "data/sample_mismatch_items.json");
        JsonStructureSampler.process("data/mismatch_stats.json", "data/sample_mismatch_stats.json");
        JsonStructureSampler.process("data/mismatch_static.json", "data/sample_mismatch_static.json");


        JsonToPlainTextConverter.convert("data/dic_items.json", "data/dic_items.txt");
        JsonToPlainTextConverter.convert("data/dic_stats.json", "data/dic_stats.txt");
        JsonToPlainTextConverter.convert("data/dic_static.json", "data/dic_static.txt");

        TranslationDictionaryBuilder.buildFromFiles(
                List.of(
                        "data/dic_items.json",
                        "data/dic_stats.json",
                        "data/dic_static.json"
                ),
                "data/dic.json"
        );

        DictionaryMerger.process("data/custom_dic.txt", "data/dic.json");
        JsonDictionaryComparator.compareDictionariesFromFiles("data/dic-old.json", "data/dic.json");
    }
}
