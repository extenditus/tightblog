/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
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
package org.apache.roller.weblogger.business.jpa;

import org.apache.roller.weblogger.business.UserManager;
import org.apache.roller.weblogger.business.WeblogManager;
import org.apache.roller.weblogger.pojos.GlobalRole;
import org.apache.roller.weblogger.pojos.User;
import org.apache.roller.weblogger.pojos.UserCredentials;
import org.apache.roller.weblogger.pojos.UserSearchCriteria;
import org.apache.roller.weblogger.pojos.UserWeblogRole;
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.pojos.WeblogRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JPAUserManagerImpl implements UserManager {

    private static Logger log = LoggerFactory.getLogger(JPAUserManagerImpl.class);

    private final JPAPersistenceStrategy strategy;

    private WeblogManager weblogManager;

    public void setWeblogManager(WeblogManager weblogManager) {
        this.weblogManager = weblogManager;
    }

    // cached mapping of userNames -> userIds
    private Map<String, String> userNameToIdMap = new HashMap<>();

    // cached mapping of screenNames -> userIds
    private Map<String, String> screenNameToIdMap = new HashMap<>();

    protected JPAUserManagerImpl(JPAPersistenceStrategy strat) {
        this.strategy = strat;
    }

    @Override
    public void removeUser(User user) {
        String userName = user.getUserName();
        
        // remove roles, maintaining both sides of relationship
        List<UserWeblogRole> roles = getWeblogRolesIncludingPending(user);
        this.strategy.removeAll(roles);
        this.strategy.remove(user);

        // remove entry from cache mapping
        this.userNameToIdMap.remove(userName);
    }

    @Override
    public void saveUser(User data) {
        if (data == null) {
            throw new IllegalArgumentException("cannot save null user");
        }
        strategy.store(data);
        strategy.flush();
    }

    @Override
    public User getUser(String id) {
        return this.strategy.load(User.class, id);
    }

    @Override
    public User getUserByUserName(String userName) {
        return getUserByUserName(userName, Boolean.TRUE);
    }

    @Override
    public UserCredentials getCredentialsByUserName(String userName) {
        if (userName==null) {
            throw new IllegalArgumentException("userName cannot be null");
        }

        TypedQuery<UserCredentials> query;
        query = strategy.getNamedQuery("UserCredentials.getByUserName", UserCredentials.class);
        query.setParameter(1, userName);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");

        UserCredentials creds;
        try {
            creds = query.getSingleResult();
        } catch (NoResultException e) {
            creds = null;
        }
        return creds;
    }

    @Override
    public void updateCredentials(String userId, String newPassword) {
        Query q = strategy.getNamedUpdate("UserCredentials.changePassword");
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        q.setParameter(1, encoder.encode(newPassword));
        q.setParameter(2, userId);
        q.executeUpdate();
        strategy.flush();
    }

    @Override
    public User getUserByUserName(String userName, Boolean enabled) {

        if (userName==null) {
            throw new IllegalArgumentException("userName cannot be null");
        }
        
        // check cache first
        // NOTE: if we ever allow changing usernames then this needs updating
        if (userNameToIdMap.containsKey(userName)) {

            User user = getUser(userNameToIdMap.get(userName));
            if (user != null) {
                // only return the user if the enabled status matches
                if (enabled == null || enabled.equals(user.isEnabled())) {
                    log.debug("userNameToIdMap CACHE HIT - {}", userName);
                    return user;
                }
            } else {
                // mapping hit with lookup miss?  mapping must be old, remove it
                userNameToIdMap.remove(userName);
            }
        }

        // cache failed, do lookup
        TypedQuery<User> query;
        Object[] params;
        if (enabled != null) {
            query = strategy.getNamedQuery("User.getByUserName&Enabled", User.class);
            params = new Object[] {userName, enabled};
        } else {
            query = strategy.getNamedQuery("User.getByUserName", User.class);
            params = new Object[] {userName};
        }
        for (int i=0; i<params.length; i++) {
            query.setParameter(i+1, params[i]);
        }
        User user;
        try {
            user = query.getSingleResult();
        } catch (NoResultException e) {
            user = null;
        }

        // add mapping to cache
        if(user != null) {
            log.debug("userNameToIdMap CACHE MISS - {}", userName);
            this.userNameToIdMap.put(user.getUserName(), user.getId());
        }

        return user;
    }


    @Override
    public User getUserByScreenName(String screenName) {

        if (screenName==null) {
            throw new IllegalArgumentException("screenName cannot be null");
        }

        // check cache first
        if(this.screenNameToIdMap.containsKey(screenName)) {
            User user = this.getUser(this.screenNameToIdMap.get(screenName));
            if (user != null) {
                log.debug("screenNameToIdMap CACHE HIT - {}", screenName);
                return user;
            } else {
                // mapping hit with lookup miss?  mapping must be old, remove it
                this.screenNameToIdMap.remove(screenName);
            }
        }

        // cache failed, do lookup
        TypedQuery<User> query;
        Object[] params;
        query = strategy.getNamedQuery("User.getByScreenName", User.class);
        params = new Object[] {screenName};

        for (int i=0; i<params.length; i++) {
            query.setParameter(i+1, params[i]);
        }
        User user;
        try {
            user = query.getSingleResult();
        } catch (NoResultException e) {
            user = null;
        }

        // add mapping to cache
        if(user != null) {
            log.debug("screenNameToIdMap CACHE MISS - {}", screenName);
            this.screenNameToIdMap.put(user.getUserName(), user.getId());
        }

        return user;
    }

    @Override
    public List<User> getUsers(UserSearchCriteria criteria) {
        List<Object> params = new ArrayList<>();
        int size = 0;
        StringBuilder queryString = new StringBuilder();

        queryString.append("SELECT u FROM User u WHERE 1=1 ");

        if (criteria.getApproved() != null) {
            params.add(size++, criteria.getApproved());
            queryString.append(" and u.approved = ?").append(size);
        }

        if (criteria.getEnabled() != null) {
            params.add(size++, criteria.getEnabled());
            queryString.append(" and u.enabled = ?").append(size);
        }

        if (criteria.getGlobalRole() != null) {
            params.add(size++, criteria.getGlobalRole());
            queryString.append(" and u.globalRole = ?").append(size);
        }

        queryString.append(" ORDER BY u.screenName ");

        TypedQuery<User> query = strategy.getDynamicQuery(queryString.toString(), User.class);
        for (int i = 0; i < params.size(); i++) {
            query.setParameter(i + 1, params.get(i));
        }

        query.setHint("javax.persistence.cache.storeMode", "REFRESH");

        if (criteria.getOffset() != 0) {
            query.setFirstResult(criteria.getOffset());
        }
        if (criteria.getMaxResults() != null) {
            query.setMaxResults(criteria.getMaxResults());
        }

        return query.getResultList();
    }

    @Override
    public long getUserCount() {
        TypedQuery<Long> q = strategy.getNamedQuery("User.getCountEnabledDistinct", Long.class);
        q.setParameter(1, Boolean.TRUE);
        List<Long> results = q.getResultList();
        return results.get(0);
    }

    @Override
    public User getUserByActivationCode(String activationCode) {
        if (activationCode == null) {
            throw new IllegalArgumentException("activationcode is null");
        }
        TypedQuery<User> q = strategy.getNamedQuery("User.getUserByActivationCode", User.class);
        q.setParameter(1, activationCode);
        try {
            return q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public boolean checkWeblogRole(String username, String weblogHandle, WeblogRole role) {
        User userToCheck = getUserByUserName(username, true);
        Weblog weblogToCheck = weblogManager.getWeblogByHandle(weblogHandle, null);
        return !(userToCheck == null || weblogToCheck == null) && checkWeblogRole(userToCheck, weblogToCheck, role);
    }

    @Override
    public boolean checkWeblogRole(User user, Weblog weblog, WeblogRole role) {

        // if user has specified permission in weblog return true
        UserWeblogRole existingRole = getWeblogRole(user, weblog);
        if (existingRole != null && existingRole.hasEffectiveWeblogRole(role)) {
            return true;
        }

        // if Blog Server admin would still have any weblog role
        if (GlobalRole.ADMIN.equals(user.getGlobalRole())) {
            return true;
        }

        if (log.isDebugEnabled()) {
            log.debug("ROLE CHECK FAILED: user {} does not have {} or greater rights on weblog {}", weblog.getHandle(),
                    user.getUserName(), role.name());
        }
        return false;
    }

    @Override
    public UserWeblogRole getUserWeblogRole(String id) {
        return this.strategy.load(UserWeblogRole.class, id);
    }

    @Override
    public UserWeblogRole getWeblogRole(String username, String weblogHandle) {
        User userToCheck = getUserByUserName(username, true);
        Weblog weblogToCheck = weblogManager.getWeblogByHandle(weblogHandle);
        return getWeblogRole(userToCheck, weblogToCheck);
    }

    @Override
    public UserWeblogRole getWeblogRole(User user, Weblog weblog) {
        TypedQuery<UserWeblogRole> q = strategy.getNamedQuery("UserWeblogRole.getByUserId&WeblogId"
                , UserWeblogRole.class);
        q.setParameter(1, user.getId());
        q.setParameter(2, weblog.getId());
        try {
            return q.getSingleResult();
        } catch (NoResultException ignored) {
            return null;
        }
    }

    @Override
    public UserWeblogRole getWeblogRoleIncludingPending(User user, Weblog weblog) {
        TypedQuery<UserWeblogRole> q = strategy.getNamedQuery("UserWeblogRole.getByUserId&WeblogIdIncludingPending",
                UserWeblogRole.class);
        q.setParameter(1, user.getId());
        q.setParameter(2, weblog.getId());
        try {
            return q.getSingleResult();
        } catch (NoResultException ignored) {
            return null;
        }
    }

    @Override
    public void grantWeblogRole(User user, Weblog weblog, WeblogRole role) {

        // first, see if user already has a permission for the specified object
        TypedQuery<UserWeblogRole> q = strategy.getNamedQuery("UserWeblogRole.getByUserId&WeblogIdIncludingPending",
                UserWeblogRole.class);
        q.setParameter(1, user.getId());
        q.setParameter(2, weblog.getId());
        UserWeblogRole existingPerm = null;

        try {
            existingPerm = q.getSingleResult();
        } catch (NoResultException ignored) {}

        // role already exists, so update it
        if (existingPerm != null) {
            existingPerm.setWeblogRole(role);
            existingPerm.setPending(false);
            this.strategy.store(existingPerm);
        } else {
            // it's a new association, so store it
            UserWeblogRole perm = new UserWeblogRole(user, weblog, role);
            this.strategy.store(perm);
        }
    }

    @Override
    public void grantPendingWeblogRole(User user, Weblog weblog, WeblogRole desiredRole) {

        // first, see if user already has a role for the specified weblog
        TypedQuery<UserWeblogRole> q = strategy.getNamedQuery("UserWeblogRole.getByUserId&WeblogIdIncludingPending",
                UserWeblogRole.class);
        q.setParameter(1, user.getId());
        q.setParameter(2, weblog.getId());
        UserWeblogRole existingRole = null;
        try {
            existingRole = q.getSingleResult();
        } catch (NoResultException ignored) {}

        if (existingRole == null) {
            UserWeblogRole newRole = new UserWeblogRole(user, weblog, desiredRole);
            newRole.setPending(true);
            this.strategy.store(newRole);
        }
    }

    
    @Override
    public void acceptWeblogInvitation(UserWeblogRole uwr) {
        // check role is still in DB
        UserWeblogRole existingRole = getUserWeblogRole(uwr.getId());
        if (existingRole != null) {
            existingRole.setPending(false);
            this.strategy.store(existingRole);
        }
    }

    @Override
    public void revokeWeblogRole(UserWeblogRole roleToRevoke) {
        // get specified role
        UserWeblogRole existingRole = getUserWeblogRole(roleToRevoke.getId());
        if (existingRole != null) {
            this.strategy.remove(existingRole);
        }
    }

    
    @Override
    public List<UserWeblogRole> getWeblogRoles(User user) {
        TypedQuery<UserWeblogRole> q = strategy.getNamedQuery("UserWeblogRole.getByUserId",
                UserWeblogRole.class);
        q.setParameter(1, user.getId());
        return q.getResultList();
    }

    @Override
    public List<UserWeblogRole> getWeblogRolesIncludingPending(User user) {
        TypedQuery<UserWeblogRole> q = strategy.getNamedQuery("UserWeblogRole.getByUserIdIncludingPending",
                UserWeblogRole.class);
        q.setParameter(1, user.getId());
        return q.getResultList();
    }

    @Override
    public List<UserWeblogRole> getWeblogRoles(Weblog weblog) {
        TypedQuery<UserWeblogRole> q = strategy.getNamedQuery("UserWeblogRole.getByWeblogId",
                UserWeblogRole.class);
        q.setParameter(1, weblog.getId());
        return q.getResultList();
    }

    @Override
    public List<UserWeblogRole> getWeblogRolesIncludingPending(Weblog weblog) {
        TypedQuery<UserWeblogRole> q = strategy.getNamedQuery("UserWeblogRole.getByWeblogIdIncludingPending",
                UserWeblogRole.class);
        q.setParameter(1, weblog.getId());
        return q.getResultList();
    }

    @Override
    public List<UserWeblogRole> getPendingWeblogRoles(Weblog weblog) {
        TypedQuery<UserWeblogRole> q = strategy.getNamedQuery("UserWeblogRole.getByWeblogId&Pending",
                UserWeblogRole.class);
        q.setParameter(1, weblog.getId());
        return q.getResultList();
    }

}
