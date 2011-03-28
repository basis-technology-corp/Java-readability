/******************************************************************************
 * Copyright (c) 2010 Basis Technology Corp.
 * 
 * Basis Technology Corp. licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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
