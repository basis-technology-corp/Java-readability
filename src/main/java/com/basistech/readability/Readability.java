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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import org.xml.sax.SAXException;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Java version of the arclab readability javascript program. This uses jsoup to handle the DOM tree and
 * provide us with the sorts of operations that the javascript code loves. Make one of these objects for each
 * page. Provide it with an object to fetch more next pages to support that stuff.
 */
public class Readability {
    private static final Logger LOG = LoggerFactory.getLogger(Readability.class);
    private static final Set<String> DIV_TO_P_ELEMENTS;
    static {
        DIV_TO_P_ELEMENTS = new HashSet<String>();
        DIV_TO_P_ELEMENTS.add("a");
        DIV_TO_P_ELEMENTS.add("blockquote");
        DIV_TO_P_ELEMENTS.add("dl");
        DIV_TO_P_ELEMENTS.add("div");
        DIV_TO_P_ELEMENTS.add("img");
        DIV_TO_P_ELEMENTS.add("ol");
        DIV_TO_P_ELEMENTS.add("p");
        DIV_TO_P_ELEMENTS.add("pre");
        DIV_TO_P_ELEMENTS.add("table");
        DIV_TO_P_ELEMENTS.add("ul");
    }
    private Document document;
    private Element body;
    private PageReader pageReader;
    private String givenUrl;
    private Set<String> parsedPages;
    private boolean impossible;
    private String title;
    private boolean stripUnlikelyCandidates = true;
    private boolean classWeight = true;
    private boolean cleanConditionally = true;
    private String nextPageLink;
    private String articleText;
    private boolean readAllPages;
    private boolean notFirstPage;
    private NekoJsoupParser nekoParser = new NekoJsoupParser();
    // for some testing and debugging purposes, obtain string reps of the XML we
    // got from parsing.
    private List<String> xmlImages;

    public Readability() {
        parsedPages = new HashSet<String>();
    }

    /**
     * Process the content of a page. This takes a String, since JSoup does not handle byte input. Caller has
     * to worry about charset detection and conversion.
     * 
     * @param url the initial url
     */
    public void processDocument(String url) throws PageReadException {
        // TODO: reset the results.
        impossible = false;
        givenUrl = url;
        nextPageLink = null;
        if (!notFirstPage) {
            xmlImages = new ArrayList<String>();
            title = null;
        }

        String content = pageReader.readPage(url);

        try {
            document = nekoParser.parse(content, url);
        } catch (SAXException e) {
            LOG.error("Failed to parse " + url, e);
            impossible = true;
            return;
        } catch (IOException e) {
            LOG.error("Failed to parse " + url, e);
            impossible = true;
            return;
        }

        init(); // this needs another name, it does all the work.
        if (readAllPages && nextPageLink != null) {
            try {
                String textSoFar = articleText;
                notFirstPage = true;
                processDocument(nextPageLink);
                if (articleText != null) {
                    articleText = textSoFar + articleText;
                }
            } finally {
                notFirstPage = false;
            }
        }
    }

    private void removeScripts() {
        Elements scripts = document.getElementsByTag("script");
        for (int i = scripts.size() - 1; i >= 0; i--) {
            Element e = scripts.get(i);
            String src = e.attr("src");
            if ("".equals(src) || (src.indexOf("readability") == -1 && src.indexOf("typekit") == -1)) {
                e.remove();
            }
        }
    }

    private void handleDoubleBr() {
        Elements doubleBrs = document.select("br + br");
        for (Element br : doubleBrs) {
            // we hope that there's a 'p' up there....
            Elements parents = br.parents();
            Element parent = null;
            for (Element aparent : parents) {
                if (aparent.tag().getName().equals("p")) {
                    parent = aparent;
                    break;
                }
            }
            if (parent == null) {
                parent = br.parent();
                parent.wrap("<p></p>");
            }
            // now it's safe to make the change.
            String inner = parent.html();
            inner = Patterns.REPLACE_BRS.matcher(inner).replaceAll("</p><p>");
            parent.html(inner);
        }
    }

    private void prepDocument() {
        /**
         * In some cases a body element can't be found (if the HTML is totally hosed for example) so we create
         * a new body node and append it to the document.
         */
        if (body == null) {
            body = document.appendElement("body");
        }

        body.attr("id", "readabilityBody");

        Elements frames = document.getElementsByTag("frame");
        if (frames.size() > 0) {
            LOG.error("Frames. Can't deal. Write code later to look at URLs and fetch");
            impossible = true;
            return;
        }

        Elements stylesheets = document.getElementsByTag("style");
        stylesheets.remove();
        stylesheets = document.select("link[rel='stylesheet']");
        stylesheets.remove();

        /* Turn all double br's into p's */
        /*
         * Note, this is pretty costly as far as processing goes. Maybe optimize later.
         */
        handleDoubleBr();
        fontsToSpans();
    }

