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

/**
 * Interface to reading HTML pages.
 */
public interface PageReader {
    /**
     * Read the content of a page. Return null and log if
     * there's some problem or another. This is responsible
     * for dealing with charset.
     * @param url
     * @return
     */
    String readPage(String url) throws PageReadException;
    /**
     * Provide a character set detector.
     * @param detector
     */
    void setCharsetDetector(PageCharsetDetector detector);
}
