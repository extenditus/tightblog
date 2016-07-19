Welcome to TightBlog! This project started off in May 2015 as a fork of the Apache Roller project.  As of 17 July 2016, <a href="https://github.com/gmazza/tightblog/releases">Release 1.0.0</a> is available.

TightBlog strives to be the mathematically cleanest and simplest implementation of a Java based blog and planet server, suitable either for direct use or
incorporation, as an Apache-licensed open source project, into larger projects.  Specifically, its goal is to satisfy the needs of 80% of bloggers while
avoiding obscure functionality that ends up bloating the application while not providing much value.

This more realistic goal--along with adopting the Spring framework, REST, and other code modernizations--has allowed TightBlog to slim down considerably from its parent: 
The 1.0.0 release of TightBlog uses 17 database tables compared to Roller V5.1.2's 33, 187 Java source files to 493 in Roller, and 51 JSPs vs. Roller's 92.  Only increase, 
a nice one, is about 15 more JavaScript files have been added, due to TightBlog's increased emphasis on browser-side processing.  

The <a href="https://www.openhub.net/p/tightblog/analyses/latest/languages_summary">OpenHub statistics</a>, updated every few weeks, provide trending code size and language breakdown.

Check <a href="https://web-gmazza.rhcloud.com/blog/category/Blogs+%26+Wikis">my blog</a> for recent status updates.

The top-level TightBlog directory consists of the following folders:

* app:                    TightBlog application - WAR application meant for deployment on a servlet container
* docs:                   Documentation in ODT (OpenOffice/LibreOffice) format: Install, User, and Templates guides.
* it-selenium:            Integrated browser tests for TightBlog using Selenium
* util:                   Utility scripts (during development and/or application use)

To obtain the source code:
* latest:  git clone git@github.com:gmazza/tightblog.git
* releases: https://github.com/gmazza/tightblog/releases

To build the application (app/target/tightblog.war) with Maven and Java 8:
  `mvn clean install` from the TightBlog root.
  
It's *very* quick and simple to try out TightBlog locally, to determine if this is a product you would like to blog with
before proceeding with an actual install.  After building the distribution via `mvn clean install`, navigate to the `app` folder and run `mvn jetty:run`, 
and view http://localhost:8080/tightblog from a browser.  From there you can register an account, create a sample blog and some entries, 
create and view comments, modify templates, etc., etc., everything you can do with production TightBlog.  Each time it is run, 
"mvn jetty:run" creates a new in-memory temporary database that exists until you Ctrl-Z out of the terminal window running this command.
 
For actual installations on Tomcat or other servlet container, please read the <a href="https://github.com/gmazza/tightblog/wiki">Install pages</a> 
on the TightBlog Wiki.
