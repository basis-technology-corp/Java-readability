/******************************************************************************
 ** This data and information is proprietary to, and a valuable trade secret
 ** of, Basis Technology Corp.  It is given in confidence by Basis Technology
 ** and may only be used as permitted under the license agreement under which
 ** it has been distributed, and in no other way.
 **
 ** Copyright (c) 2000-2008 Basis Technology Corporation All rights reserved.
 **
 ** The technical data and information provided herein are provided with
 ** `limited rights', and the computer software provided herein is provided
 ** with `restricted rights' as those terms are defined in DAR and ASPR
 ** 7-104.9(a).
 ******************************************************************************/

package com.basistech.readability;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * At the moment this class will take all html files from the flat directory: ./src/test/resources/htmlInput/
 * and write them to: ./src/test/resources/textOutput/
 * 
 * In the future, a nice thing to do might be to abstract this so that it can process just about anything you
 * throw in there.  It's a matter of using the appropriate PageReaders.  Also, having the directories be hard-coded
 * might be a problem in the future.
 */

public final class ReadabilityDriver {
    
    //the logger
    private static final Logger LOG = LoggerFactory.getLogger(ReadabilityDriver.class);
    
    //the paths
    private static final String INPUT_PATH = "./src/test/resources/htmlInput/";
    private static final String OUTPUT_PATH = "target";
    
    //private constructor
    private ReadabilityDriver() { }
    
    public static void main(String[] args) throws IOException {
        
        //input directory file
        File inputDir = new File(INPUT_PATH);
        
        //create the FilePageReader for Readability
        FilePageReader reader = new FilePageReader();
        reader.setBaseDirectory(inputDir);
        
        //instantiate Readability and set reader
        Readability readability = new Readability();
        readability.setPageReader(reader);
        readability.setReadAllPages(false);
        reader.setCharsetDetector(new TikaCharsetDetector());
        
        //instantiate a file array
        File[] htmlFiles;
        
        //get all html files in directory
        if (inputDir.exists()) {
            htmlFiles = inputDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.matches(".*\\.html$");
                }
            });
        } else {
            htmlFiles = new File[0];
        }
        
        //iterate over the files and run Readability on them
        for (File page : htmlFiles) {
            
            //get the page path
            String path = page.getPath();
            
            //process the page
            try {
                LOG.info("processing page: " + path);
                readability.processDocument(path);
            } catch (PageReadException e) {
                LOG.error("PageReadError while processing: " + path);
                e.printStackTrace();
                continue;
            }
            
            //write the output, forcing a sentence break between title and body with \u2029.
            String title = readability.getTitle().trim() + "\u2029";
            String content = readability.getArticleText();
            String returnText = OUTPUT_PATH + page.getName().replaceAll("html$", "txt");
            FileOutputStream fos = new FileOutputStream(returnText);
            fos.write((title + System.getProperty("line.separator") + content).getBytes("UTF8"));
            fos.flush();
            fos.close();
        }
    }
}
