/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.guremi.boxsync.store;

import com.guremi.boxsync.App;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

/**
 *
 * @author htaka
 */
public class DigestServiceTest {

    public DigestServiceTest() {
    }

    @Test
    public void testGetDigest() throws Exception {
        Config config = ConfigFactory.load("application.json");
        DigestService digestService = new DigestService();

        App.config = config;
        Path path = Paths.get("src", "test", "resources", "testfile", "LICENSE");
        String digest = digestService.getDigest(path);

        assertThat(digest, is("ce6b4f89ccc3bc1da856e9631f8a0a55757fb6d3"));
    }

    @Test
    public void testStoreDigest() {
    }

    @Test
    public void testClearDigest() {
    }

}
