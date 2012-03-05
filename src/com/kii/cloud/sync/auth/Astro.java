package com.kii.cloud.sync.auth;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.kii.sync.KiiClient;
import com.kii.sync.SyncMsg;
import com.kii.sync.utils.HttpClientUtils;

public class Astro extends Identity {

    public Astro(KiiClient syncManager, String URL) {
        super(syncManager, URL);
    }

    private static String TAG = "Astro";

    private static final String URL_SUBSCRIPTION = "/integration/astro/subscription/sync";
    private static final String TAG_SYNC_ENABLED = "syncEnabled";
    private static final String TAG_USED_BYTE_SIZE = "usedByteSize";
    private static final String TAG_QUOTA_BYTE_SIZE = "quotaByteSize";

    /**
     * For testing only Set the subscription and quota
     * 
     * @param sync
     * @param quota
     *            in number of bytes
     * @return SyncMsg
     */
    public int setSubscription(String username, String password, boolean sync,
            long quota) {
        HttpPut request = new HttpPut(mBaseURL + URL_SUBSCRIPTION);
        request.addHeader("Content-Type", "application/json");
        JSONObject j = new JSONObject();
        StringEntity entry = null;

        try {
            j.put("quota", quota);
            j.put(TAG_SYNC_ENABLED, sync);

            Log.v(TAG, "User create for JsonObj: " + j.toString());
            entry = new StringEntity(j.toString(), HTTP.UTF_8);
            entry.setContentType("application/json");

            request.setEntity(entry);
            HttpResponse response = null;

            response = HttpClientUtils.execute(request, username, password);
            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode == HttpStatus.SC_OK) {
                return SyncMsg.OK;
            } else {
                return SyncMsg.convertStatusCodeToKiiError(HttpClientUtils
                        .logHttpResponse(TAG, request, response));
            }

        } catch (JSONException e) {
            Log.e(TAG, "API setSubscription JSONException:", e);
            return SyncMsg.ERROR_JSON;
        } catch (UnsupportedEncodingException ue) {
            Log.e(TAG, "API setSubscription UnsupportedEncodingException:", ue);
            return SyncMsg.ERROR_JSON;
        } catch (ClientProtocolException cpe) {
            Log.e(TAG, "API setSubscription ClientProtocolException:", cpe);
            return SyncMsg.ERROR_IO;
        } catch (IOException ioe) {
            Log.e(TAG, "API setSubscription IOException:", ioe);
            return SyncMsg.ERROR_IO;
        }
    }

    /**
     * Returns a list of services that the account is currently link to
     * 
     * @return "syncEnabled":true,"usedByteSize":0,"quotaByteSize":1000000000
     */
    public List<String> getSubscription(String username, String password) {
        // HttpGet request = new HttpGet(SyncPref.getAppUrl() +
        // URL_SUBSCRIPTION);
        HttpGet request = new HttpGet(
                "https://product-jp.kii.com/integration/astro"
                        + URL_SUBSCRIPTION);
        request.addHeader("Content-Type", "application/json");

        HttpResponse response = null;
        try {
            response = HttpClientUtils.execute(request, username, password);
            if (response.getStatusLine().getStatusCode() != 200) {
                HttpClientUtils.logHttpResponse(TAG, request, response);
                return null;
            }

            ArrayList<String> servicesList = new ArrayList<String>();

            String res = EntityUtils.toString(response.getEntity());
            Log.d(TAG, "getListServices response is " + res);
            JSONObject jsonObj = new JSONObject(res);

            if (jsonObj.has(TAG_SYNC_ENABLED)) {
                servicesList.add(jsonObj.getString(TAG_SYNC_ENABLED));
            } else {
                servicesList.add(null);
            }

            if (jsonObj.has(TAG_USED_BYTE_SIZE)) {
                servicesList.add(jsonObj.getString(TAG_USED_BYTE_SIZE));
            } else {
                servicesList.add(null);
            }

            if (jsonObj.has(TAG_QUOTA_BYTE_SIZE)) {
                servicesList.add(jsonObj.getString(TAG_QUOTA_BYTE_SIZE));
            } else {
                servicesList.add(null);
            }

            return servicesList;

        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
        return null;
    }

}
