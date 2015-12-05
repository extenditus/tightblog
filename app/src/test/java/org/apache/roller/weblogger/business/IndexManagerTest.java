/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 *
 * Source file modified from the original ASF source; all changes made
 * are also under Apache License.
 */
package org.apache.roller.weblogger.business;

import java.sql.Timestamp;
import org.apache.commons.lang.time.DateUtils;
import org.apache.roller.weblogger.WebloggerCommon;
import org.apache.roller.weblogger.WebloggerTest;
import org.apache.roller.weblogger.business.search.operations.AddEntryOperation;
import org.apache.roller.weblogger.business.search.operations.SearchOperation;
import org.apache.roller.weblogger.business.search.IndexManager;
import org.apache.roller.weblogger.pojos.User;
import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.apache.roller.weblogger.pojos.WeblogEntry.PubStatus;
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.pojos.WeblogCategory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Resource;

import static org.junit.Assert.*;


/**
 * Test Search Manager business layer operations.
 */
public class IndexManagerTest extends WebloggerTest {
    public static Log log = LogFactory.getLog(IndexManagerTest.class);

    private User testUser = null;
    private Weblog testWeblog = null;

    @Resource
    private IndexManager indexManager;

    public void setIndexManager(IndexManager indexManager) {
        this.indexManager = indexManager;
    }

    /**
     * All tests in this suite require a user and a weblog.
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();
        
        try {
            testUser = setupUser("entryTestUser");
            testWeblog = setupWeblog("entryTestWeblog", testUser);
            endSession(true);
            assertEquals(1, weblogManager.getWeblogCount());
        } catch (Exception ex) {
            log.error("ERROR in test setup", ex);
            throw new Exception("Test setup failed", ex);
        }
    }

    @After
    public void tearDown() throws Exception {
        try {
            teardownWeblog(testWeblog.getId());
            teardownUser(testUser.getUserName());
            endSession(true);
        } catch (Exception ex) {
            log.error("ERROR in test teardown", ex);
            throw new Exception("Test teardown failed", ex);
        }
    }

    @Test
    public void testSearch() throws Exception {
        WeblogEntry wd1 = new WeblogEntry();
        wd1.setId(WebloggerCommon.generateUUID());
        wd1.setTitle("The Tholian Web");
        wd1.setText(
         "When the Enterprise attempts to ascertain the fate of the  "
        +"U.S.S. Defiant which vanished 3 weeks ago, the warp engines  "
        +"begin to lose power, and Spock reports strange sensor readings.");
        wd1.setAnchor("dummy1");
        wd1.setCreatorUserName(testUser.getUserName());
        wd1.setStatus(PubStatus.PUBLISHED);
        wd1.setUpdateTime(new Timestamp(System.currentTimeMillis()));
        wd1.setPubTime(new Timestamp(System.currentTimeMillis()));
        wd1.setWeblog(getManagedWeblog(testWeblog));

        WeblogCategory cat = weblogManager.getWeblogCategory(testWeblog.getWeblogCategory("General").getId());
        wd1.setCategory(cat);

        weblogEntryManager.saveWeblogEntry(wd1);
        endSession(true);
        wd1 = getManagedWeblogEntry(wd1);

        indexManager.executeIndexOperationNow(
                new AddEntryOperation(weblogEntryManager, indexManager, wd1));

        WeblogEntry wd2 = new WeblogEntry();
        wd2.setId(WebloggerCommon.generateUUID());
        wd2.setTitle("A Piece of the Action");
        wd2.setText(
          "The crew of the Enterprise attempts to make contact with "
          +"the inhabitants of planet Sigma Iotia II, and Uhura puts Kirk "
          +"in communication with Boss Oxmyx.");
        wd2.setAnchor("dummy2");
        wd2.setStatus(PubStatus.PUBLISHED);
        wd2.setCreatorUserName(testUser.getUserName());
        wd2.setUpdateTime(new Timestamp(System.currentTimeMillis()));
        wd2.setPubTime(new Timestamp(System.currentTimeMillis()));
        wd2.setWeblog(getManagedWeblog(testWeblog));

        cat = weblogManager.getWeblogCategory(testWeblog.getWeblogCategory("General").getId());
        wd2.setCategory(cat);

        weblogEntryManager.saveWeblogEntry(wd2);
        endSession(true);
        wd2 = getManagedWeblogEntry(wd2);

        indexManager.executeIndexOperationNow(
            new AddEntryOperation(weblogEntryManager, indexManager, wd2));

        Thread.sleep(DateUtils.MILLIS_PER_SECOND);

        SearchOperation search = new SearchOperation(indexManager);
        search.setTerm("Enterprise");
        indexManager.executeIndexOperationNow(search);
        assertEquals(2, search.getResultsCount());

        SearchOperation search2 = new SearchOperation(indexManager);
        search2.setTerm("Tholian");
        indexManager.executeIndexOperationNow(search2);
        assertEquals(1, search2.getResultsCount());

        // Clean up
        indexManager.removeEntryIndexOperation(wd1);
        indexManager.removeEntryIndexOperation(wd2);

        SearchOperation search3 = new SearchOperation(indexManager);
        search3.setTerm("Enterprise");
        indexManager.executeIndexOperationNow(search3);
        assertEquals(0, search3.getResultsCount());
    }    
}
