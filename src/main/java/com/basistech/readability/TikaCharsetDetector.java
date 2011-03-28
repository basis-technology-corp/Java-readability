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
