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

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class HttpPageReader extends AbstractPageReader implements PageReader {
    static final Logger LOG = LoggerFactory.getLogger(HttpPageReader.class);

    /** {@inheritDoc}*/
    @Override
    public String readPage(String url) throws PageReadException {
        LOG.info("Reading " + url);
        HttpParams httpParameters = new BasicHttpParams();
        // Set the timeout in milliseconds until a connection is established.
        int timeoutConnection = 3000;
        HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
        // Set the default socket timeout (SO_TIMEOUT) 
        // in milliseconds which is the timeout for waiting for data.
        int timeoutSocket = 10000;
        HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
        DefaultHttpClient httpclient = new DefaultHttpClient(httpParameters);
        HttpContext localContext = new BasicHttpContext();
        HttpGet get = new HttpGet(url);
        InputStream response = null;
        HttpResponse httpResponse = null;
        try {
            try {
                httpResponse = httpclient.execute(get, localContext);
                int resp = httpResponse.getStatusLine().getStatusCode();
                if (HttpStatus.SC_OK != resp) {
                    LOG.error("Download failed of " + url + " status " + resp + " " + httpResponse.getStatusLine().getReasonPhrase());
                    return null;
                }
                String respCharset = EntityUtils.getContentCharSet(httpResponse.getEntity());
                return readContent(httpResponse.getEntity().getContent(), respCharset);
            } finally {
                if (response != null) {
                    response.close();
                }
                if (httpResponse != null && httpResponse.getEntity() != null) {
                    httpResponse.getEntity().consumeContent();
                }

            }
        } catch (IOException e) {
            LOG.error("Download failed of " + url, e);
            throw new PageReadException("Failed to read " + url, e);
        }
    }

}
