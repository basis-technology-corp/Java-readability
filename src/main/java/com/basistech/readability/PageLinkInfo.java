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
