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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractPageReader {
    static final Logger LOG = LoggerFactory.getLogger(HttpPageReader.class);
    static final Charset UTF8 = Charset.forName("utf-8");

    private PageCharsetDetector charsetDetector;
    private Charset charset;
    private boolean serverReturnedEncoding;
    private boolean respectServerEncoding;
    private String detectedEncoding;

    protected String readContent(InputStream response, String forceEncoding) throws IOException {
        byte[] bytes = IOUtils.toByteArray(response);
        charset = null;
        String hint = null;
        if (forceEncoding != null) {
            serverReturnedEncoding = true;
            try {
                charset = Charset.forName(forceEncoding);
                hint = charset.name();
            } catch (Exception e) {
                //
            }
        }
        if (charsetDetector != null && !respectServerEncoding || charset == null) {
            String charsetName = charsetDetector.detect(bytes, hint);
            if (charsetName != null) {
                try {
                    charset = Charset.forName(charsetName);
                    detectedEncoding = charset.name();
                } catch (Exception e) {
                    LOG.warn("Detected character set " + charsetName + " not supported");
                }
            }
        }
        if (charset == null) {
            LOG.warn("Defaulting to utf-8");
            charset = UTF8;
        }
        return new String(bytes, charset);
    }

    public PageCharsetDetector getCharsetDetector() {
        return charsetDetector;
    }

    public void setCharsetDetector(PageCharsetDetector charsetDetector) {
        this.charsetDetector = charsetDetector;
    }

    public Charset getCharset() {
        return charset;
    }

    public boolean isServerReturnedEncoding() {
        return serverReturnedEncoding;
    }

    public void setRespectServerEncoding(boolean respectServerEncoding) {
        this.respectServerEncoding = respectServerEncoding;
    }

    public boolean isRespectServerEncoding() {
        return respectServerEncoding;
    }

    public String getDetectedEncoding() {
        return detectedEncoding;
    }

}
