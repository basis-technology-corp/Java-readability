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

import java.util.regex.Pattern;

/**
 *
 */
final class Patterns {

    static final Pattern PAGE_NUMBER_LIKE = ciPattern("((_|-)?p[a-z]*|(_|-))[0-9]{1,2}$");
    static final Pattern PAGE_AND_NUMBER = ciPattern("p(a|g|ag)?(e|ing|ination)?(=|/)[0-9]{1,2}");
    static final Pattern PAGE_OR_PAGING = ciPattern("(page|paging)");
    static final Pattern EXTRANEOUS = ciPattern("print|archive|comment|discuss|e[\\-]?mail|share|reply|all|login|sign|single");
    static final Pattern NEXT_LINK = ciPattern("(next|weiter|continue|>([^\\|]|$)|»([^\\|]|$))");
                    // Match: next, continue, >, >>, » but not >|, »| as those usually mean last."
    static final Pattern PAGINATION = ciPattern("pag(e|ing|inat)");
    static final Pattern FIRST_OR_LAST = ciPattern("(first|last)");
    static final Pattern NEGATIVE = ciPattern("(combx|comment|com-|contact|foot|footer|footnote|masthead|media|meta|outbrain|promo|related|scroll|shoutbox|sidebar|sponsor|shopping|tags|tool|widget)");
    static final Pattern PREV_LINK = ciPattern("(prev|earl|old|new|<|«)");
    static final Pattern POSITIVE = ciPattern("(article|body|content|entry|hentry|main|page|pagination|post|text|blog|story)");
    static final Pattern REPLACE_BRS = ciPattern("(<br[^>]*>[ \n\r\t]*){2,}");

    static final Pattern UNLIKELY_CANDIDATES = ciPattern("combx|comment|community|disqus|extra|foot|header|menu|remark|rss|shoutbox|sidebar|sponsor|ad-break|agegate|pagination|pager|popup|tweet|twitter");
    static final Pattern OK_MAYBE_ITS_A_CANDIDATE = ciPattern("and|article|body|column|main|shadow");
    static final Pattern ENDS_WITH_DOT = Pattern.compile("\\.( |$)");
    static final Pattern DIGIT = Pattern.compile("\\d");
    static final Pattern BAR_DASH = Pattern.compile(" [\\|\\-] ");

    private Patterns() {
        //
    }

    static boolean match(Pattern pattern, String string) {
        return pattern.matcher(string).matches();
    }

    static boolean exists(Pattern pattern, String string) {
        return pattern.matcher(string).find();
    }

    private static Pattern ciPattern(String patternString) {
        return Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
    }

}