    private void fontsToSpans() {
        Elements allFonts = document.getElementsByTag("font");
        for (Element fontElement : allFonts) {
            changeElementTag(fontElement, "span");
        }
    }

    private String normalizeTrailingSlash(String url) {
        return url.replaceAll("/$", "");
    }

    private void init() {
        removeScripts();
        convertNoscriptToDiv();
        // there should never be more than one ... */
        Elements bodies = document.getElementsByTag("body");
        if (bodies.size() > 1) {
            LOG.warn("More than one <body/>");
        }
        body = null;
        body = bodies.get(0);
        /*
         * Make sure this document is added to the list of parsed pages first, so we don't double up on the
         * first page
         */
        parsedPages.add(normalizeTrailingSlash(givenUrl));
        nextPageLink = findNextPageLink(body);
        if (!notFirstPage) {
            title = getArticleTitle();
        }
        prepDocument();

        Element articleContent = grabArticle(null);
        if (articleContent == null && !notFirstPage) {
            // this happens when the content of the page is very short.
            // we don't believe in super-short next pages.
            articleText = body.text();
        } else {
            xmlImages.add(articleContent.outerHtml());
            articleText = getDisplayText(articleContent);
        }
    }

    private void convertNoscriptToDiv() {
        Elements noscript = document.getElementsByTag("noscript");
        for (Element e : noscript) {
            changeElementTag(e, "div");
        }

    }

    private void setContentScore(Element node, double score) {
        node.attr("data-readability.contentScore", Double.toString(score));
    }

    private boolean isElementScored(Element node) {
        return node.hasAttr("data-readability.contentScore");
    }

    private void incrementContentScore(Element node, double score) {
        node.attr("data-readability.contentScore", Double.toString(getContentScore(node) + score));
    }

    private double getContentScore(Element node) {
        String scoreString = node.attr("data-readability.contentScore");
        if ("".equals(scoreString)) {
            return 0;
        } else {
            return Double.parseDouble(scoreString);
        }
    }

    private void initializeNode(Element node) {
        // CHECKSTYLE:OFF
        node.attr("readability", "true");
        String tagName = node.tagName();
        if ("div".equals(tagName)) {
            incrementContentScore(node, 5);
        } else if ("pre".equals(tagName) || "td".equals(tagName) || "blockquote".equals(tagName)) {
            incrementContentScore(node, 3);
        } else if ("address".equals(tagName) || "ol".equals(tagName) || "ul".equals(tagName)
                   || "dl".equals(tagName) || "dd".equals(tagName) || "dt".equals(tagName)
                   || "li".equals(tagName) || "form".equals(tagName)) {
            incrementContentScore(node, -3);
        } else if (tagName.matches("h[1-6]") || "th".equals(tagName)) {
            incrementContentScore(node, -5);
        }
        incrementContentScore(node, getClassWeight(node));
        // CHECKSTYLE:ON
    }

    /**
     * Get an elements class/id weight. Uses regular expressions to tell if this element looks good or bad.
     * 
     * @param Element
     * @return number (Integer)
     **/
    private double getClassWeight(Element e) {
        if (!classWeight) {
            return 0;
        }

        int weight = 0;

        /* Look for a special classname */
        String className = e.className();
        if (!"".equals(className)) {
            if (Patterns.exists(Patterns.NEGATIVE, className)) {
                weight -= 25;
            }
            if (Patterns.exists(Patterns.POSITIVE, className)) {
                weight += 25;
            }
        }

        /* Look for a special ID */
        String id = e.id();
        if (!"".equals(id)) {
            if (Patterns.exists(Patterns.NEGATIVE, id)) {
                weight -= 25;
            }
            if (Patterns.exists(Patterns.POSITIVE, id)) {
                weight += 25;
            }
        }
        return weight;
    }

    private Element changeElementTag(Element e, String newTag) {
        Element newElement = document.createElement(newTag);
        /* JSoup gives us the live child list, so we need to make a copy. */
        List<Node> copyOfChildNodeList = new ArrayList<Node>();
        copyOfChildNodeList.addAll(e.childNodes());
        for (Node n : copyOfChildNodeList) {
            n.remove();
            newElement.appendChild(n);
        }
        e.replaceWith(newElement);
        return newElement;
    }

