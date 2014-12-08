package com.guremi.boxsync.utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.xml.bind.DatatypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author htaka
 */
public class DigestUtils {
    private static final Logger LOG = LoggerFactory.getLogger(DigestUtils.class);

    public static String getDigest(Path path) throws IOException {
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
            return DatatypeConverter.printHexBinary(md.digest()).toLowerCase();
        } catch (NoSuchAlgorithmException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return null;
    }

    private DigestUtils() {
    }
}
