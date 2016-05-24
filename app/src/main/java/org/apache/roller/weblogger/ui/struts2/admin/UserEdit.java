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
package org.apache.roller.weblogger.ui.struts2.admin;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import com.opensymphony.xwork2.validator.annotations.EmailValidator;
import com.opensymphony.xwork2.validator.annotations.Validations;
import org.apache.commons.lang3.CharSetUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.roller.weblogger.WebloggerCommon;
import org.apache.roller.weblogger.WebloggerCommon.AuthMethod;
import org.apache.roller.weblogger.business.WeblogManager;
import org.apache.roller.weblogger.business.WebloggerFactory;
import org.apache.roller.weblogger.business.UserManager;
import org.apache.roller.weblogger.business.WebloggerStaticConfig;
import org.apache.roller.weblogger.pojos.SafeUser;
import org.apache.roller.weblogger.pojos.User;
import org.apache.roller.weblogger.pojos.UserWeblogRole;
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.pojos.WeblogRole;
import org.apache.roller.weblogger.ui.struts2.core.Register;
import org.apache.roller.weblogger.ui.struts2.util.UIAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.RollbackException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@RestController
public class UserEdit extends UIAction {

    private static Logger log = LoggerFactory.getLogger(UserEdit.class);

    @Autowired
    private UserManager userManager;

    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
    }

    @Autowired
    private WeblogManager weblogManager;

    public void setWeblogManager(WeblogManager weblogManager) {
        this.weblogManager = weblogManager;
    }

    public UserEdit() {
    }

    @Override
    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    @RequestMapping(value = "/tb-ui/admin/rest/useradmin/userlist", method = RequestMethod.GET)
    public Map<String, String> getUserEditList() throws ServletException {
        return createUserMap(userManager.getUsers(null, null, 0, -1));
    }

    @RequestMapping(value = "/tb-ui/admin/rest/useradmin/user/{id}", method = RequestMethod.GET)
    public User getUserData(@PathVariable String id, HttpServletResponse response) throws ServletException {
        User user = userManager.getUser(id);
        if (user != null) {
            user.setPassword(null);
            user.setPasswordText(null);
            user.setPasswordConfirm(null);
            return user;
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
    }

    @RequestMapping(value = "/tb-ui/admin/rest/useradmin/user/{id}", method = RequestMethod.PUT)
    public User updateUser(@PathVariable String id, @RequestBody User newData,
                               HttpServletResponse response) throws ServletException {
        User user = userManager.getUser(id);
        saveUser(user, newData, response, false);
        return user;
    }

    @RequestMapping(value = "/tb-ui/admin/rest/useradmin/users", method = RequestMethod.PUT)
    public User addUser(@RequestBody User newData, HttpServletResponse response) throws ServletException {
        User user = new User();
        user.setId(WebloggerCommon.generateUUID());
        user.setUserName(newData.getUserName());
        user.setDateCreated(new java.util.Date());
        user.setGlobalRole(newData.getGlobalRole());
        saveUser(user, newData, response, true);
        return response.getStatus() == HttpServletResponse.SC_OK ? user : null;
    }

    private void saveUser(User user, User newData, HttpServletResponse response, boolean isAdd) throws ServletException {
        try {
            if (user != null) {
                user.setScreenName(newData.getScreenName().trim());
                user.setEmailAddress(newData.getEmailAddress().trim());
                user.setLocale(newData.getLocale());
                user.setEnabled(newData.getEnabled());

                // reset password if set
                if (!StringUtils.isEmpty(newData.getPassword())) {
                    user.resetPassword(newData.getPassword().trim());
                }
                if (isAdd) {
                    // save new user
                    userManager.addUser(user);
                } else {
                    if (!user.equals(getAuthenticatedUser())) {
                        user.setGlobalRole(newData.getGlobalRole());
                    }
                    userManager.saveUser(user);
                }

                try {
                    WebloggerFactory.flush();
                    response.setStatus(HttpServletResponse.SC_OK);
                } catch (RollbackException e) {
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                }
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (Exception e) {
            throw new ServletException(e.getMessage());
        }
    }



    @RequestMapping(value = "/tb-ui/authoring/rest/{weblogHandle}/potentialmembers", method = RequestMethod.GET)
    public Map<String, String> getPotentialNewMembers(@PathVariable String weblogHandle, Principal p,
                                                      HttpServletResponse response)
            throws ServletException {

        if (userManager.checkWeblogRole(p.getName(), weblogHandle, WeblogRole.OWNER)) {
            // member list excludes inactive accounts
            List<SafeUser> potentialUsers = userManager.getUsers(null, true, 0, -1);

            // filter out people already members
            Weblog weblog = weblogManager.getWeblogByHandle(weblogHandle);
            ListIterator<SafeUser> potentialIter = potentialUsers.listIterator();
            List<UserWeblogRole> currentUserList = userManager.getWeblogRolesIncludingPending(weblog);
            while (potentialIter.hasNext() && !currentUserList.isEmpty()) {
                SafeUser su = potentialIter.next();
                ListIterator<UserWeblogRole> alreadyIter = currentUserList.listIterator();
                while (alreadyIter.hasNext()) {
                    UserWeblogRole au = alreadyIter.next();
                    if (su.getId().equals(au.getUser().getId())) {
                        potentialIter.remove();
                        alreadyIter.remove();
                        break;
                    }
                }
            }
            return createUserMap(potentialUsers);
        } else {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
    }

    private Map<String, String> createUserMap(List<SafeUser> users) {
        Map<String, String> userMap = new HashMap<>();
        for (SafeUser user : users) {
            userMap.put(user.getId(), user.getScreenName() + " (" + user.getEmailAddress() + ")");
        }
        return userMap;
    }


    @Validations(
            emails = { @EmailValidator(fieldName="bean.emailAddress", key="Register.error.emailAddressBad")}
    )
    private void myValidate() {
        User bean = new User();

        if (StringUtils.isEmpty(bean.getUserName())) {
            addError("error.add.user.missingUserName");
        }
        if (StringUtils.isEmpty(bean.getScreenName())) {
            addError("Register.error.screenNameNull");
        }
        if (StringUtils.isEmpty(bean.getEmailAddress())) {
            addError("Register.error.emailAddressNull");
        }
        // if (isAdd()) {
            String allowed = WebloggerStaticConfig.getProperty("username.allowedChars");
            if(allowed == null || allowed.trim().length() == 0) {
                allowed = Register.DEFAULT_ALLOWED_CHARS;
            }
            String safe = CharSetUtils.keep(bean.getUserName(), allowed);
            if (!safe.equals(bean.getUserName()) ) {
                addError("error.add.user.badUserName");
            }
            if (WebloggerStaticConfig.getAuthMethod() == AuthMethod.DATABASE && StringUtils.isEmpty(bean.getPassword())) {
                addError("error.add.user.missingPassword");
            }
       //  }
    }

}
