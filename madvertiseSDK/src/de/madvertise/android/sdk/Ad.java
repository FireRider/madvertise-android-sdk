/*
 * Copyright 2011 madvertise Mobile Advertising GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.madvertise.android.sdk;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

/**
 * Defines an ad from a JSON object, that contains all necessary information which is provided 
 * by the madvertise ad server. Icons and banners are synchronously fetched from the madvertise server and 
 * stored on the device. Click action is handled asynchronously.
 */
public class Ad {

    private final String CLICK_URL_CODE = "click_url";

    private final String BANNER_URL_CODE = "banner_url";

    private final String TEXT_CODE = "text";

    private final String HAS_BANNER_CODE = "has_banner";

    private String clickURL;

    private String bannerURL;

    private String text;

    private boolean hasBannerLink;

    private boolean hasBanner;

    private JSONArray jsonNames;

    private JSONArray jsonValues;

    private byte[] imageByteArray;

    private Context context;

    /**
     * Constructor, blocking due to http request, should be called in a thread pool, a request queue, 
     * a network thread
     * 
     * @param context 
     *      the applications context
     * @param json 
     *      json object containing all ad information
     */
    protected Ad(Context context, JSONObject json) {
        this.context = context;

        MadUtil.logMessage(null, Log.DEBUG, "Creating ad");

        // init json arrays and print all keys / values
        jsonNames = json.names();
        try {
            jsonValues = json.toJSONArray(jsonNames);

            if (MadUtil.PRINT_LOG) {
                for (int i = 0; i < jsonNames.length(); i++) {
                    MadUtil.logMessage(null, Log.DEBUG, "Key => " + jsonNames.getString(i) + " Value => "
                            + jsonValues.getString(i));
                }
            }

            clickURL = json.isNull(CLICK_URL_CODE) ? "" : json.getString(CLICK_URL_CODE);
            bannerURL = json.isNull(BANNER_URL_CODE) ? "" : json.getString(BANNER_URL_CODE);
            text = json.isNull(TEXT_CODE) ? "" : json.getString(TEXT_CODE);
            hasBannerLink = Boolean.parseBoolean(json.isNull(HAS_BANNER_CODE) ? "true" : json
                    .getString(HAS_BANNER_CODE));

        } catch (JSONException e) {
            MadUtil.logMessage(null, Log.DEBUG, "Error in json string");
            e.printStackTrace();
        }

        if (hasBannerLink) {
            imageByteArray = downloadImage(bannerURL);
        } else {
            MadUtil.logMessage(null, Log.DEBUG, "No banner link in json found");
        }

        if (imageByteArray != null) {
            hasBanner = true;
        } else {
            hasBanner = false;
        }
    }

    /**
     * Download an image from given URL and return it as byte array.
     * 
     * @param imageURLString 
     *      url of the banner
     * @return 
     *      image as byte array
     */
    private byte[] downloadImage(String imageURLString) {

        InputStream inputStream = null;
        ByteArrayOutputStream byteArrayOutputStream = null;
        HttpClient client = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(imageURLString);
        HttpResponse response = null;
        byte[] returnByteArray = null;

        if (imageURLString == null) {
            return returnByteArray;
        }

        HttpParams clientParams = client.getParams();
        HttpConnectionParams.setConnectionTimeout(clientParams,
                MadUtil.CONNECTION_TIMEOUT.intValue());
        HttpConnectionParams.setSoTimeout(clientParams, MadUtil.CONNECTION_TIMEOUT.intValue());

        MadUtil.logMessage(null, Log.DEBUG, "Try to download banner: " + imageURLString);

        try {
            response = client.execute(getRequest);

            MadUtil.logMessage(null, Log.DEBUG, "Response Code=> " + response.getStatusLine().getStatusCode());

            HttpEntity entity = response.getEntity();
            int responseCode = response.getStatusLine().getStatusCode();

            if (responseCode == 200 && entity != null) {

                inputStream = response.getEntity().getContent();
                byteArrayOutputStream = new ByteArrayOutputStream();

                int input = inputStream.read();
                while (input != -1) {
                    byteArrayOutputStream.write(input);
                    input = inputStream.read();
                }
                returnByteArray = byteArrayOutputStream.toByteArray();
            } else {
                MadUtil.logMessage(null, Log.DEBUG, "Could not download banner because response code is "+ responseCode +" (expected 200) or empty body");
            }
        } catch (IOException e) {
            MadUtil.logMessage(null, Log.DEBUG, "Cannot fetch banner or icon from server");
            e.printStackTrace();
        } finally {
            // close all streams
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }

            if (byteArrayOutputStream != null) {
                try {
                    byteArrayOutputStream.close();
                } catch (IOException e) {
                }
            }
        }
        return returnByteArray;
    }

    /**
     * Handles the click action (opens the click url)
     */
    protected void handleClick() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(clickURL));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(intent);
        } catch (Exception e) {
            MadUtil.logMessage(null, Log.DEBUG, "Failed to open URL : " + clickURL);
            e.printStackTrace();
        }
    }

    protected String getClickURL() {
        return clickURL;
    }

    protected String getBannerURL() {
        return bannerURL;
    }

    protected String getText() {
        return text;
    }

    protected boolean hasBanner() {
        return hasBanner;
    }

    protected byte[] getImageByteArray() {
        return imageByteArray;
    }
}
