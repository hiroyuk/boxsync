package com.guremi.boxsync.utils;

import com.guremi.boxsync.store.DigestService;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import javax.xml.bind.DatatypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author htaka
 */
public class DigestUtils {
    private static final Logger LOG = LoggerFactory.getLogger(DigestUtils.class);
    private static final DigestService digestService = new DigestService();

    public static String getDigest(Path path) throws IOException {
        Optional<String> digestValue = digestService.getCachedDigest(path);

        if (digestValue.isPresent()) {
            LOG.debug("cached digest: {}", digestValue.get());
            return digestValue.get();
        } else {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA");
                ByteBuffer bb = ByteBuffer.allocate(10240);
                try (SeekableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.READ)) {
                    while (true) {
                        bb.clear();
                        int size = channel.read(bb);
                        if (size == -1) {
                            break;
                        }
                        bb.flip();
                        md.update(bb);
                    }
                }
                String digest = DatatypeConverter.printHexBinary(md.digest()).toLowerCase();
                digestService.storeDigest(path, digest);

                return digest;
            } catch (NoSuchAlgorithmException ex) {
                throw new IOException(ex);
            }
        }
    }

    private DigestUtils() {
    }
}
