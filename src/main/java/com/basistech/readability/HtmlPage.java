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

import java.util.HashMap;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class HtmlPage extends XmlDataMap {
    public static final String KEY = "htmlPage";
    static Map<String, ElementAction> elementActionMap;
    static {
        elementActionMap = new HashMap<String, ElementAction>();
        elementActionMap.put("img", ElementAction.Alt);
        elementActionMap.put("applet", ElementAction.Alt);
        elementActionMap.put("area", ElementAction.Alt);
        elementActionMap.put("input", ElementAction.Alt);
        elementActionMap.put("script", ElementAction.Banned);
        elementActionMap.put("iframe", ElementAction.Banned);
        elementActionMap.put("style", ElementAction.Banned);
        elementActionMap.put("br", ElementAction.Whitespace);
        elementActionMap.put("p", ElementAction.Sentence);
        elementActionMap.put("hr", ElementAction.Sentence);
        elementActionMap.put("ul", ElementAction.Sentence);
        elementActionMap.put("h1", ElementAction.Sentence);
        elementActionMap.put("h2", ElementAction.Sentence);
        elementActionMap.put("h3", ElementAction.Sentence);
        elementActionMap.put("h4", ElementAction.Sentence);
        elementActionMap.put("h5", ElementAction.Sentence);
        elementActionMap.put("h6", ElementAction.Sentence);
        elementActionMap.put("pre", ElementAction.Sentence);
        elementActionMap.put("blockquote", ElementAction.Sentence);
        elementActionMap.put("title", ElementAction.Sentence);
        elementActionMap.put("div", ElementAction.Sentence);
        // hmm, span tags with CSS with certain properties? Hopeless.
        elementActionMap.put("center", ElementAction.Whitespace);
        elementActionMap.put("form", ElementAction.Sentence);
        elementActionMap.put("table", ElementAction.Sentence);
        elementActionMap.put("td", ElementAction.Sentence);
        elementActionMap.put("th", ElementAction.Sentence);
        elementActionMap.put("li", ElementAction.Sentence);
        elementActionMap.put("dir", ElementAction.Sentence);
        elementActionMap.put("menu", ElementAction.Sentence);
        elementActionMap.put("ol", ElementAction.Sentence);
    }

    // the data as formatted for RLP -- just the PC-DATA.
    private String pcData;
    private String mimeType;

    public HtmlPage() {
        super();
    }

    public void process(Document document) {
        Element body = document.body();
        if (body != null) { // page might have no body.
            process(body);
            pcData = pcDataBuffer.toString();
        }
    }

    public String getPcData() {
        return pcData;
    }

    @Override
    protected ElementAction classifyElement(Element element) {
        if (element.hasAttr("basisInline")) {
            return null;
        }
        return elementActionMap.get(element.tagName());
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
}
