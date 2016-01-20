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
 */

package org.apache.roller.weblogger.ui.rendering.model;

import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.URLStrategy;
import org.apache.roller.weblogger.business.WeblogEntryManager;
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.ui.core.tags.calendar.BigWeblogCalendarModel;
import org.apache.roller.weblogger.ui.core.tags.calendar.CalendarTag;
import org.apache.roller.weblogger.ui.core.tags.calendar.WeblogCalendarModel;
import org.apache.roller.weblogger.ui.rendering.util.WeblogPageRequest;
import org.apache.roller.weblogger.ui.rendering.util.WeblogRequest;


/**
 * Model which provides functionality for displaying weblog calendar.
 * 
 * Implemented by calling hybrid JSP tag.
 */
public class CalendarModel implements Model {
    
    private static Log log = LogFactory.getLog(CalendarModel.class);
    
    private WeblogPageRequest pageRequest = null;

    protected WeblogEntryManager weblogEntryManager;

    public void setWeblogEntryManager(WeblogEntryManager weblogEntryManager) {
        this.weblogEntryManager = weblogEntryManager;
    }

    protected URLStrategy urlStrategy;

    public void setUrlStrategy(URLStrategy urlStrategy) {
        this.urlStrategy = urlStrategy;
    }

    /** Template context name to be used for model */
    public String getModelName() {
        return "calendarModel";
    }
    
    
    /** Init page model based on request */
    public void init(Map initData) throws WebloggerException {
        
        // we expect the init data to contain a weblogRequest object
        WeblogRequest weblogRequest = (WeblogRequest) initData.get("parsedRequest");
        if(weblogRequest == null) {
            throw new WebloggerException("expected weblogRequest from init data");
        }
        
        // CalendarModel only works on page requests, so cast weblogRequest
        // into a WeblogPageRequest and if it fails then throw exception
        if(weblogRequest instanceof WeblogPageRequest) {
            this.pageRequest = (WeblogPageRequest) weblogRequest;
        } else {
            throw new WebloggerException("weblogRequest is not a WeblogPageRequest."+
                    "  CalendarModel only supports page requests.");
        }
    }
    
    
    public String showWeblogEntryCalendar(Weblog websiteWrapper, String catArgument) {
        return showWeblogEntryCalendar(websiteWrapper, catArgument, false);
    }
    
    
    public String showWeblogEntryCalendarBig(Weblog websiteWrapper, String catArgument) {
        return showWeblogEntryCalendar(websiteWrapper, catArgument, true);
    }
    
    
    private String showWeblogEntryCalendar(Weblog websiteWrapper, String catArgument, boolean big) {
        
        if ("nil".equals(catArgument)) {
            catArgument = null;
        }
        String ret = null;
        try {
            org.apache.roller.weblogger.ui.core.tags.calendar.CalendarModel model;
            if (big) {
                model = new BigWeblogCalendarModel(pageRequest, catArgument, weblogEntryManager, urlStrategy);
            } else {
                model = new WeblogCalendarModel(pageRequest, catArgument, weblogEntryManager, urlStrategy);
            }
            
            // save model in JSP page context so CalendarTag can find it
//            pageContext.setAttribute("calendarModel", model);
            
            CalendarTag calTag = new CalendarTag();
            calTag.setName("calendar");
            calTag.setModel("calendarModel");
            calTag.setLocale(websiteWrapper.getLocaleInstance());
            calTag.setCalendarModel(model);
            if (big) {
                calTag.setClassSuffix("Big");
            }
            ret = calTag.emit();
        } catch (Exception e) {
            log.error("ERROR: initializing calendar tag",e);
        }
        return ret;
    }
    
}
