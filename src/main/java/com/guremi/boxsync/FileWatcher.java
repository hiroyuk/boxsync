package com.guremi.boxsync;

import com.guremi.boxsync.utils.DigestUtils;
import com.typesafe.config.ConfigObject;
import java.io.IOException;
import java.nio.file.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileWatcher {
    private static final Logger LOG = LoggerFactory.getLogger(FileWatcher.class);

    public FileWatcher() {
    }

    public void register() throws IOException {
        ConfigObject co = App.config.getObject("syncpair");
        WatchService watcher = FileSystems.getDefault().newWatchService();
        co.forEach((k, v) -> {
            try {
                Path path = Paths.get(k);
                path.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
            } catch (IOException ex) {
                LOG.error(ex.getMessage(), ex);
            }
        });

        Runnable runner = () -> {
            try {
                WatchKey key = watcher.take();
                while(key != null) {
                    for (WatchEvent event : key.pollEvents()) {
						if (event.context() instanceof Path) {
							Path path = (Path) event.context();
							LOG.info("receiced {} event for file: {} ({})", event.kind(), path, path.toAbsolutePath());
                            if (Files.isReadable(path) && Files.isRegularFile(path)) {
    							String sha1Hash = DigestUtils.getDigest(path);
                            }
						}
                    }
                    key.reset();
                    key = watcher.take();
                }
            } catch (InterruptedException ex) {
                LOG.error(ex.getMessage(), ex);
            }
        };

        Thread thread = new Thread(runner, "FileWatcher");
        thread.start();

        try {
            thread.join();
        } catch (InterruptedException ex) {
        }
    }
}
