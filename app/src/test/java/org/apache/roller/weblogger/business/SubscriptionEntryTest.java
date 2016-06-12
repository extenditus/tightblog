/*
 * Copyright 2005 Sun Microsystems, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Source file modified from the original ASF source; all changes made
 * are also under Apache License.
 */
package org.apache.roller.weblogger.business;

import java.time.LocalDateTime;
import org.apache.roller.weblogger.WebloggerTest;
import org.apache.roller.weblogger.pojos.Planet;
import org.apache.roller.weblogger.pojos.SubscriptionEntry;
import org.apache.roller.weblogger.pojos.Subscription;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test planet Entry functionality.
 */
public class SubscriptionEntryTest extends WebloggerTest {
    
    private Planet testPlanet = null;
    private Subscription testSub = null;
    private SubscriptionEntry testEntry = null;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        testPlanet = setupPlanet("testPlanet");
        testSub = setupSubscription(testPlanet, "testSubscription");
        testEntry = setupEntry(testSub, "testEntry");
        setupEntry(testSub, "testEntry2");

        planetManager.savePlanet(testPlanet);
        strategy.flush();
    }
    
    @After
    public void tearDown() throws Exception {
        teardownPlanet(testPlanet.getHandle());
    }

    @Test
    public void testEntryLookups() throws Exception {
        // by id
        SubscriptionEntry entry = planetManager.getEntryById(testEntry.getId());
        assertNotNull(entry);
        assertEquals("testEntry", entry.getPermalink());
        
        // by subscription
        Subscription sub = planetManager.getSubscription(testSub.getId());
        assertEquals(2, sub.getEntries().size());
        
        // by planet
        Planet planet = planetManager.getPlanet(testPlanet.getId());
        assertEquals(2, planetManager.getEntries(planet, null, 0, 10).size());
        
        // by planet with timeframe constraint
        assertEquals(0, planetManager.getEntries(planet, LocalDateTime.now(), 0, 10).size());
    }
    
    @Test
    public void testDeleteEntries() throws Exception {
        Subscription sub = planetManager.getSubscription(testSub.getId());
        
        // make sure entries are there
        assertEquals(2, sub.getEntries().size());
        
        // purge entries
        sub.getEntries().clear();
        endSession(true);
        
        // verify
        sub = planetManager.getSubscription(testSub.getId());
        assertEquals(0, sub.getEntries().size());
    }
    
}
