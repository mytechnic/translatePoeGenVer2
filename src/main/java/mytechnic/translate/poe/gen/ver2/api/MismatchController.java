package mytechnic.translate.poe.gen.ver2.api;

import mytechnic.translate.poe.gen.ver2.api.model.SaveDictionaryRequest;
import mytechnic.translate.poe.gen.ver2.model.MismatchStructureDto;
import mytechnic.translate.poe.gen.ver2.service.MismatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/mismatch")
public class MismatchController {

    private final MismatchService mismatchService;

    public MismatchController(MismatchService mismatchService) {
        this.mismatchService = mismatchService;
    }

    // 미스매치 파일 읽기 (load)
    @GetMapping("/load")
    public ResponseEntity<List<String>> loadMismatchFile() {
        List<String> files = mismatchService.loadMismatchFileList();
        return ResponseEntity.ok(files);
    }

    // 사전 파일 읽기 (get)
    @GetMapping("/get")
    public ResponseEntity<List<MismatchStructureDto>> getDictionaryFile(@RequestParam String fileName) throws IOException {
        return ResponseEntity.ok(mismatchService.getMismatchFile(fileName));
    }

    // 저장 처리 (save)
    @PostMapping("/save")
    public void saveMismatch(@RequestBody SaveDictionaryRequest request) {
        mismatchService.saveDictionary(request);
    }
}
