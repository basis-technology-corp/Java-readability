/******************************************************************************
 ** This data and information is proprietary to, and a valuable trade secret
 ** of, Basis Technology Corp.  It is given in confidence by Basis Technology
 ** and may only be used as permitted under the license agreement under which
 ** it has been distributed, and in no other way.
 **
 ** Copyright (c) 2010 Basis Technology Corporation All rights reserved.
 **
 ** The technical data and information provided herein are provided with
 ** `limited rights', and the computer software provided herein is provided
 ** with `restricted rights' as those terms are defined in DAR and ASPR
 ** 7-104.9(a).
 ******************************************************************************/

package com.basistech.readability;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.tika.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class FilePageReader extends AbstractPageReader implements PageReader {
    private static final Logger LOG = LoggerFactory.getLogger(FilePageReader.class);

    private File baseDirectory;

    /** {@inheritDoc} */
    @Override
    public String readPage(String url) throws PageReadException {
        int lastSlash = url.replace("\\", "/").lastIndexOf('/');
        File testFile = new File(baseDirectory, url.substring(lastSlash + 1));
        LOG.info("Reading " + testFile + " for " + url);
        FileInputStream fis = null;
        try {
            try {
                fis = new FileInputStream(testFile);
                return readContent(fis, null);
            } catch (IOException e) {
                throw new PageReadException("Failed to read " + url, e);
            }
        } finally {
            if (fis != null) {
                IOUtils.closeQuietly(fis);
            }
        }
    }

    public void setBaseDirectory(File baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    public File getBaseDirectory() {
        return baseDirectory;
    }

}
