/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tightblog.rendering.requests;

import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WeblogFeedRequestTest {

    private HttpServletRequest mockRequest;

    @Before
    public void initializeMocks() {
        mockRequest = mock(HttpServletRequest.class);
    }

    @Test
    public void testParseFullFeed() {
        when(mockRequest.getPathInfo()).thenReturn("/myblog/feed/");

        WeblogFeedRequest.Creator creator = new WeblogFeedRequest.Creator();
        WeblogFeedRequest wfr = creator.create(mockRequest);
        assertEquals("myblog", wfr.getWeblogHandle());
        assertNull(wfr.getWeblogCategoryName());
        assertNull(wfr.getTag());
    }

    @Test
    public void testParseCategoryFeed() {
        when(mockRequest.getPathInfo()).thenReturn("/myblog/feed/category/abc");

        WeblogFeedRequest.Creator creator = new WeblogFeedRequest.Creator();
        WeblogFeedRequest wfr = creator.create(mockRequest);
        assertEquals("myblog", wfr.getWeblogHandle());
        assertEquals("abc", wfr.getWeblogCategoryName());
        assertNull(wfr.getTag());
    }

    @Test
    public void testParseTagFeed() {
        when(mockRequest.getPathInfo()).thenReturn("/myblog/feed/tag/bcd");

        WeblogFeedRequest.Creator creator = new WeblogFeedRequest.Creator();
        WeblogFeedRequest wfr = creator.create(mockRequest);
        assertEquals("myblog", wfr.getWeblogHandle());
        assertNull(wfr.getWeblogCategoryName());
        assertEquals("bcd", wfr.getTag());
    }
}