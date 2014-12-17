package com.guremi.boxsync;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import java.util.concurrent.ForkJoinPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author htaka
 */
public class App {
    private static final Logger LOG = LoggerFactory.getLogger(App.class);
    public static final ForkJoinPool POOL = ForkJoinPool.commonPool();
    
    public static Config config;
	

    public static void main(String[] args) {
        App.config = ConfigFactory.load("application.json");

        try {
            new AllScaner().scan();
            new FileWatcher().register();
        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }
}
