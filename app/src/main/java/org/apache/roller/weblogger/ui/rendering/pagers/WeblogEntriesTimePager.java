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
package org.apache.roller.weblogger.ui.rendering.pagers;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.WebloggerCommon;
import org.apache.roller.weblogger.business.WeblogEntryManager;
import org.apache.roller.weblogger.config.WebloggerRuntimeConfig;
import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.pojos.WeblogEntrySearchCriteria;
import org.apache.roller.weblogger.business.URLStrategy;
import org.apache.roller.weblogger.util.I18nMessages;

/**
 *  Pager for daily, monthly, and standard reverse chronological paging, e.g.:
 *  http://server/tightblog/myblog/date/20160120 (daily)
 *  http://server/tightblog/myblog/date/20160120 (monthly)
 *  http://server/tightblog/myblog/ (latest, pages via ?page=X query parameter)
 */
public class WeblogEntriesTimePager implements WeblogEntriesPager {
    
    private static Log log = LogFactory.getLog(WeblogEntriesTimePager.class);

    // url strategy for building urls
    URLStrategy urlStrategy = null;

    // message utils for doing i18n messages
    I18nMessages messageUtils = null;

    Weblog weblog = null;
    String dateString = null;
    String catName = null;
    List<String> tags = new ArrayList<>();
    int offset = 0;
    int page = 0;
    int length = 0;

    protected WeblogEntryManager weblogEntryManager;

    public enum PagingInterval {
        // For day-by-day paging
        DAY("day", WebloggerCommon.FORMAT_8CHARS),
        // month-by-month paging
        MONTH("month", WebloggerCommon.FORMAT_6CHARS),
        // default "all" paging: reverse chronological, no time bounds
        LATEST("latest", null);

        // used to get text for labels
        private String messageIndex;

        public String getMessageIndex() {
            return messageIndex;
        }

        private String dateFormat;

        public String getDateFormat() { return dateFormat; }

        PagingInterval(String messageIndex, String dateFormat) {
            this.messageIndex = messageIndex;
            this.dateFormat = dateFormat;
        }
    }

    private PagingInterval interval;

    private SimpleDateFormat dateFormat;
    
    private Date timePeriod;
    private Date nextTimePeriod;
    private Date prevTimePeriod;
    
    // collection for the pager
    private Map<Date, List<WeblogEntry>> entries = null;

    // are there more pages?
    private boolean more = false;

