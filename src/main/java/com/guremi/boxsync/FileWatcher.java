package com.guremi.boxsync;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import java.io.IOException;
import java.nio.file.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileWatcher {
    private static final Logger LOG = LoggerFactory.getLogger(FileWatcher.class);
    private final Config config;

    public FileWatcher(Config config) {
        this.config = config;
    }

    public void register() throws IOException {
        ConfigObject co = config.getObject("syncpair");
        WatchService watcher = FileSystems.getDefault().newWatchService();
        co.forEach((k, v) -> {
            try {
                Path path = Paths.get(k);
                path.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
            } catch (IOException ex) {
                LOG.error(ex.getMessage(), ex);
            }
        });

        Runnable runner = () -> {
            try {
                WatchKey key = watcher.take();
                while(key != null) {
                    for (WatchEvent event : key.pollEvents()) {
                        LOG.info("receiced {} event for file: {}", event.kind(), event.context());
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
