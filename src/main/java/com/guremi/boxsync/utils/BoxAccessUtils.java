package com.guremi.boxsync.utils;

import com.box.boxjavalibv2.BoxClient;
import com.box.boxjavalibv2.dao.BoxCollection;
import com.box.boxjavalibv2.dao.BoxFile;
import com.box.boxjavalibv2.dao.BoxFolder;
import com.box.boxjavalibv2.dao.BoxTypedObject;
import com.box.boxjavalibv2.exceptions.AuthFatalFailureException;
import com.box.boxjavalibv2.exceptions.BoxServerException;
import static com.box.boxjavalibv2.requests.requestobjects.BoxFolderDeleteRequestObject.deleteFolderRequestObject;
import com.box.boxjavalibv2.requests.requestobjects.BoxFolderRequestObject;
import com.box.restclientv2.exceptions.BoxRestException;
import com.box.restclientv2.requestsbase.BoxDefaultRequestObject;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BoxAccessUtils {
    private static final Logger LOG = LoggerFactory.getLogger(BoxAccessUtils.class);
    private final BoxClient client;

    private final Map<String, String> folderCache;

    public BoxAccessUtils(BoxClient client) {
        this.client = client;
        this.folderCache = Collections.synchronizedMap(new HashMap<>());
    }

    void fillFolderCache(String parentId, Path parent) throws IOException {
        try {
            BoxFolder folder = client.getFoldersManager().getFolder(parentId, null);
            List<BoxTypedObject> entries = folder.getItemCollection().getEntries();
            entries.stream()
                    .filter(o -> Objects.equals("folder", o.getType()))
                    .map(o -> BoxFolder.class.cast(o))
                    .forEach(t -> {
                        final String id = t.getId();
                        final Path currentPath = parent == null ? Paths.get(t.getName()) : parent.resolve(t.getName());
                        final String cacheKey = getCacheKey(currentPath);

                        if (!Objects.equals(id, folderCache.put(cacheKey, id))) {
                            LOG.debug("store cache. name: {}, id: {}", cacheKey, id);
                        }
                    });
        } catch (BoxRestException | BoxServerException | AuthFatalFailureException ex) {
            throw new IOException(ex);
        }
    }

    String getCacheKey(Path path) {
        StringJoiner sj = new StringJoiner("/");
        path.forEach(p -> {
            sj.add(p.toString());
        });
        return "/" + sj.toString() + "/";
    }

    BoxFile getFile(Path path) throws IOException {
        Path folderPath = path.getParent();
        String folderId = getFolderId(folderPath);

        BoxDefaultRequestObject requestObject = new BoxDefaultRequestObject();
        requestObject.getRequestExtras().addQueryParam("scope", "user_content");
        requestObject.getRequestExtras().addQueryParam("ancestor_folder_ids", folderId);
        requestObject.getRequestExtras().addQueryParam("type", "file");

        try {
            BoxCollection collection = client.getSearchManager().search(path.getFileName().toString(), requestObject);
            for (BoxTypedObject obj : collection.getEntries()) {
                BoxFile bf = (BoxFile)obj;
                if (Objects.equals(bf.getName(), path.getFileName().toString())) {
                    return bf;
                }
            }
        } catch (BoxRestException | BoxServerException ex) {
            throw new IOException(ex);
        } catch (AuthFatalFailureException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return null;
    }

    public String getFolderId(Path path) throws IOException {
        if (path == null) {
            return "0";
        }
        final String cacheKey = getCacheKey(path);
        if (!folderCache.containsKey(cacheKey)) {
            Path parent = path.getParent();
            String parentId = getFolderId(parent);
            if (parentId == null) {
                return null;
            }
            fillFolderCache(parentId, parent);
        }
        String result = folderCache.get(cacheKey);
        folderCache.put(cacheKey, result);
        return result;
    }

    public String createDir(Path remotePath) throws IOException {
        try {
            String parentFolderId = getFolderId(remotePath.getParent());
            if (parentFolderId == null) {
                parentFolderId = createDir(remotePath.getParent());
            }

            BoxFolderRequestObject requestObject = new BoxFolderRequestObject();
            requestObject.setName(remotePath.getFileName().toString());
            requestObject.setParent(parentFolderId);
            BoxFolder folder = client.getFoldersManager().createFolder(requestObject);
            folderCache.put(getCacheKey(remotePath), folder.getId());

            return folder.getId();
        } catch (AuthFatalFailureException | BoxRestException | BoxServerException ex) {
            throw new IOException(ex);
        }
    }

    public void deleteDir(Path remotePath) throws IOException {
        String targetId = getFolderId(remotePath);
        if (targetId == null) {
            return;
        }

        try {
            client.getFoldersManager().deleteFolder(targetId, deleteFolderRequestObject(true));
            folderCache.remove(getCacheKey(remotePath));
        } catch (BoxRestException | BoxServerException ex) {
            throw new IOException(ex);
        } catch (AuthFatalFailureException ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    public void upload(Path localFile, Path remoteRoot, Path remotePath) throws BoxRestException, BoxServerException {
    }

    public void download(String key, Path path) throws IOException {

    }

    public boolean checkDigest(Path filePath, String localDigest) throws IOException {
        Path folderPath = filePath.getParent();
        String folderId = getFolderId(folderPath);
        BoxFile file = getFile(filePath);

        if (file == null) {
            return false;
        }
        return Objects.equals(file.getSha1(), localDigest);
    }

}