    // CHECKSTYLE:OFF
    private Element grabArticle(Element pageElement) {
        boolean isPaging = pageElement != null;

        if (pageElement == null) {
            pageElement = body;
        }

        String pageCacheHtml = pageElement.html();
        Elements allElements = pageElement.getAllElements();
        /*
         * Note: in Javascript, this list would be *live*. If you deleted a node from the tree, it and its
         * children would remove themselves. To get the same effect, we make a linked list and we remove
         * things from it. This won't win prizes for speed, but, then again, the code in Javascript has to be
         * doing something nearly as awful.
         */
        LinkedList<Element> allElementsList = new LinkedList<Element>();
        allElementsList.addAll(allElements);

        /**
         * First, node prepping. Trash nodes that look cruddy (like ones with the class name "comment", etc),
         * and turn divs into P tags where they have been used inappropriately (as in, where they contain no
         * other block level elements.) Note: Assignment from index for performance. See
         * http://www.peachpit.com/articles/article.aspx?p=31567&seqNum=5 TODO: Shouldn't this be a reverse
         * traversal?
         **/
        List<Element> nodesToScore = new ArrayList<Element>();
        ListIterator<Element> elIterator = allElementsList.listIterator();
        Set<Element> goodAsDead = new HashSet<Element>();
        while (elIterator.hasNext()) {
            Element node = elIterator.next();
            if (goodAsDead.contains(node)) {
                continue;
            }

            /* Remove unlikely candidates */
            if (stripUnlikelyCandidates) {
                String unlikelyMatchString = node.className() + node.id();
                if (Patterns.exists(Patterns.UNLIKELY_CANDIDATES, unlikelyMatchString)
                    && !Patterns.exists(Patterns.OK_MAYBE_ITS_A_CANDIDATE, unlikelyMatchString)
                    && !"body".equals(node.tagName())) {
                    LOG.debug("Removing unlikely candidate - " + unlikelyMatchString);
                    List<Element> toRemoveAndBelow = node.getAllElements();
                    elIterator.remove();
                    /*
                     * adding 'node' to that set is harmless and reduces the code complexity here.
                     */
                    goodAsDead.addAll(toRemoveAndBelow);
                    continue;
                }
            }

            if ("p".equals(node.tagName()) || "td".equals(node.tagName()) || "pre".equals(node.tagName())) {
                nodesToScore.add(node);
            }

            /*
             * Turn all divs that don't have children block level elements into p's
             */
            if ("div".equals(node.tagName())) {
                boolean hasBlock = false;
                for (Element divChild : node.getAllElements()) {
                    if (divChild != node) {
                        if (DIV_TO_P_ELEMENTS.contains(divChild.tagName())) {
                            hasBlock = true;
                            break;
                        }
                    }
                }
                if (!hasBlock) {
                    Element newElement = changeElementTag(node, "p");
                    nodesToScore.remove(node);
                    nodesToScore.add(newElement);
                } else {
                    /* EXPERIMENTAL *//*
                                       * grab just child text and wrap each chunk in a p
                                       */
                    int limit = node.childNodes().size();
                    for (int i = 0; i < limit; i++) {
                        Node childNode = node.childNodes().get(i);
                        if (childNode instanceof TextNode) {
                            Element p = document.createElement("p");
                            p.attr("basisInline", "true");
                            p.html(((TextNode)childNode).text());
                            childNode.replaceWith(p);
                        }
                    }
                }
            }
        }

        /**
         * Loop through all paragraphs, and assign a score to them based on how content-y they look. Then add
         * their score to their parent node. A score is determined by things like number of commas, class
         * names, etc. Maybe eventually link density.
         **/
        List<Element> candidates = new ArrayList<Element>();
        for (Element nodeToScore : nodesToScore) {
            Element parentNode = nodeToScore.parent();
            if (null == parentNode) { // might be an orphan whose parent was
                // dropped previously.
                continue;
            }
            Element grandParentNode = parentNode.parent();
            if (grandParentNode == null) {
                continue; // ditto
            }
            String innerText = nodeToScore.text();

            /*
             * If this paragraph is less than 25 characters, don't even count it.
             */
            if (innerText.length() < 25) {
                continue;
            }

            /* Initialize readability data for the parent. */
            if ("".equals(parentNode.attr("readability"))) {
                initializeNode(parentNode);
                candidates.add(parentNode);
            }

            /* Initialize readability data for the grandparent. */
            /*
             * If the grandparent has no parent, we don't want it as a candidate. It's probably a symptom that
             * we're operating in an orphan.
             */
            if (grandParentNode.parent() != null && "".equals(grandParentNode.attr("readability"))) {
                initializeNode(grandParentNode);
                candidates.add(grandParentNode);
            }

            double contentScore = 0;

            /* Add a point for the paragraph itself as a base. */
            contentScore++;

            /* Add points for any commas within this paragraph */
            contentScore += innerText.split(",").length;

            /*
             * For every 100 characters in this paragraph, add another point. Up to 3 points.
             */
            contentScore += Math.min(Math.floor(innerText.length() / 100.0), 3.0);

            /* Add the score to the parent. The grandparent gets half. */
            incrementContentScore(parentNode, contentScore);

            if (grandParentNode != null) {
                incrementContentScore(grandParentNode, contentScore / 2.0);
            }
        }

        /**
         * After we've calculated scores, loop through all of the possible candidate nodes we found and find
         * the one with the highest score.
         **/
        Element topCandidate = null;
        for (Element candidate : candidates) {
            /**
             * Scale the final candidates score based on link density. Good content should have a relatively
             * small link density (5% or less) and be mostly unaffected by this operation.
             **/
            double score = getContentScore(candidate);
            double newScore = score * (1.0 - getLinkDensity(candidate));
            setContentScore(candidate, newScore);
            LOG.debug("Candidate [" + candidate.getClass() + "] (" + candidate.className() + ":"
                      + candidate.id() + ") with score " + newScore);

            if (null == topCandidate || newScore > getContentScore(topCandidate)) {
                topCandidate = candidate;
            }
        }

        /**
         * If we still have no top candidate, just use the body as a last resort. We also have to copy the
         * body node so it is something we can modify.
         **/
        if (topCandidate == null || topCandidate == body) {
            topCandidate = document.createElement("div");
            // not efficient but not likely.
            topCandidate.html(pageElement.html());
            pageElement.html("");
            pageElement.appendChild(topCandidate);
            initializeNode(topCandidate);
        }

        /**
         * Now that we have the top candidate, look through its siblings for content that might also be
         * related. Things like preambles, content split by ads that we removed, etc.
         **/
        Element articleContent = document.createElement("div");
        if (isPaging) {
            articleContent.attr("id", "readability-content");
        }
        double siblingScoreThreshold = Math.max(10, getContentScore(topCandidate) * 0.2);
        List<Element> siblingNodes = topCandidate.siblingElements();

        for (Element siblingNode : siblingNodes) {
            boolean scored = isElementScored(siblingNode);

            boolean append = false;

            LOG.debug("Looking at sibling node: [" + siblingNode.getClass() + "] (" + siblingNode.className()
                      + ":" + siblingNode.id() + ")");
            if (scored) {
                LOG.debug("Sibling has score " + getContentScore(siblingNode));
            } else {
                LOG.debug("Sibling has score unknown");
            }

            if (siblingNode == topCandidate) {
                append = true;
            }

            double contentBonus = 0;
            /*
             * Give a bonus if sibling nodes and top candidates have the example same classname
             */
            if (siblingNode.className().equals(topCandidate.className())
                && !"".equals(topCandidate.className())) {
                contentBonus += getContentScore(topCandidate) * 0.2;
            }

            if (scored && (getContentScore(siblingNode) + contentBonus >= siblingScoreThreshold)) {
                append = true;
            }

            if ("p".equals(siblingNode.tagName())) {
                double linkDensity = getLinkDensity(siblingNode);
                String nodeContent = siblingNode.text();
                int nodeLength = nodeContent.length();

                if (nodeLength > 80 && linkDensity < 0.25) {
                    append = true;
                } else if (nodeLength < 80 && linkDensity == 0
                           && Patterns.exists(Patterns.ENDS_WITH_DOT, nodeContent)) {
                    append = true;
                }
            }

            if (append) {
                LOG.debug("Appending node: [" + siblingNode.getClass() + "]");

                Element nodeToAppend = null;
                if (!"div".equals(siblingNode.tagName()) && !"p".equals(siblingNode.tagName())) {
                    /*
                     * We have a node that isn't a common block level element, like a form or td tag. Turn it
                     * into a div so it doesn't get filtered out later by accident.
                     */

                    LOG.debug("Altering siblingNode of " + siblingNode.tagName() + " to div.");
                    nodeToAppend = changeElementTag(siblingNode, "div");
                } else {
                    nodeToAppend = siblingNode;
                }

                /*
                 * To ensure a node does not interfere with readability styles, remove its classnames
                 */
                nodeToAppend.removeAttr("class");

                /*
                 * Append sibling and subtract from our list because it removes the node when you append to
                 * another node
                 */
                articleContent.appendChild(nodeToAppend);
            }
        }

        document.body().empty();
        document.body().appendChild(articleContent);

        /**
         * So we have all of the content that we need. Now we clean it up for presentation.
         **/
        prepArticle(articleContent);

        /**
         * Now that we've gone through the full algorithm, check to see if we got any meaningful content. If
         * we didn't, we may need to re-run grabArticle with different flags set. This gives us a higher
         * likelihood of finding the content, and the sieve approach gives us a higher likelihood of finding
         * the -right- content.
         **/
        if (articleContent.text().length() < 250) {
            pageElement.html(pageCacheHtml);
            if (stripUnlikelyCandidates) {
                try {
                    stripUnlikelyCandidates = false;
                    return grabArticle(pageElement);
                } finally {
                    stripUnlikelyCandidates = true;
                }
            } else if (classWeight) {
                try {
                    classWeight = false;
                    return grabArticle(pageElement);
                } finally {
                    classWeight = true;
                }
            } else if (cleanConditionally) {
                try {
                    cleanConditionally = false;
                    return grabArticle(pageElement);
                } finally {
                    cleanConditionally = true;
                }
            } else {
                return null;
            }
        }

        return articleContent;
    }

