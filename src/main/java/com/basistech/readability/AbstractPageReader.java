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
