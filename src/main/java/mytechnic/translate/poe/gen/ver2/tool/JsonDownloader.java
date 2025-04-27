package mytechnic.translate.poe.gen.ver2.tool;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

@Slf4j
public class JsonDownloader {

    public static void download(String sourceJsonUrl, String downloadedJsonPath) throws Exception {

        File downloadedFile = new File(downloadedJsonPath);

        if (downloadedFile.exists()) {
            log.info("기존 파일이 이미 존재하여 다운로드 생략: {}", downloadedJsonPath);
            return;
        }

        File parentDir = downloadedFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            if (created) {
                log.info("디렉토리 생성됨: {}", parentDir.getAbsolutePath());
            }
        }

        try (InputStream in = new URL(sourceJsonUrl).openStream();
             FileOutputStream out = new FileOutputStream(downloadedFile)) {
            in.transferTo(out);
            log.info("파일 다운로드 완료: {}", downloadedJsonPath);
        }
    }
}
