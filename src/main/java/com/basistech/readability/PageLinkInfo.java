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

/**
 *
 */
public class PageLinkInfo {
    private double score;
    private String linkText;
    private String href;
    public PageLinkInfo(double score, String linkText, String href) {
        this.score = score;
        this.linkText = linkText;
        this.href = href;
    }
    public void setScore(double score) {
        this.score = score;
    }
    public void incrementScore(double incr) {
        score = score + incr;
    }
    public void setLinkText(String linkText) {
        this.linkText = linkText;
    }
    public double getScore() {
        return score;
    }
    public String getLinkText() {
        return linkText;
    }
    public String getHref() {
        return href;
    }
    @Override
    public String toString() {
        return "PageLinkInfo [score=" + score + ", linkText=" + linkText + ", href=" + href + "]";
    }
}
