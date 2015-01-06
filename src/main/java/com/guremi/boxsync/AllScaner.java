package com.guremi.boxsync;

import com.box.boxjavalibv2.exceptions.AuthFatalFailureException;
import com.guremi.boxsync.store.BoxClientManager;
import com.guremi.boxsync.store.BoxAccessService;
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
    private final ThreadLocal<BoxAccessService> boxAccessUtils;

    public AllScaner() throws AuthFatalFailureException {
        BoxClientManager manager = new BoxClientManager();
        boxAccessUtils = ThreadLocal.withInitial(() -> {
            try {
                return new BoxAccessService(manager.getAuthenticatedClient());
            } catch (AuthFatalFailureException ex) {
                LOG.error(ex.getMessage(), ex);
            }
            return null;
        });
    }

    public void scan() {
        ConfigObject co = App.config.getObject("syncpair");
        co.forEach((k, v) -> {
            try {
                Path path = Paths.get(k);
                Files.walk(path)
//                        .parallel()
                        .filter(p -> Files.isRegularFile(p))
                        .forEach(p -> {
                            Path relativepath = path.relativize(p);
                            LOG.info("starting scan, file: {}", relativepath.toString());
                            try {
                                Path remotePath = Paths.get((String)v.unwrapped()).resolve(relativepath);
                                LOG.info("remote path : {}", remotePath.toString());
                                boxAccessUtils.get().checkAndUpload(p, remotePath);
                            } catch (IOException ex) {
                                LOG.warn(ex.getMessage(), ex);
                            }
                        });
            } catch (IOException ex) {
                LOG.error(ex.getMessage(), ex);
            }
        });

    }
}
