package com.guremi.boxsync.utils;

import com.guremi.boxsync.FileWatcher;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author htaka
 */
public class DigestUtilsTest {

    public DigestUtilsTest() {
    }

    @Test
    public void testGetDigest() throws Exception {
        Config config = ConfigFactory.load("application.json");
        FileWatcher.config = config;
        Path path = Paths.get("src", "test", "resources", "testfile", "LICENSE");
        String digest = DigestUtils.getDigest(path);

        assertThat(digest, is("ce6b4f89ccc3bc1da856e9631f8a0a55757fb6d3"));
    }

}