    private String getDisplayText(Element e) {
        HtmlPage htmlPage = new HtmlPage();
        htmlPage.process(document);
        String thisText = htmlPage.getPcData();
        LOG.debug("Text: " + thisText);
        return thisText;
    }

    /**
     * Clean an element of all tags of type "tag" if they look fishy. "Fishy" is an algorithm based on content
     * length, classnames, link density, number of images & embeds, etc.
     * 
     * @return void
     **/
    private void cleanConditionally(Element e, String tag) {

        if (!cleanConditionally) {
            return;
        }

        Elements tagsList = e.getElementsByTag(tag);
        int curTagsLength = tagsList.size();

        /**
         * Gather counts for other typical elements embedded within. Traverse backwards so we can remove nodes
         * at the same time without effecting the traversal. TODO: Consider taking into account original
         * contentScore here.
         **/
        for (int i = curTagsLength - 1; i >= 0; i--) {
            Element ee = tagsList.get(i);
            if (ee.ownerDocument() == null) {
                continue; // it a child of something we've already killed, so it
                // has no document.
            }
            double weight = getClassWeight(ee);
            double contentScore = getContentScore(ee);

            LOG.debug("Cleaning Conditionally [" + ee.getClass() + "] (" + ee.className() + ":" + ee.id()
                      + ")" + contentScore);

            if (weight + contentScore < 0) {
                LOG.debug("Negative content score");
                ee.remove();
            } else if (getCharCount(ee, ',') < 10) {
                /**
                 * If there are not very many commas, and the number of non-paragraph elements is more than
                 * paragraphs or other ominous signs, remove the element.
                 **/
                int p = ee.getElementsByTag("p").size();
                int img = ee.getElementsByTag("img").size();
                int li = ee.getElementsByTag("li").size() - 100;
                int input = ee.getElementsByTag("input").size();

                Elements embeds = ee.getElementsByTag("embed");
                int embedCount = embeds.size();
                // removed code that pays specific attention to youtube.
                double linkDensity = getLinkDensity(ee);
                int contentLength = ee.text().length();
                boolean toRemove = false;

                if (img > p) {
                    toRemove = true;
                } else if (li > p && !"ul".equals(tag) && !"ol".equals(tag)) {
                    toRemove = true;
                } else if (input > Math.floor(p / 3)) {
                    toRemove = true;
                } else if (contentLength < 25 && (img == 0 || img > 2)) {
                    toRemove = true;
                } else if (weight < 25 && linkDensity > 0.2) {
                    toRemove = true;
                } else if (weight >= 25 && linkDensity > 0.5) {
                    toRemove = true;
                } else if ((embedCount == 1 && contentLength < 75) || embedCount > 1) {
                    toRemove = true;
                }

                if (toRemove) {
                    LOG.debug("failed keep tests.");
                    ee.remove();
                }
            }
        }
    }

