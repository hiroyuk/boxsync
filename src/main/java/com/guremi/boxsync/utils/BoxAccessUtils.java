package com.guremi.boxsync.utils;

import com.box.boxjavalibv2.BoxClient;
import com.box.boxjavalibv2.dao.BoxFile;
import com.box.boxjavalibv2.exceptions.AuthFatalFailureException;
import com.box.boxjavalibv2.exceptions.BoxServerException;
import com.box.boxjavalibv2.resourcemanagers.IBoxFilesManager;
import com.box.restclientv2.exceptions.BoxRestException;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BoxAccessUtils {
    private static final Logger LOG = LoggerFactory.getLogger(BoxAccessUtils.class);
    private final BoxClient client;


    public BoxAccessUtils(BoxClient client) {
        this.client = client;
    }

    public void upload(Path path, String key) {
        
    }

    public void download(String key, Path path) {
        
    }

    public boolean checkDigest(String key, String localDigest) {
        try {
            IBoxFilesManager manager = client.getFilesManager();
            BoxFile file = manager.getFile(key, null);
            
            return false;
        } catch (BoxRestException | BoxServerException | AuthFatalFailureException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return false;
    }

}
