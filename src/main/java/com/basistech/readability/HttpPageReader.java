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
