package com.guremi.boxsync;

import com.guremi.boxsync.utils.DigestUtils;
import com.typesafe.config.ConfigObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author htaka
 */
public class AllScaner {

    private static final Logger LOG = LoggerFactory.getLogger(AllScaner.class);

    public void scan() {
        ConfigObject co = App.config.getObject("syncpair");
        co.forEach((k, v) -> {
            try {
                Path path = Paths.get(k);
                Files.walk(path)
                        .parallel()
                        .filter(p -> Files.isRegularFile(p))
                        .forEach(p -> {
                            Path relativepath = path.relativize(p);
                            LOG.info("starting scan, file: {}", relativepath.toString());
                            String digest = DigestUtils.getDigest(p);
                        });
            } catch (IOException ex) {
                LOG.error(ex.getMessage(), ex);
            }
        });

    }
}