    /**
     * Clean out spurious headers from an Element. Checks things like classnames and link density.
     * 
     * @param Element
     * @return void
     **/
    private void cleanHeaders(Element e) {
        for (int headerIndex = 1; headerIndex < 3; headerIndex++) {
            Elements headers = e.getElementsByTag("h" + headerIndex);
            for (int i = headers.size() - 1; i >= 0; i--) {
                if (getClassWeight(headers.get(i)) < 0 || getLinkDensity(headers.get(i)) > 0.33) {
                    headers.get(i).remove();
                }
            }
        }
    }

    /**
     * Prepare the article node for display. Clean out any inline styles, iframes, forms, strip extraneous
     * <p>
     * tags, etc. This takes an element in, but returns a string.
     * 
     * @param Element
     * @return void
     **/
    private void prepArticle(Element articleContent) {
        // we don't need to do this, we don't care
        cleanStyles(articleContent);
        // this replaces any break element or an nbsp with a plain break
        // element.
        // not needed. We will deal with breaks as we deal with breaks
        // killBreaks(articleContent);

        /* Clean out junk from the article content */
        cleanConditionally(articleContent, "form");
        clean(articleContent, "object");
        clean(articleContent, "h1");

        /**
         * If there is only one h2, they are probably using it as a header and not a subheader, so remove it
         * since we already have a header.
         ***/
        if (articleContent.getElementsByTag("h2").size() == 1) {
            clean(articleContent, "h2");
        }
        clean(articleContent, "iframe");

        cleanHeaders(articleContent);

        /*
         * Do these last as the previous stuff may have removed junk that will affect these
         */
        cleanConditionally(articleContent, "table");
        cleanConditionally(articleContent, "ul");
        cleanConditionally(articleContent.child(0), "div");

        /* Remove extra paragraphs */
        Elements articleParagraphs = articleContent.getElementsByTag("p");
        for (Element para : articleParagraphs) {
            int imgCount = para.getElementsByTag("img").size();
            int embedCount = para.getElementsByTag("embed").size();
            int objectCount = para.getElementsByTag("object").size();

            if (imgCount == 0 && embedCount == 0 && objectCount == 0 && para.text().matches("\\s*")) {
                para.remove();
            }
        }

        Elements parasWithPreceedingBreaks = articleContent.getElementsByTag("br + p");
        for (Element pe : parasWithPreceedingBreaks) {
            Element brElement = pe.previousElementSibling();
            brElement.remove();
        }
    }

