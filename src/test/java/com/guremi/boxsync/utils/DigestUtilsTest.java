/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.guremi.boxsync.utils;

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
		Path path = Paths.get("src", "test", "resources", "testfile", "LICENSE");
		String digest = DigestUtils.getDigest(path);
		
		assertThat(digest, is("ce6b4f89ccc3bc1da856e9631f8a0a55757fb6d3"));
	}
	
}
