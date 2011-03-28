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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

/**
 * Maintains map between PC-DATA offsets and Text nodes in an XML document. Provides some structure for the
 * process of pulling the data out.
 */
public abstract class XmlDataMap {
    protected char forceSentenceChar = '\u2029'; // paragraph

    /**
     * Classify elements in the tree.
     */
    public enum ElementAction {
        /**
         * Ignore text under here.
         */
        Banned,
        /**
         * Not currently used.
         */
        Alt,
        /**
         * Insert whitespace.
         */
        Whitespace,
        /**
         * Treat as sentence boundary.
         */
        Sentence,
        /**
         * Remember where this was.
         */
        Mark;
    }

    public static class Mark {
        private String tag;
        private int offset;

        /**
         * * @return Returns the tag.
         */
        public String getTag() {
            return tag;
        }

        /**
         * @param tag The tag to set.
         */
        public void setTag(String tag) {
            this.tag = tag;
        }

        /**
         * * @return Returns the offset.
         */
        public int getOffset() {
            return offset;
        }

        /**
         * @param offset The offset to set.
         */
        public void setOffset(int offset) {
            this.offset = offset;
        }
    }

    protected StringBuffer pcDataBuffer;
    private List<Mark> marks;
    // CHECKSTYLE:OFF
    private LinkedList<OffsetRange> offsetRanges;
    // CHECKSTYLE:ON
    private int pcDataOffset;
    private boolean justAppendedSpace;
    private boolean justAppendedPeriod;

    private ListIterator<OffsetRange> optimizedListPointer;

    private OffsetRange optimizedRangeListElement;

    protected XmlDataMap() {
        offsetRanges = new LinkedList<OffsetRange>();
        pcDataOffset = 0;
        pcDataBuffer = new StringBuffer();
        justAppendedSpace = false;
        justAppendedPeriod = false;
        marks = new ArrayList<Mark>();
    }

    public OffsetRange findOffsetRangeForOffset(int offset) {
        if (optimizedRangeListElement.offsetInRange(offset)) {
            return optimizedRangeListElement;
        } else if (offset > optimizedRangeListElement.getStart()) {
            while (optimizedRangeListElement.getStart() < offset && optimizedListPointer.hasNext()) {
                optimizedRangeListElement = optimizedListPointer.next();
                if (optimizedRangeListElement.offsetInRange(offset)) {
                    return optimizedRangeListElement;
                }
            }
            throw new RuntimeException("Offset " + offset + " beyond last range");
        } else {
            // we don't expect to exercise this case.
            // has to be smaller, no?
            while (offset < optimizedRangeListElement.getStart() && optimizedListPointer.hasPrevious()) {
                optimizedRangeListElement = optimizedListPointer.previous();
                if (optimizedRangeListElement.offsetInRange(offset)) {
                    return optimizedRangeListElement;
                }
            }
            throw new RuntimeException("Offset " + offset + " before the first offset");
        }
    }

    /**
     * Retrieve the offset ranges for the text nodes of the original tree.
     *
     * @return the ranges.
     */
    public List<OffsetRange> getOffsetRanges() {
        return offsetRanges;
    }

    public String getPcData() {
        return pcDataBuffer.toString();
    }

    /**
     * Retrieve the accumulated pc data.
     *
     * @return
     */
    public StringBuffer getPcDataBuffer() {
        return pcDataBuffer;
    }

    /**
     * If we need to split a range for annotation, we want to keep the map of offset ranges usable. Note that
     * the caller has to revalidate or maintain any indices it has grabbed for ranges after the one we are
     * splitting. Note that this does not insert the new text node into the parent contents, the caller does
     * that.
     *
     * @param range
     * @param splitPoint
     * @return
     */
    public TextNode splitText(int rangePoint, int splitPoint) {
        assert splitPoint > 0;
        OffsetRange range = offsetRanges.get(rangePoint);
        assert splitPoint < range.getText().text().length();
        TextNode newText = new TextNode(range.getText().text().substring(splitPoint), null);
        range.getText().text(range.getText().text().substring(0, splitPoint));
        OffsetRange newRange = new OffsetRange(range.getStart() + splitPoint, range.getEnd(), newText);
        offsetRanges.add(rangePoint + 1, newRange);
        range.setEnd(splitPoint + range.getStart());
        assert range.getText().text().length() == range.getEnd() - range.getStart();
        return newText;
    }