    private void cleanStyles(Element articleContent) {
        // we want to clear off the style attributes in case they influence
        // something else.
        for (Element e : articleContent.getAllElements()) {
            e.removeAttr("style");
        }
    }

    /**
     * Clean a node of all elements of type "tag".
     * 
     * @param Element
     * @param string tag to clean
     **/
    private void clean(Element e, String tag) {
        Elements targetList = e.getElementsByTag(tag);
        targetList.remove();
    }

    private double getLinkDensity(Element e) {
        Elements links = e.getElementsByTag("a");
        double textLength = e.text().length();
        double linkLength = 0;
        for (Element link : links) {
            linkLength += link.text().length();
        }

        return linkLength / textLength;
    }

    private String getArticleTitle() {
        String curTitle = "";
        String origTitle = "";

        Elements titleElements = document.getElementsByTag("title");
        if (titleElements.size() > 0) {
            if (titleElements.size() > 1) {
                LOG.warn("More than one title.");
            }
            curTitle = titleElements.get(0).text();
            origTitle = curTitle;
        }

        if (Patterns.exists(Patterns.BAR_DASH, curTitle)) {
            curTitle = origTitle.replaceAll("(.*)[\\|\\-] .*", "$1");
            if (curTitle.split(" ").length < 3) {
                curTitle = origTitle.replaceAll("[^\\|\\-]*[\\|\\-](.*)", "$1");
            }
        } else if (curTitle.indexOf(": ") != -1) {
            curTitle = origTitle.replaceAll(".*:(.*)", "$1");

            if (curTitle.split(" ").length < 3) {
                curTitle = origTitle.replaceAll("[^:]*[:](.*)", "$1");
            }
        } else if (curTitle.length() > 150 || curTitle.length() < 15) {
            Elements hOnes = document.getElementsByTag("h1");
            if (hOnes.size() == 1) {
                curTitle = hOnes.get(0).text();
            }
        }

        curTitle = curTitle.trim();

        if (curTitle.split(" ").length <= 4) {
            curTitle = origTitle;
        }
        return curTitle;
    }

    private String findBaseUrl(String stringUrl) {
        try {
            URI base = findBaseUrl0(stringUrl);
            return base.toString();
        } catch (URISyntaxException e) {
            LOG.debug("Failed to get base URI", e);
            return null;
        }
    }

    private URI findBaseUrl0(String stringUrl) throws URISyntaxException {
        //Compensate for Windows path names. 
    	stringUrl = stringUrl.replace("\\", "/");
    	int qindex = stringUrl.indexOf("?");
        if (qindex != -1) {
            // stuff after the ? tends to make the Java URL parser burp.
            stringUrl = stringUrl.substring(0, qindex);
        }
        URI url = new URI(stringUrl);
        URI baseUrl = new URI(url.getScheme(), url.getAuthority(), url.getPath(), null, null);

        String path = baseUrl.getPath().substring(1); // toss the leading /
        String[] pieces = path.split("/");
        List<String> urlSlashes = new ArrayList<String>();
        // reverse
        for (String piece : pieces) {
            urlSlashes.add(piece);
        }
        List<String> cleanedSegments = new ArrayList<String>();
        String possibleType = "";
        boolean del;

        for (int i = 0; i < urlSlashes.size(); i++) {
            String segment = urlSlashes.get(i);

            // Split off and save anything that looks like a file type.
            if (segment.indexOf(".") != -1) {
                possibleType = segment.split("\\.")[1];

                /*
                 * If the type isn't alpha-only, it's probably not actually a file extension.
                 */
                if (!possibleType.matches("[^a-zA-Z]")) {
                    segment = segment.split("\\.")[0];
                }
            }

            /**
             * EW-CMS specific segment replacement. Ugly. Example:
             * http://www.ew.com/ew/article/0,,20313460_20369436,00.html
             **/
            if (segment.indexOf(",00") != -1) {
                segment = segment.replaceFirst(",00", "");
            }

            // If our first or second segment has anything looking like a page
            // number, remove it.
            /* Javascript code has some /i's here, we might need to fiddle */
            Matcher pnMatcher = Patterns.PAGE_NUMBER_LIKE.matcher(segment);
            if (pnMatcher.matches() && ((i == 1) || (i == 0))) {
                segment = pnMatcher.replaceAll("");
            }

            del = false;

            /*
             * If this is purely a number, and it's the first or second segment, it's probably a page number.
             * Remove it.
             */
            if (i < 2 && segment.matches("^\\d{1,2}$")) {
                del = true;
            }

            /* If this is the first segment and it's just "index", remove it. */
            if (i == 0 && segment.toLowerCase() == "index")
                del = true;

            /*
             * If our first or second segment is smaller than 3 characters, and the first segment was purely
             * alphas, remove it.
             */
            /* /i again */
            if (i < 2 && segment.length() < 3 && !urlSlashes.get(0).matches("[a-z]"))
                del = true;

            /* If it's not marked for deletion, push it to cleanedSegments. */
            if (!del) {
                cleanedSegments.add(segment);
            }
        }

        String cleanedPath = "";
        for (String s : cleanedSegments) {
            cleanedPath = cleanedPath + s;
            cleanedPath = cleanedPath + "/";
        }
        URI cleaned = new URI(url.getScheme(), url.getAuthority(), "/"
                                                                   + cleanedPath.substring(0, cleanedPath
                                                                       .length() - 1), null, null);
        return cleaned;
    }

