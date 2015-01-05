package com.guremi.boxsync.utils;

import com.box.boxjavalibv2.BoxClient;
import com.box.boxjavalibv2.dao.BoxCollection;
import com.box.boxjavalibv2.dao.BoxFile;
import com.box.boxjavalibv2.dao.BoxFolder;
import com.box.boxjavalibv2.dao.BoxTypedObject;
import com.box.boxjavalibv2.exceptions.AuthFatalFailureException;
import com.box.boxjavalibv2.exceptions.BoxJSONException;
import com.box.boxjavalibv2.exceptions.BoxServerException;
import com.box.boxjavalibv2.requests.requestobjects.BoxFolderDeleteRequestObject;
import com.box.boxjavalibv2.requests.requestobjects.BoxFolderRequestObject;
import com.box.restclientv2.exceptions.BoxRestException;
import com.box.restclientv2.requestsbase.BoxDefaultRequestObject;
import com.box.restclientv2.requestsbase.BoxFileUploadRequestObject;
import com.guremi.boxsync.store.DigestService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BoxAccessUtils {
    private static final Logger LOG = LoggerFactory.getLogger(BoxAccessUtils.class);
    private final BoxClient client;

    private final Map<String, String> folderCache;
    private final DigestService digestService;

    public BoxAccessUtils(BoxClient client) {
        this.client = client;
        this.folderCache = Collections.synchronizedMap(new HashMap<>());
        this.digestService = new DigestService();
    }

    /**
     * parentId以下のディレクトリ情報をキャッシュする
     * @param parentId
     * @param parent
     * @throws IOException
     */
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

    /**
     * キャッシュのキーを作成する
     * @param path
     * @return
     */
    String getCacheKey(Path path) {
        StringJoiner sj = new StringJoiner("/");
        path.forEach(p -> {
            sj.add(p.toString());
        });
        return "/" + sj.toString() + "/";
    }

    /**
     * pathファイルの情報を取得する
     * @param path
     * @return
     * @throws IOException
     */
    BoxFile getFile(Path path) throws IOException {
        Path folderPath = path.getParent();
        String folderId = getFolderId(folderPath);
        if (folderId == null) {
            return null;
        }

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
        } catch (AuthFatalFailureException | BoxRestException | BoxServerException ex) {
            throw new IOException(ex);
        }
        return null;
    }

    /**
     * pathのフォルダIDを取得する
     * @param path
     * @return
     * @throws IOException
     */
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

    /**
     * ディレクトリを作成する
     * @param remotePath
     * @return
     * @throws IOException
     */
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

    /**
     * remotePathフォルダを再帰的に削除する。
     * @param remotePath
     * @throws IOException
     */
    public void deleteDir(Path remotePath) throws IOException {
        String targetId = getFolderId(remotePath);
        if (targetId == null) {
            return;
        }

        try {
            client.getFoldersManager().deleteFolder(targetId, BoxFolderDeleteRequestObject.deleteFolderRequestObject(true));
            folderCache.remove(getCacheKey(remotePath));
        } catch (BoxRestException | BoxServerException | AuthFatalFailureException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * localFileをアップロードしてremotePathを作成する
     * @param localFile
     * @param remotePath
     * @throws IOException
     */
    public void checkAndUpload(Path localFile, Path remotePath) throws IOException {
        String localDigest = DigestUtils.getDigest(localFile);
        if (localDigest != null && checkDigest(remotePath, localDigest)) {
            LOG.debug("sha1 agree with remote. skip.");
            return;
        }

        deleteIfExist(remotePath);

        Path remoteFolder = remotePath.getParent();
        String folderId = getFolderId(remoteFolder);
        if (folderId == null) {
            folderId = createDir(remoteFolder);
        }
        try (InputStream is = Files.newInputStream(localFile)) {
            BoxFileUploadRequestObject requestObject = BoxFileUploadRequestObject.uploadFileRequestObject(folderId, remotePath.getFileName().toString(), is);
            client.getFilesManager().uploadFile(requestObject);
        } catch (BoxRestException | BoxServerException | AuthFatalFailureException | BoxJSONException | InterruptedException ex) {
            throw new IOException(ex);
        }
    }

    public void deleteIfExist(Path remotePath) throws IOException {
        BoxFile file = getFile(remotePath);
        if (file == null) {
            return;
        }
        BoxDefaultRequestObject object = new BoxDefaultRequestObject();
        try {
            client.getFilesManager().deleteFile(file.getId(), object);
        } catch (BoxRestException | BoxServerException | AuthFatalFailureException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * remotePathをダウンロードしてlocalPathファイルを作成する
     * @param remotePath
     * @param localPath
     * @throws IOException
     */
    public void download(Path remotePath, Path localPath) throws IOException {
        BoxFile boxFile = getFile(remotePath);
        if (boxFile == null) {
            throw new IOException("remote file not found.");
        }

        BoxDefaultRequestObject requestObject = new BoxDefaultRequestObject();
        try (InputStream is = client.getFilesManager().downloadFile(boxFile.getId(), requestObject)) {
            Files.copy(is, localPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (BoxRestException | BoxServerException | AuthFatalFailureException ex) {
            throw new IOException(ex);
        }
    }

    public boolean checkDigest(Path filePath, String localDigest) throws IOException {
        BoxFile file = getFile(filePath);

        if (file == null) {
            return false;
        }
        return Objects.equals(file.getSha1(), localDigest);
    }

}
