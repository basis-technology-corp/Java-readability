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
