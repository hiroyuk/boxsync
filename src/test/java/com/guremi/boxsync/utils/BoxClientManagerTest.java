/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.guremi.boxsync.utils;

import org.junit.Test;

/**
 *
 * @author htaka
 */
public class BoxClientManagerTest {

    public BoxClientManagerTest() {
    }

    @Test
    public void testGetAuthenticatedClient() {
        BoxClientManager manager = new BoxClientManager();
        manager.getAuthenticatedClient();
    }

}
