package com.guremi.boxsync.utils;

import com.box.boxjavalibv2.dao.BoxFolder;
import com.box.boxjavalibv2.exceptions.AuthFatalFailureException;
import com.guremi.boxsync.App;
import com.guremi.boxsync.store.BoxClientManager;
import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import org.junit.Test;

/**
 *
 * @author htaka
 */
public class BoxAccessUtilsTest {
    BoxClientManager clientManager = new BoxClientManager();
    BoxAccessUtils boxAccessUtils ;

    public BoxAccessUtilsTest() {
        App.config = ConfigFactory.load("application.json");
        try {
            boxAccessUtils = new BoxAccessUtils(clientManager.getAuthenticatedClient());
        } catch (AuthFatalFailureException ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void getCacheKey() {
        Path path = Paths.get("data1", "data2");
        String name = boxAccessUtils.getCacheKey(path);

        assertThat(name, is("/data1/data2/"));
    }

    @Test
    public void testGetFolderId() throws IOException {
        String folderId = boxAccessUtils.getFolderId(Paths.get("test", "hoge", "hage"));
        assertThat(folderId, is(nullValue()));
    }

    @Test
    public void testCreateDir() throws Exception {
        try {
            String folderId = boxAccessUtils.createDir(Paths.get("test", "agg"));
            assertThat(folderId, is(notNullValue()));

            BoxFolder folder = clientManager.getAuthenticatedClient().getFoldersManager().getFolder(folderId, null);
            assertThat(folder.getName(), is("agg"));
            assertThat(folder.getParent().getName(), is("test"));
            assertThat(folder.getParent().getParent(), is(nullValue()));
        } finally {
            boxAccessUtils.deleteDir(Paths.get("test"));
        }

    }

    @Test
    public void testUpload() throws Exception {
    }

    @Test
    public void testDownload() {
    }

    @Test
    public void testCheckDigest() {
    }

}
