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