    /*
     * Officially parsing URL's from HTML pages is a mug's game.
     */

    private String getUrlHost(String url) {
        // httpx://host/.....

        int hostStart = url.indexOf("//");
        if (hostStart == -1) {
            return "";
        }
        int hostEnd = url.indexOf("/", hostStart + 2);
        if (hostEnd == -1) {
            return url.substring(hostStart + 2);
        } else {
            return url.substring(hostStart + 2, hostEnd);
        }

    }

    private String findNextPageLink(Element body) {
        Map<String, PageLinkInfo> possiblePages = new HashMap<String, PageLinkInfo>();
        Elements allLinks = body.getElementsByTag("a");
        String articleBaseUrl = findBaseUrl(givenUrl);
        String baseHost = getUrlHost(articleBaseUrl);

        /**
         * Loop through all links, looking for hints that they may be next-page links. Things like having
         * "page" in their textContent, className or id, or being a child of a node with a page-y className or
         * id. Also possible: levenshtein distance? longest common subsequence? After we do that, assign each
         * page a score, and
         **/
        for (Element link : allLinks) {
            String linkHref = link.attr("abs:href").replaceAll("#.*$", "").replaceAll("/$", "");

            /* If we've already seen this page, ignore it */
            if ("".equals(linkHref) || linkHref.equals(articleBaseUrl) || linkHref.equals(givenUrl)
                || parsedPages.contains(linkHref)) {
                continue;
            }

            String linkHost = getUrlHost(linkHref);

            /* If it's on a different domain, skip it. */
            if (!linkHost.equals(baseHost)) {
                continue;
            }

            String linkText = link.text(); // like innerText

            /* If the linkText looks like it's not the next page, skip it. */
            if (Patterns.EXTRANEOUS.matcher(linkText).matches() || linkText.length() > 25) {
                continue;
            }

            /*
             * If the leftovers of the URL after removing the base URL don't contain any digits, it's
             * certainly not a next page link.
             */
            String linkHrefLeftover = linkHref.replaceFirst(articleBaseUrl, "");
            if (!Patterns.exists(Patterns.DIGIT, linkHrefLeftover)) {
                continue;
            }

            PageLinkInfo linkObj = possiblePages.get(linkHref);
            if (linkObj == null) {
                linkObj = new PageLinkInfo(0.0, linkText, linkHref);
                possiblePages.put(linkHref, linkObj);
            } else {
                String newLinkText = linkObj.getLinkText() + " | " + linkText;
                linkObj.setLinkText(newLinkText);
            }

            /**
             * If the articleBaseUrl isn't part of this URL, penalize this link. It could still be the link,
             * but the odds are lower. Example:
             * http://www.actionscript.org/resources/articles/745/1/JavaScript
             * -and-VBScript-Injection-in-ActionScript-3/Page1.html
             **/
            if (linkHref.indexOf(articleBaseUrl) != 0) {
                linkObj.incrementScore(-25);
            }

            String linkData = linkText + " " + link.className() + " " + link.id();
            if (Patterns.exists(Patterns.NEXT_LINK, linkData)) {
                linkObj.incrementScore(50);
            }
            if (Patterns.exists(Patterns.PAGINATION, linkData)) {
                linkObj.incrementScore(25);
            }
            if (Patterns.exists(Patterns.FIRST_OR_LAST, linkData)) {
                // -65 is enough to negate any bonuses gotten from a > or » in
                // the text,
                /*
                 * If we already matched on "next", last is probably fine. If we didn't, then it's bad.
                 * Penalize.
                 */
                if (!Patterns.exists(Patterns.NEXT_LINK, linkObj.getLinkText())) {
                    linkObj.incrementScore(-65);
                }
            }

            if (Patterns.exists(Patterns.NEGATIVE, linkData)
                || Patterns.exists(Patterns.EXTRANEOUS, linkData)) {
                linkObj.incrementScore(-50);
            }
            if (Patterns.exists(Patterns.PREV_LINK, linkData)) {
                linkObj.incrementScore(-200);
            }

            /* If a parentNode contains page or paging or paginat */
            Element parentNode = link.parent();
            boolean positiveNodeMatch = false;
            boolean negativeNodeMatch = false;
            while (parentNode != null) {
                String parentNodeClassAndId = parentNode.className() + " " + parentNode.id();
                if (!positiveNodeMatch && Patterns.match(Patterns.PAGINATION, parentNodeClassAndId)) {
                    positiveNodeMatch = true;
                    linkObj.incrementScore(25);
                }
                if (!negativeNodeMatch && Patterns.match(Patterns.NEGATIVE, parentNodeClassAndId)) {
                    /*
                     * If this is just something like "footer", give it a negative. If it's something like
                     * "body-and-footer", leave it be.
                     */
                    if (!Patterns.exists(Patterns.POSITIVE, parentNodeClassAndId)) {
                        linkObj.incrementScore(-25);
                        negativeNodeMatch = true;
                    }
                }
                parentNode = parentNode.parent();
            }

            /**
             * If the URL looks like it has paging in it, add to the score. Things like /page/2/, /pagenum/2,
             * ?p=3, ?page=11, ?pagination=34
             **/
            if (Patterns.exists(Patterns.PAGE_AND_NUMBER, linkHref)
                || Patterns.exists(Patterns.PAGE_OR_PAGING, linkHref)) {
                linkObj.incrementScore(+25);
            }

            /* If the URL contains negative values, give a slight decrease. */
            if (Patterns.exists(Patterns.EXTRANEOUS, linkHref)) {
                linkObj.incrementScore(-15);
            }

            /**
             * Minor punishment to anything that doesn't match our current URL. NOTE: I'm finding this to
             * cause more harm than good where something is exactly 50 points. Dan, can you show me a
             * counterexample where this is necessary? if (linkHref.indexOf(window.location.href) !== 0) {
             * linkObj.score -= 1; }
             **/

            /**
             * If the link text can be parsed as a number, give it a minor bonus, with a slight bias towards
             * lower numbered pages. This is so that pages that might not have 'next' in their text can still
             * get scored, and sorted properly by score.
             **/
            boolean linkNumeric = false;
            int linkTextAsNumber = 0;

            try {
                linkTextAsNumber = Integer.parseInt(linkText);
                linkNumeric = true;
            } catch (NumberFormatException e) {
            }

            if (linkNumeric) {
                // Punish 1 since we're either already there, or it's probably
                // before what we want anyways.
                if (linkTextAsNumber == 1) {
                    linkObj.incrementScore(-10);
                } else {
                    // Todo: Describe this better
                    linkObj.incrementScore(Math.max(0, 10 - linkTextAsNumber));
                }
            }
        }

        /**
         * Loop through all of our possible pages from above and find our top candidate for the next page URL.
         * Require at least a score of 50, which is a relatively high confidence that this page is the next
         * link.
         **/
        PageLinkInfo topPage = null;
        for (Map.Entry<String, PageLinkInfo> pageEntry : possiblePages.entrySet()) {
            if (pageEntry.getValue().getScore() >= 50
                && (topPage == null || topPage.getScore() < pageEntry.getValue().getScore())) {
                topPage = pageEntry.getValue();
            }
        }

        if (topPage != null) {
            String nextHref = topPage.getHref().replaceFirst("/$", "");
            LOG.debug("Next page = " + nextHref);
            parsedPages.add(nextHref);
            return nextHref;
        } else {
            return null;
        }
    }

    /**
     * Get the number of times a string s appears in the node e.
     * 
     * @param Element
     * @param string - what to split on. Default is ","
     * @return number (integer)
     **/
    int getCharCount(Element e, char s) {
        return e.text().split(Character.toString(s)).length - 1;
    }

    public void setPageReader(PageReader pageReader) {
        this.pageReader = pageReader;
    }

    public PageReader getPageReader() {
        return pageReader;
    }

    public boolean isImpossible() {
        return impossible;
    }

    public String getNextPageLink() {
        return nextPageLink;
    }

    public String getTitle() {
        return title;
    }

    public String getArticleText() {
        return articleText;
    }

    public void setReadAllPages(boolean readAllPages) {
        this.readAllPages = readAllPages;
    }

    public boolean isReadAllPages() {
        return readAllPages;
    }

    public List<String> getXmlImages() {
        return xmlImages;
    }

}