    public WeblogEntriesTimePager(
            PagingInterval interval,
            WeblogEntryManager weblogEntryManager,
            URLStrategy        strat,
            Weblog             weblog,
            String             dateString,
            String             catName,
            List<String>       tags,
            int                page) {

        this.urlStrategy = strat;
        this.weblogEntryManager = weblogEntryManager;
        this.weblog = weblog;
        this.dateString = dateString;
        this.catName = catName;

        if (tags != null) {
            this.tags = tags;
        }

        // make sure offset, length, and page are valid
        int maxLength = WebloggerRuntimeConfig.getIntProperty("site.pages.maxEntries");
        length = weblog.getEntryDisplayCount();
        if (length > maxLength) {
            length = maxLength;
        }

        if (page > 0) {
            this.page = page;
        }
        this.offset = length * page;

        // get a message utils instance to handle i18n of messages
        Locale viewLocale = weblog.getLocaleInstance();
        this.messageUtils = I18nMessages.getMessages(viewLocale);

        this.interval = interval;

        Date startDate = null;
        Date endDate = null;

        if (interval == PagingInterval.DAY || interval == PagingInterval.MONTH) {
            TimeZone tz = weblog.getTimeZoneInstance();

            dateFormat = new SimpleDateFormat(
                    messageUtils.getString("weblogEntriesPager." + interval.getMessageIndex() + ".dateFormat"));
            dateFormat.setTimeZone(tz);

            timePeriod = parseDate(dateString);
            Calendar cal = Calendar.getInstance(tz);
            cal.setTime(timePeriod);

            if (interval == PagingInterval.DAY) {
                startDate = DateUtils.truncate(cal, Calendar.DATE).getTime();
                endDate = DateUtils.addMilliseconds(DateUtils.ceiling(cal, Calendar.DATE).getTime(), -1);

                cal.add(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                nextTimePeriod = cal.getTime();
                if (nextTimePeriod.after(getToday())) {
                    nextTimePeriod = null;
                }

                cal.setTime(timePeriod);
                cal.add(Calendar.DAY_OF_MONTH, -1);
                cal.set(Calendar.HOUR, 23);
                cal.set(Calendar.MINUTE, 59);
                cal.set(Calendar.SECOND, 59);
                prevTimePeriod = cal.getTime();
                // one millisecond before start of the next day
                Date endOfPrevDay = new Date(DateUtils.ceiling(cal, Calendar.DATE).getTimeInMillis() - 1);
                Date weblogInitialDate = weblog.getDateCreated() != null ? weblog.getDateCreated() : new Date(0);
                if (endOfPrevDay.before(weblogInitialDate)) {
                    prevTimePeriod = null;
                }
            } else {
                startDate = DateUtils.truncate(cal, Calendar.MONTH).getTime();
                endDate = new Date(DateUtils.ceiling(cal.getTime(), Calendar.MONTH).getTime() - 1);

                cal.add(Calendar.MONTH, 1);
                nextTimePeriod = cal.getTime();
                // don't allow for paging into months in the future
                if (nextTimePeriod.after(getToday())) {
                    nextTimePeriod = null;
                }

                // don't allow for paging into months before the blog's create date
                cal.setTime(timePeriod);
                cal.add(Calendar.MONTH, -1);
                prevTimePeriod = cal.getTime();
                Date endOfPrevMonth = new Date(DateUtils.ceiling(cal, Calendar.MONTH).getTimeInMillis() - 1);
                Date weblogInitialDate = weblog.getDateCreated() != null ? weblog.getDateCreated() : new Date(0);
                if (endOfPrevMonth.before(weblogInitialDate)) {
                    prevTimePeriod = null;
                }
            }
        }

        loadEntries(startDate, endDate);
    }

    public Map<Date, List<WeblogEntry>> getEntries() { return entries; }

    public Map<Date, List<WeblogEntry>> loadEntries(Date startDate, Date endDate) {

        if (entries == null) {
            entries = new TreeMap<>(Collections.reverseOrder());
            try {
                WeblogEntrySearchCriteria wesc = new WeblogEntrySearchCriteria();
                wesc.setWeblog(weblog);
                wesc.setStartDate(startDate);
                wesc.setEndDate(endDate);
                wesc.setCatName(catName);
                wesc.setTags(tags);
                wesc.setStatus(WeblogEntry.PubStatus.PUBLISHED);
                wesc.setOffset(offset);
                wesc.setMaxResults(length+1);
                Map<Date, List<WeblogEntry>> mmap = weblogEntryManager.getWeblogEntryObjectMap(wesc);

                // need to wrap pojos
                int count = 0;
                for (Map.Entry<Date, List<WeblogEntry>> entry : mmap.entrySet()) {
                    // now we need to go through each entry in a timePeriod and wrap
                    List<WeblogEntry> wrapped = new ArrayList<>();
                    List<WeblogEntry> unwrapped = entry.getValue();
                    for (int i=0; i < unwrapped.size(); i++) {
                        if (count++ < length) {
                            wrapped.add(i, unwrapped.get(i).templateCopy());
                        } else {
                            more = true;
                        }
                    }
                    
                    // done with that timePeriod, put it in the map
                    if (wrapped.size() > 0) {
                        entries.put(entry.getKey(), wrapped);
                    }
                }
            } catch (Exception e) {
                log.error("ERROR: getting entry month map", e);
            }
        }
        return entries;
    }
    
    
    public String getHomeLink() {
        return createURL(0, 0, weblog, null, catName, tags);
    }
    
    
    public String getHomeName() {
        return messageUtils.getString("weblogEntriesPager." + interval.getMessageIndex() + ".home");
    }
    
    
    public String getNextLink() {
        if (more) {
            return createURL(page, 1, weblog, dateString, catName, tags);
        }
        return null;
    }
    
    
    public String getNextName() {
        if (getNextLink() != null) {
            return messageUtils.getString("weblogEntriesPager." + interval.getMessageIndex() + ".next",
                    dateFormat != null ? new Object[] {dateFormat.format(timePeriod)} : null);
        }
        return null;
    }
    
    
    public String getPrevLink() {
        if (page > 0) {
            return createURL(page, -1, weblog, dateString, catName, tags);
        }
        return null;
    }
    
    
    public String getPrevName() {
        if (getPrevLink() != null) {
            return messageUtils.getString("weblogEntriesPager." + interval.getMessageIndex() + ".prev",
                    dateFormat != null ? new Object[] {dateFormat.format(timePeriod)} : null);
        }
        return null;
    }
    
    
    public String getNextCollectionLink() {
        if (nextTimePeriod != null) {
            String next = FastDateFormat.getInstance(interval.getDateFormat(), weblog.getTimeZoneInstance()).format(nextTimePeriod);
            return createURL(0, 0, weblog, next, catName, tags);
        }
        return null;
    }
    
    
    public String getNextCollectionName() {
        if (nextTimePeriod != null) {
            return messageUtils.getString("weblogEntriesPager." + interval.getMessageIndex() + ".nextCollection",
                    dateFormat != null ? new Object[] {dateFormat.format(nextTimePeriod)} : null);
        }
        return null;
    }
    
    
    public String getPrevCollectionLink() {
        if (prevTimePeriod != null) {
            String prev = FastDateFormat.getInstance(interval.getDateFormat(), weblog.getTimeZoneInstance()).format(prevTimePeriod);
            return createURL(0, 0, weblog, prev, catName, tags);
        }
        return null;
    }
    
    
    public String getPrevCollectionName() {
        if (prevTimePeriod != null) {
            return messageUtils.getString("weblogEntriesPager." + interval.getMessageIndex() + ".prevCollection",
                    dateFormat != null ? new Object[] {dateFormat.format(prevTimePeriod)} : null);
        }
        return null;
    }

    /**
     * Return today based on current blog's timezone/locale.
     */
    protected Date getToday() {
        Calendar todayCal = Calendar.getInstance(
                weblog.getTimeZoneInstance(), weblog.getLocaleInstance());
        todayCal.setTime(new Date());
        return todayCal.getTime();
    }

    /**
     * Parse data as either 6-char or 8-char format.
     */
    protected Date parseDate(String dateString) {
        FastDateFormat dateFormat;
        if (dateString != null && StringUtils.isNumeric(dateString)) {
            if (dateString.length() == 8) {
                dateFormat = FastDateFormat.getInstance(WebloggerCommon.FORMAT_8CHARS,
                        weblog.getTimeZoneInstance(), weblog.getLocaleInstance());
            } else if (dateString.length() == 6) {
                dateFormat = FastDateFormat.getInstance(WebloggerCommon.FORMAT_6CHARS,
                        weblog.getTimeZoneInstance(), weblog.getLocaleInstance());
            } else {
                return null;
            }
        } else {
            return null;
        }

        ParsePosition pos = new ParsePosition(0);
        Date ret = dateFormat.parse(dateString, pos);

        // make sure the requested date is not in the future
        Date today = getToday();
        if (ret.after(today)) {
            ret = today;
        }
        return ret;
    }

    /**
     * Create URL that encodes pager state using most appropriate forms of URL.
     * @param pageAdd To be added to page number, or 0 for no page number
     */
    protected String createURL(
            int                page,
            int                pageAdd,
            Weblog             website,
            String             dateString,
            String             catName,
            List               tags) {

        int pageNum = page + pageAdd;
        return urlStrategy.getWeblogCollectionURL(website, catName, dateString, tags, pageNum, false);
    }
}
