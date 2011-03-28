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
import java.io.StringReader;

import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import org.cyberneko.html.parsers.SAXParser;
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
}
