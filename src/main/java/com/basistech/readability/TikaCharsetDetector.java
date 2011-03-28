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

import org.apache.tika.parser.txt.CharsetDetector;
import org.apache.tika.parser.txt.CharsetMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class TikaCharsetDetector implements PageCharsetDetector {
    static final Logger LOG = LoggerFactory.getLogger(TikaCharsetDetector.class);
    @Override
    public String detect(byte[] data, String hint) {
        CharsetDetector detector = new CharsetDetector();
        if (hint != null) {
            detector.setDeclaredEncoding(hint);
        }
        detector.setText(data);
        CharsetMatch match = detector.detect();
        return match.getName();
    }
}
