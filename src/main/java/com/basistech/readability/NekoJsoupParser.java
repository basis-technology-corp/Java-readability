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
import java.io.StringReader;

import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import org.cyberneko.html.parsers.SAXParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Due to bugs in the Jsoup parser, we want a class that uses Neko to do the parse.
 * The same trick could be played with JSoup.
 */
public class NekoJsoupParser {
    private static final Logger LOG = LoggerFactory.getLogger(NekoJsoupParser.class);

    public NekoJsoupParser() {
        //
    }

    private final class LocalErrorHandler implements ErrorHandler {
        @Override
        public void error(SAXParseException e) throws SAXException {
            LOG.error("Parse error", e);
            throw e;
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            LOG.error("Parse error", e);
            throw e;
        }

        @Override
        public void warning(SAXParseException e) throws SAXException {
            LOG.warn("Parse warning", e);
        }
    }

    private class Handler extends DefaultHandler {
        private Document document;
        private Element currentElement;
        private int depth;
        Handler(Document document) {
            this.document = document;
        }
        @Override
        public void characters(char[] data, int start, int length) throws SAXException {
            assert currentElement != null;
            currentElement.appendText(new String(data, start, length));
        }
        @Override
        public void endDocument() throws SAXException {
            assert depth == 0;
        }
        @Override
        public void endElement(String uri, String localName, String qname) throws SAXException {
            LOG.debug("end element " + qname);
            currentElement = currentElement.parent();
            depth--;
        }
        @Override
        public void ignorableWhitespace(char[] data, int start, int length) throws SAXException {
            characters(data, start, length);
        }
        @Override
        public void startDocument() throws SAXException {
            currentElement = document;
        }
        @Override
        public void startElement(String uri, String localName, String qname, Attributes attrs) throws SAXException {
            LOG.debug("start element " + qname + " " + depth);
            Element newElement;
            newElement = currentElement.appendElement(localName);

            for (int ax = 0; ax < attrs.getLength(); ax++) {
                String name = attrs.getQName(ax);
                String value = attrs.getValue(ax);
                newElement.attr(name, value);
            }
            currentElement = newElement;
            depth++;
        }
    }

    public Document parse(InputStream data, String baseUri) throws SAXException, IOException {
        InputSource source = new InputSource();
        source.setByteStream(data);
        SAXParser nekoParser = new SAXParser();
        Document document = new Document(baseUri);
        nekoParser.setContentHandler(new Handler(document));
        nekoParser.setErrorHandler(new LocalErrorHandler());
        nekoParser.parse(source);
        return document;
    }

    public Document parse(String data, String baseUri) throws SAXException, IOException {
        InputSource source = new InputSource();
        source.setCharacterStream(new StringReader(data));
        SAXParser nekoParser = new SAXParser();
        Document document = new Document(baseUri);
        nekoParser.setContentHandler(new Handler(document));
        nekoParser.setErrorHandler(new LocalErrorHandler());
        nekoParser.parse(source);
        return document;
    }

    public Document parse(String data) throws SAXException, IOException {
	return Jsoup.parse(data);
    }
}
