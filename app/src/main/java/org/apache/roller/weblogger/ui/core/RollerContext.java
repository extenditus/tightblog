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

package org.apache.roller.weblogger.ui.core;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserCache;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;
import org.springframework.security.authentication.RememberMeAuthenticationProvider;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.BootstrapException;
import org.apache.roller.weblogger.business.startup.StartupException;
import org.apache.roller.weblogger.config.WebloggerConfig;
import org.apache.roller.weblogger.business.WebloggerFactory;
import org.apache.roller.weblogger.business.startup.WebloggerStartup;
import org.apache.roller.weblogger.ui.core.plugins.UIPluginManager;
import org.apache.roller.weblogger.ui.core.plugins.UIPluginManagerImpl;
import org.apache.roller.weblogger.ui.core.security.AutoProvision;
import org.apache.roller.weblogger.util.cache.CacheManager;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.WebApplicationContextUtils;


/**
 * Initialize the Roller web application/context.
 */
public class RollerContext extends ContextLoaderListener  
        implements ServletContextListener { 
    
    private static Log log = LogFactory.getLog(RollerContext.class);
    
    private static ServletContext servletContext = null;

    
    public RollerContext() {
        super();
    }
    
    
    /**
     * Access to the plugin manager for the UI layer. TODO: we may want 
     * something similar to the Roller interface for the UI layer if we dont 
     * want methods like this here in RollerContext.
     */
    public static UIPluginManager getUIPluginManager() {
        return UIPluginManagerImpl.getInstance();
    }
    
    
    /**
     * Get the ServletContext.
     * @return ServletContext
     */
    public static ServletContext getServletContext() {
        return servletContext;
    } 
    
    
    /**
     * Responds to app-init event and triggers startup procedures.
     */
    public void contextInitialized(ServletContextEvent sce) {

        // First, initialize everything that requires no database

        // Keep a reference to ServletContext object
        RollerContext.servletContext = sce.getServletContext();
        
        // Call Spring's context ContextLoaderListener to initialize all the
        // context files specified in web.xml. This is necessary because
        // listeners don't initialize in the order specified in 2.3 containers
        super.contextInitialized(sce);
        
        // get the *real* path to <context>/resources
        String ctxPath = servletContext.getRealPath("/");
        if (ctxPath == null) {
            log.fatal("Roller requires an exploded WAR file to run.");
            return;
        }
        if (!ctxPath.endsWith(File.separator)) {
            ctxPath += File.separator + "resources";
        } else {
            ctxPath += "resources";
        }
        
        // try setting the themes path to <context>/themes
        // NOTE: this should go away at some point
        // we leave it here for now to allow users to keep using
        // themes in their webapp context, but this is a bad idea
        //
        // also, the WebloggerConfig.setThemesDir() method is smart
        // enough to disregard this call unless the themes.dir
        // is set to ${webapp.context}
        WebloggerConfig.setThemesDir(servletContext.getRealPath("/")+File.separator+"themes");
        
        
        // Now prepare the core services of the app so we can bootstrap
        try {
            WebloggerStartup.prepare();
        } catch (StartupException ex) {
            log.fatal("Roller Weblogger startup failed during app preparation", ex);
            return;
        }
        
        
        // if preparation failed or is incomplete then we are done,
        // otherwise try to bootstrap the business tier
        if (!WebloggerStartup.isPrepared()) {
            StringBuilder buf = new StringBuilder();
            buf.append("\n--------------------------------------------------------------");
            buf.append("\nRoller Weblogger startup INCOMPLETE, user interaction required");
            buf.append("\n--------------------------------------------------------------");
            log.info(buf.toString());
        } else {
            try {
                // trigger bootstrapping process
                WebloggerFactory.bootstrap();
                
                // trigger initialization process
                WebloggerFactory.getWeblogger().initialize();
                
            } catch (BootstrapException ex) {
                log.fatal("Roller Weblogger bootstrap failed", ex);
            } catch (WebloggerException ex) {
                log.fatal("Roller Weblogger initialization failed", ex);
            }
		}
            
        // do a small amount of work to initialize the web tier
        try {
            // Initialize Spring Security based on Roller configuration
            initializeSecurityFeatures(servletContext);
            
            // Setup Velocity template engine
            setupVelocity();
        } catch (WebloggerException ex) {
            log.fatal("Error initializing Roller Weblogger web tier", ex);
        }
        
    }
    
    
    /** 
     * Responds to app-destroy event and triggers shutdown sequence.
     */
    public void contextDestroyed(ServletContextEvent sce) {        
        WebloggerFactory.getWeblogger().shutdown();        
        // do we need a more generic mechanism for presentation layer shutdown?
        CacheManager.shutdown();
    }
    
    
    /**
     * Initialize the Velocity rendering engine.
     */
    private void setupVelocity() throws WebloggerException {        
        log.info("Initializing Velocity");
        
        // initialize the Velocity engine
        Properties velocityProps = new Properties();
        
        try {
            InputStream instream = servletContext.getResourceAsStream("/WEB-INF/velocity.properties");
            
            velocityProps.load(instream);
            
            log.debug("Velocity props = "+velocityProps);
            
            // init velocity
            RuntimeSingleton.init(velocityProps);
            
        } catch (Exception e) {
            throw new WebloggerException(e);
        }
        
    }
         
    /**
     * Setup Spring Security security features.
     */
    protected void initializeSecurityFeatures(ServletContext context) { 

        ApplicationContext ctx =
                WebApplicationContextUtils.getRequiredWebApplicationContext(context);

        /*String[] beanNames = ctx.getBeanDefinitionNames();
        for (String name : beanNames)
            System.out.println(name);*/
        
        String rememberMe = WebloggerConfig.getProperty("rememberme.enabled");
        boolean rememberMeEnabled = Boolean.valueOf(rememberMe);
        
        log.info("Remember Me enabled: " + rememberMeEnabled);
        
        context.setAttribute("rememberMeEnabled", rememberMe);
        
        if (!rememberMeEnabled) {
            ProviderManager provider = (ProviderManager) ctx.getBean("_authenticationManager");
            for (AuthenticationProvider authProvider : provider.getProviders()) {
                if (authProvider instanceof RememberMeAuthenticationProvider) {
                    provider.getProviders().remove(authProvider);
                }
            }
        }
        
        String encryptPasswords = WebloggerConfig.getProperty("passwds.encryption.enabled");
        boolean doEncrypt = Boolean.valueOf(encryptPasswords);
        
        String daoBeanName = "org.springframework.security.authentication.dao.DaoAuthenticationProvider#0";

        // for LDAP-only authentication, no daoBeanName (i.e., UserDetailsService) may be provided in security.xml.
        if (doEncrypt && ctx.containsBean(daoBeanName)) {
            DaoAuthenticationProvider provider = (DaoAuthenticationProvider) ctx.getBean(daoBeanName);
            String algorithm = WebloggerConfig.getProperty("passwds.encryption.algorithm");
            PasswordEncoder encoder = null;
            if (algorithm.equalsIgnoreCase("SHA")) {
                encoder = new ShaPasswordEncoder();
            } else if (algorithm.equalsIgnoreCase("MD5")) {
                encoder = new Md5PasswordEncoder();
            } else {
                log.error("Encryption algorithm '" + algorithm + "' not supported, disabling encryption.");
            }
            if (encoder != null) {
                provider.setPasswordEncoder(encoder);
                log.info("Password Encryption Algorithm set to '" + algorithm + "'");
            }
        }

        if (WebloggerConfig.getBooleanProperty("securelogin.enabled")) {
            LoginUrlAuthenticationEntryPoint entryPoint =
                (LoginUrlAuthenticationEntryPoint) ctx.getBean("_formLoginEntryPoint");
            entryPoint.setForceHttps(true);
        }
    }
    
    
    /**
     * Flush user from any caches maintained by security system.
     */
    public static void flushAuthenticationUserCache(String userName) {                                
        ApplicationContext ctx = 
            WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
		try {
			UserCache userCache = (UserCache) ctx.getBean("userCache");
			if (userCache != null) {
				userCache.removeUserFromCache(userName);
			}
		} catch (NoSuchBeanDefinitionException exc) {
			log.debug("No userCache bean in context", exc);
		}
    }
 
    
    /**
     * Get an instance of AutoProvision, if available in roller.properties
     * @return AutoProvision
     */
    public static AutoProvision getAutoProvision() {        
        String clazzName = WebloggerConfig.getProperty("users.ldap.autoProvision.className");
        
        if (null == clazzName) {
            return null;
        }
        
        Class clazz;
        try {
            clazz = Class.forName(clazzName);
        } catch (ClassNotFoundException e) {
            log.warn("Unable to found specified Auto Provision class.", e);
            return null;
        }
        
        Class[] interfaces = clazz.getInterfaces();
        for (Class clazz2 : interfaces) {
            if (clazz2.equals(AutoProvision.class)) {
                try {
                    return (AutoProvision) clazz.newInstance();
                } catch (InstantiationException e) {
                    log.warn("InstantiationException while creating: " + clazzName, e);
                } catch (IllegalAccessException e) {
                    log.warn("IllegalAccessException while creating: " + clazzName, e);
                }
            }
        }        
        return null;        
    }   
}