    /**
     * A subclass may process metadata into the pc-data stream by calling this directly.
     *
     * @param textObject
     * @param text
     */
    protected void append(TextNode textObject, String text) {
        // if an entire Text element is whitespace, chances are that it's <div>NL noise. We don't need it.
        boolean spaceText = text.matches("[\\s]*");
        if (spaceText && justAppendedSpace) {
            return;
        }
        if (spaceText) {
            justAppendedSpace = true;
        }

        OffsetRange offsetRange = new OffsetRange(pcDataOffset, pcDataOffset + text.length(), textObject);
        pcDataBuffer.append(text);
        pcDataOffset += text.length();
        justAppendedSpace = Character.isWhitespace(text.charAt(text.length() - 1));
        justAppendedPeriod = eosPunctuation(lastNonWhitepaceCharacter(text));
        offsetRanges.add(offsetRange);
    }

    protected char lastNonWhitepaceCharacter(String text) {
        for (int index = text.length() - 1; index >= 0; index--) {
            char c = text.charAt(index);
            if (!Character.isWhitespace(c)) {
                return c;
            }
        }
        return '\ufeff'; // it won't count as punctuation
    }

    //SK: allow quotes to be considered as EOS punctuation, so that we don't 
    //  add extra punctuation to sentences ending with quotes. This isn't 
    //  entirely unicode-friendly, and we may want to fix that someday.
    private static boolean eosPunctuation(char c) {
        String s = "!?.\u2029\"\u0027\u2018\u2019\u201c\u201d"; 
        return s.indexOf(c) != -1;
    }

    protected void appendPeriod() {
        int startPcDataOffset = pcDataOffset;
        if (!justAppendedSpace && !justAppendedPeriod) {
            String appendMe = " . " + System.getProperty("line.separator");
            pcDataBuffer.append(appendMe);
            pcDataOffset += appendMe.length();
            justAppendedPeriod = true;
            justAppendedSpace = true;
        } else if (!justAppendedPeriod) {
            String appendMe = ". " + System.getProperty("line.separator");
            pcDataBuffer.append(appendMe);
            pcDataOffset += appendMe.length();
            justAppendedPeriod = true;
            justAppendedSpace = true;
        } else if (!justAppendedSpace) {
            String appendMe = " " + System.getProperty("line.separator");
            pcDataBuffer.append(appendMe);
            pcDataOffset += appendMe.length();
            justAppendedPeriod = true;
            justAppendedSpace = true;
        }
        // we make a range so that the code can tell the difference between 'spurious, added, period'
        // and 'bug that failed to make an offset range.'
        OffsetRange offsetRange = new OffsetRange(startPcDataOffset, pcDataOffset, null);
        offsetRanges.add(offsetRange);

    }

    protected void appendSpace() {
        if (!justAppendedSpace && !justAppendedPeriod) {
            justAppendedSpace = true;
            pcDataBuffer.append(' ');
            pcDataOffset++;
        }
    }

    protected abstract ElementAction classifyElement(Element element);

    public void process(Element rootElement) {
        recurse(rootElement);
        optimizedListPointer = offsetRanges.listIterator();
        optimizedRangeListElement = offsetRanges.getFirst();
    }

    private void recurse(Element element) {
        ElementAction action = classifyElement(element);
        if (action == ElementAction.Whitespace || action == ElementAction.Sentence) {
            appendSpace();
        }
        for (Node childNode : element.childNodes()) {
            // n.b., cdata not possible if we are coming from TagSoup. If we also handle
            // real xhtml by directly parsing it, then we have another story on our hands.
            // though we could use canonical XML to get rid of them.
            if (childNode instanceof TextNode && action != ElementAction.Banned) {
                TextNode textContent = (TextNode)childNode;
                String textString = textContent.text();
                append(textContent, textString);
            } else if (childNode instanceof Element) {
                recurse((Element)childNode);
            }
        }
        if (action == ElementAction.Whitespace) {
            appendSpace();
        } else if (action == ElementAction.Sentence) {
            appendPeriod();
        } else if (action == ElementAction.Mark) {
            Mark mark = new Mark();
            mark.setOffset(pcDataOffset);
            mark.setTag(element.tagName());
        }
    }

    /**
     * * @return Returns the marks.
     */
    public List<Mark> getMarks() {
        return marks;
    }

    public char getForceSentenceChar() {
        return forceSentenceChar;
    }

    public void setForceSentenceChar(char forceSentenceChar) {
        this.forceSentenceChar = forceSentenceChar;
    }
}
