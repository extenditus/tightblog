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
package org.apache.roller.weblogger.business.jpa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.roller.weblogger.business.FileContentManager;
import org.apache.roller.weblogger.business.MediaFileManager;
import org.apache.roller.weblogger.pojos.FileContent;
import org.apache.roller.weblogger.pojos.MediaDirectory;
import org.apache.roller.weblogger.pojos.MediaFile;
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.util.RollerMessages;

import javax.imageio.ImageIO;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class JPAMediaFileManagerImpl implements MediaFileManager {

    private final JPAPersistenceStrategy strategy;
    private final FileContentManager fileContentManager;

    private static Logger log = LoggerFactory.getLogger(JPAMediaFileManagerImpl.class);




    /**
     * Creates a new instance of MediaFileManagerImpl
     */
    protected JPAMediaFileManagerImpl(FileContentManager fcm, JPAPersistenceStrategy persistenceStrategy) {
        this.fileContentManager = fcm;
        this.strategy = persistenceStrategy;
    }

    @Override
    public void moveMediaFiles(Collection<MediaFile> mediaFiles, MediaDirectory targetDirectory) {

        List<MediaFile> moved = new ArrayList<>();
        moved.addAll(mediaFiles);

        for (MediaFile mediaFile : moved) {
            mediaFile.getDirectory().getMediaFiles().remove(mediaFile);

            mediaFile.setDirectory(targetDirectory);
            this.strategy.store(mediaFile);

            targetDirectory.getMediaFiles().add(mediaFile);
            this.strategy.store(targetDirectory);
        }

        // Refresh associated parent for changes
        strategy.flush();
        if (moved.size() > 0) {
            strategy.refresh(moved.get(0).getDirectory());
        }

        // Refresh associated parent for changes
        strategy.refresh(targetDirectory);
    }

    @Override
    public void moveMediaFile(MediaFile mediaFile, MediaDirectory targetDirectory) {
        moveMediaFiles(Collections.singletonList(mediaFile), targetDirectory);
    }

    @Override
    public MediaDirectory createMediaDirectory(Weblog weblog, String requestedName) {

        requestedName = requestedName.startsWith("/") ? requestedName.substring(1) : requestedName;

        if (requestedName.equals("") || requestedName.equals("default")) {
            // Default cannot be created using this method.
            // Use createDefaultMediaDirectory instead
            throw new IllegalArgumentException("Invalid media file directory name!");
        }

        MediaDirectory newDirectory;

        if (weblog.hasMediaDirectory(requestedName)) {
            throw new IllegalArgumentException("Directory exists");
        } else {
            newDirectory = new MediaDirectory(weblog, requestedName);
            this.strategy.store(newDirectory);
            this.strategy.flush();
            log.debug("Created new Directory {}", requestedName);
        }
        return newDirectory;
    }

    @Override
    public MediaDirectory createDefaultMediaDirectory(Weblog weblog) {
        MediaDirectory defaultDirectory = new MediaDirectory(weblog, "default");
        this.strategy.store(defaultDirectory);
        return defaultDirectory;
    }

    @Override
    public void createMediaFile(Weblog weblog, MediaFile mediaFile, RollerMessages errors) throws IOException {
        if (!fileContentManager.canSave(weblog, mediaFile.getName(),
                mediaFile.getContentType(), mediaFile.getLength(), errors)) {
            return;
        }
        strategy.store(mediaFile);

        // Refresh associated parent for changes
        strategy.flush();
        strategy.refresh(mediaFile.getDirectory());

        fileContentManager.saveFileContent(weblog, mediaFile.getId(), mediaFile.getInputStream());

        if (mediaFile.isImageFile()) {
            updateThumbnail(mediaFile);
        }
    }

    private void updateThumbnail(MediaFile mediaFile) {
        try {
            FileContent fc = fileContentManager.getFileContent(mediaFile.getDirectory().getWeblog(), mediaFile.getId());
            BufferedImage img;

            img = ImageIO.read(fc.getInputStream());

            // determine and save width and height
            mediaFile.setWidth(img.getWidth());
            mediaFile.setHeight(img.getHeight());
            strategy.store(mediaFile);

            int newWidth = mediaFile.getThumbnailWidth();
            int newHeight = mediaFile.getThumbnailHeight();

            // create thumbnail image
            Image newImage = img.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
            BufferedImage tmp = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = tmp.createGraphics();
            g2.drawImage(newImage, 0, 0, newWidth, newHeight, null);
            g2.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(tmp, "png", baos);

            fileContentManager.saveFileContent(mediaFile.getDirectory().getWeblog(), mediaFile.getId()
                    + "_sm", new ByteArrayInputStream(baos.toByteArray()));

            strategy.flush();
            // Refresh associated parent for changes
            strategy.refresh(mediaFile.getDirectory());

        } catch (Exception e) {
            log.debug("ERROR creating thumbnail", e);
        }
    }

    @Override
    public void updateMediaFile(Weblog weblog, MediaFile mediaFile) {
        mediaFile.setLastUpdated(new Timestamp(System.currentTimeMillis()));
        strategy.store(mediaFile);

        strategy.flush();
        // Refresh associated parent for changes
        strategy.refresh(mediaFile.getDirectory());

        updateWeblogLastModifiedDate(weblog);
    }

    @Override
    public void updateMediaFile(Weblog weblog, MediaFile mediaFile, InputStream is) throws IOException {
        mediaFile.setLastUpdated(new Timestamp(System.currentTimeMillis()));
        strategy.store(mediaFile);

        strategy.flush();
        // Refresh associated parent for changes
        strategy.refresh(mediaFile.getDirectory());

        updateWeblogLastModifiedDate(weblog);

        RollerMessages msgs = new RollerMessages();
        if (!fileContentManager.canSave(weblog, mediaFile.getName(),
                mediaFile.getContentType(), mediaFile.getLength(), msgs)) {
            throw new IOException(msgs.toString());
        }
        fileContentManager.saveFileContent(weblog, mediaFile.getId(), is);

        if (mediaFile.isImageFile()) {
            updateThumbnail(mediaFile);
        }
    }

    @Override
    public MediaFile getMediaFile(String id) throws IOException {
        return getMediaFile(id, false);
    }

    @Override
    public MediaFile getMediaFile(String id, boolean includeContent) throws IOException {
        MediaFile mediaFile = this.strategy.load(MediaFile.class, id);
        if (includeContent) {
            FileContent content = fileContentManager.getFileContent(mediaFile.getDirectory().getWeblog(), id);
            mediaFile.setContent(content);

            try {
                FileContent thumbnail = fileContentManager.getFileContent(mediaFile.getDirectory().getWeblog(),
                        id + "_sm");
                mediaFile.setThumbnailContent(thumbnail);
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.debug("Cannot load thumbnail for image {}", id, e);
                } else {
                    log.warn("Cannot load thumbnail for image {}", id);
                }
            }
        }
        return mediaFile;
    }

    @Override
    public MediaDirectory getMediaDirectoryByName(Weblog weblog, String name) {

        name = name.startsWith("/") ? name.substring(1) : name;

        log.debug("Looking up weblog|media file directory {}|{}", weblog.getHandle(), name);

        TypedQuery<MediaDirectory> q = this.strategy
                .getNamedQuery("MediaDirectory.getByWeblogAndName", MediaDirectory.class);
        q.setParameter(1, weblog);
        q.setParameter(2, name);
        try {
            return q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public MediaDirectory getMediaDirectory(String id) {
        return this.strategy.load(MediaDirectory.class, id);
    }

    @Override
    public MediaDirectory getDefaultMediaDirectory(Weblog weblog) {
        MediaDirectory temp = getMediaDirectoryByName(weblog, "default");
        if (temp == null) {
            throw new IllegalStateException("Required default Media Directory for Weblog: " + weblog.getHandle() + " is missing.");
        }
        return temp;
    }

    @Override
    public List<MediaDirectory> getMediaDirectories(Weblog weblog) {
        TypedQuery<MediaDirectory> q = this.strategy.getNamedQuery("MediaDirectory.getByWeblog",
                MediaDirectory.class);
        q.setParameter(1, weblog);
        return q.getResultList();
    }

    @Override
    public void removeMediaFile(Weblog weblog, MediaFile mediaFile) {
        this.strategy.remove(mediaFile);
        // Refresh associated parent for changes
        strategy.refresh(mediaFile.getDirectory());

        try {
            fileContentManager.deleteFile(weblog, mediaFile.getId());
            // Now thumbnail
            fileContentManager.deleteFile(weblog, mediaFile.getId() + "_sm");
        } catch (Exception e) {
            log.debug("File to be deleted already unavailable in the file store");
        }
    }

    public void removeAllFiles(Weblog weblog) {
        List<MediaDirectory> list = getMediaDirectories(weblog);

        for (MediaDirectory directory : list) {
            removeMediaDirectory(directory);
        }
    }

    public void removeMediaDirectory(MediaDirectory dir) {
        if (dir == null) {
            return;
        }

        Set<MediaFile> files = dir.getMediaFiles();
        for (MediaFile mf : files) {
            try {
                fileContentManager.deleteFile(dir.getWeblog(), mf.getId());
                // Now thumbnail
                fileContentManager.deleteFile(dir.getWeblog(), mf.getId() + "_sm");
            } catch (Exception e) {
                log.debug("File to be deleted already unavailable in the file store");
            }
            this.strategy.remove(mf);
        }

        strategy.remove(dir);
        strategy.flush();
        strategy.refresh(dir.getWeblog());
    }

    private void updateWeblogLastModifiedDate(Weblog weblog) {
        weblog.invalidateCache();
        strategy.store(weblog);
    }
}
