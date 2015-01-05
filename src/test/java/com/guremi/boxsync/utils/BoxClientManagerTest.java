package com.guremi.boxsync.utils;

import com.box.boxjavalibv2.exceptions.AuthFatalFailureException;
import com.guremi.boxsync.store.BoxClientManager;
import org.junit.Test;

/**
 *
 * @author htaka
 */
public class BoxClientManagerTest {

    public BoxClientManagerTest() {
    }

    @Test
    public void testGetAuthenticatedClient() throws AuthFatalFailureException {
        BoxClientManager manager = new BoxClientManager();
        manager.getAuthenticatedClient();
    }

}
