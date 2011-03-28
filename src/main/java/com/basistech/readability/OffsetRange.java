/******************************************************************************
 ** This data and information is proprietary to, and a valuable trade secret
 ** of, Basis Technology Corp.  It is given in confidence by Basis Technology
 ** and may only be used as permitted under the license agreement under which
 ** it has been distributed, and in no other way.
 **
 ** Copyright (c) 2009 Basis Technology Corporation All rights reserved.
 **
 ** The technical data and information provided herein are provided with
 ** `limited rights', and the computer software provided herein is provided
 ** with `restricted rights' as those terms are defined in DAR and ASPR
 ** 7-104.9(a).
 ******************************************************************************/
package com.basistech.readability;

import org.jsoup.nodes.TextNode;

/**
 * Object to relate a range of PC-data to a text node in an XOM tree.
 */
public class OffsetRange {
    private int start;
    private int end;
    private TextNode text;

    OffsetRange(int start, int end, TextNode text) {
        this.start = start;
        this.end = end;
        this.text = text;

        assert this.text == null || this.text.text().length() == this.end - this.start;
    }

    public String toString() {
        return super.toString() + "[" + this.start + "-" + this.end + " " + this.text.text() + "]";
    }

    public TextNode getText() {
        return text;
    }

    public int getEnd() {
        return end;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public void setText(TextNode text) {
        this.text = text;
    }

    public boolean offsetInRange(int offset) {
        return offset >= start && offset < end;
    }
}
