package org.readium.sdk.lcp;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.ProgressCallback;
import com.koushikdutta.ion.Response;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

//#if !DISABLE_LSD
public class StatusDocumentProcessing {

    public interface IListener {
        void onStatusDocumentProcessingComplete();
        //void onStatusDocumentProcessingComplete_(StatusDocumentProcessing sdp);
    }

    public static abstract class Listener implements IListener {
        final private StatusDocumentProcessing m_StatusDocumentProcessing;

        public Listener(StatusDocumentProcessing sdp) {
            m_StatusDocumentProcessing = sdp;
        }

        public void onStatusDocumentProcessingComplete() {
            this.onStatusDocumentProcessingComplete_(m_StatusDocumentProcessing);
        }

        public abstract void onStatusDocumentProcessingComplete_(StatusDocumentProcessing sdp);
    }

    private Context m_context;
    private Service mLcpService = null;
    private String mBookPath = null;
    private License mLicense = null;
    public StatusDocumentProcessing(Context context, Service service, String path, License license) {
        m_context = context;
        mLcpService = service;
        mBookPath = path;
        mLicense = license;
    }

    private boolean m_wasCancelled = false;
    public boolean wasCancelled() {
        return m_wasCancelled;
    }

    public void cancel() {
        m_wasCancelled = true;
        mLicense.setStatusDocumentProcessingFlag(false);
    }

    final protected class StatusDocumentLink {
        public final String m_rel;
        public final String m_href;
        public final String m_type;
        public final String m_templated;
        public final String m_title;
        public final String m_profile;
        public StatusDocumentLink(String rel, String href, String type, String templated, String title, String profile) {
            m_rel = rel == null ? "" : rel;
            m_href = href == null ? "" : href;
            m_type = type == null ? "" : type;
            m_templated = templated == null ? "" : templated;
            m_title = title == null ? "" : title;
            m_profile = profile == null ? "" : profile;
        }
    }

    private String m_statusDocument_ID = "";
    private String m_statusDocument_STATUS = ""; // ready, active, revoked, returned, cancelled, expired
    private String m_statusDocument_MESSAGE = ""; // localized as per HTTP Accept-Language
    private String m_statusDocument_UPDATED_LICENSE = ""; //ISO 8601 time and date
    private String m_statusDocument_UPDATED_STATUS = ""; //ISO 8601 time and date
    private StatusDocumentLink m_statusDocument_LINK_LICENSE = null; // HTTP GET URL, no-template
    private StatusDocumentLink m_statusDocument_LINK_REGISTER = null;
    private StatusDocumentLink m_statusDocument_LINK_RETURN = null;
    private StatusDocumentLink m_statusDocument_LINK_RENEW = null;
    private String m_statusDocument_POTENTIAL_RIGHTS_END = ""; // ISO 8601 time and date

    private void fetchAndInjectUpdatedLicense() {

        if (m_statusDocument_LINK_LICENSE == null) {
            m_statusDocumentProcessingListener.onStatusDocumentProcessingComplete();
            return;
        }

        //#if !ENABLE_NET_PROVIDER

        final String url = m_statusDocument_LINK_LICENSE.m_href;

//        final AsyncHttpRequestFactory current = Ion.getDefault(context).configure().getAsyncHttpRequestFactory();
//        Ion.getDefault(context).configure().setAsyncHttpRequestFactory(new AsyncHttpRequestFactory() {
//            @Override
//            public AsyncHttpRequest createAsyncHttpRequest(Uri uri, String method, Headers headers) {
//                AsyncHttpRequest ret = current.createAsyncHttpRequest(uri, method, headers);
//                ret.setTimeout(1000);
//                return ret;
//            }
//        });

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {

                Locale currentLocale = getCurrentLocale();
                String langCode = currentLocale.toString().replace('_', '-');
                langCode = langCode + ",en-US;q=0.7,en;q=0.5";

                Future<Response<InputStream>> request = Ion.with(m_context)
                        .load(url)
                        .setLogging("Readium Ion", Log.VERBOSE)
//                        .progress(new ProgressCallback() {
//                            @Override
//                            public void onProgress(long downloaded, long total) {
//
//                                // total is -1 when HTTP content-length header is not set.
//                                if (total < downloaded) {
//                                    total = downloaded*2;
//                                }
//                                float value = (downloaded / (float)total);
//
//                                //noop
//                            }
//                        }) // not UI thread
                        //.progressHandler(callback) // UI thread
                        //.setTimeout(AsyncHttpRequest.DEFAULT_TIMEOUT) //30000
                        .setTimeout(6000)

                        // TODO: comment this in production! (this is only for testing a local HTTP server)
                        //.setHeader("X-Add-Delay", "2s")

                        // LCP / LSD server with message localization
                        .setHeader("Accept-Language", langCode)

                        .asInputStream()
                        .withResponse()
                        .setCallback(new FutureCallback<Response<InputStream>>() {
                            @Override
                            public void onCompleted(Exception e, Response<InputStream> response) {

                                InputStream inputStream = response != null ? response.getResult() : null;
                                int httpResponseCode = response != null ? response.getHeaders().code() : 0;
                                if (e != null || inputStream == null
                                        || httpResponseCode < 200 || httpResponseCode >= 300) {

                                    m_statusDocumentProcessingListener.onStatusDocumentProcessingComplete();
                                    return;
                                }

                                try {
                                    StringWriter writer = new StringWriter();
                                    IOUtils.copy(inputStream, writer, "UTF-8");
                                    String json = writer.toString().trim();

                                    // new LCP license
                                    mLcpService.injectLicense(mBookPath, json);

                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                } finally {
                                    try {
                                        inputStream.close();
                                    } catch (IOException ex) {
                                        ex.printStackTrace();
                                        // ignore
                                    }

                                    m_statusDocumentProcessingListener.onStatusDocumentProcessingComplete();
                                }
                            }
                        })
//                .write(new File(dstPath))
//                .setCallback(callback)
                        ;


//            }
//        });
            }
        }, 500);
    }

    private void registerDevice() {

        final String deviceNAME = "Android";

        String id = UUID.randomUUID().toString();

        // TODO: weird MAC address on my device...not sure it's reliable (Wifi-ADB, LLDB debug session).
//        try {
//            WifiManager wm = (WifiManager) m_context.getSystemService(Context.WIFI_SERVICE);
//            id = wm.getConnectionInfo().getMacAddress();
//        } catch(Exception ex){
//            // ignore
//        }

        SharedPreferences sharedPrefs_DEVICEID = m_context.getSharedPreferences(
                "DEVICE_ID", Context.MODE_PRIVATE);
        String pref_DEVICEID = sharedPrefs_DEVICEID.getString("DEVICE_ID", null);
        if (pref_DEVICEID == null) {
            SharedPreferences.Editor editor = sharedPrefs_DEVICEID.edit();
            editor.putString("DEVICE_ID", id);
            editor.commit();
        } else {
            id = pref_DEVICEID;
        }

        final String deviceID = id;

        final String PREF_ID = "LSD_DEVICE_REG_" + m_statusDocument_ID;

        boolean doRegister = false;
        if (m_statusDocument_LINK_REGISTER == null) {
            doRegister = false;
        }
        else if (m_statusDocument_STATUS.equals("ready")) {
            doRegister = true;
        } else if (m_statusDocument_STATUS.equals("active")) {

            SharedPreferences sharedPrefs = m_context.getSharedPreferences(
                    PREF_ID, Context.MODE_PRIVATE);
            String pref = sharedPrefs.getString(PREF_ID, null);
            if (pref == null) {
                doRegister = true;
            } else if (!pref.equals(deviceID)) {
                doRegister = true;
            }
        }

        doRegister = true;

        if (!doRegister) {
            m_statusDocumentProcessingListener.onStatusDocumentProcessingComplete();
            return;
        }


        String url_ = m_statusDocument_LINK_REGISTER.m_href;
        if (m_statusDocument_LINK_REGISTER.m_templated.equals("true")) {
            url_ = url_.replace("{?id,name}", ""); // TODO: smarter regexp?
        }
        final String url = url_;

//        final AsyncHttpRequestFactory current = Ion.getDefault(context).configure().getAsyncHttpRequestFactory();
//        Ion.getDefault(context).configure().setAsyncHttpRequestFactory(new AsyncHttpRequestFactory() {
//            @Override
//            public AsyncHttpRequest createAsyncHttpRequest(Uri uri, String method, Headers headers) {
//                AsyncHttpRequest ret = current.createAsyncHttpRequest(uri, method, headers);
//                ret.setTimeout(1000);
//                return ret;
//            }
//        });

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {

                Locale currentLocale = getCurrentLocale();
                String langCode = currentLocale.toString().replace('_', '-');
                langCode = langCode + ",en-US;q=0.7,en;q=0.5";

                Future<Response<InputStream>> request = Ion.with(m_context)
                        .load(url)
                        .setLogging("Readium Ion", Log.VERBOSE)
//                        .progress(new ProgressCallback() {
//                            @Override
//                            public void onProgress(long downloaded, long total) {
//
//                                // total is -1 when HTTP content-length header is not set.
//                                if (total < downloaded) {
//                                    total = downloaded*2;
//                                }
//                                float value = (downloaded / (float)total);
//
//                                //noop
//                            }
//                        }) // not UI thread
                        //.progressHandler(callback) // UI thread
                        //.setTimeout(AsyncHttpRequest.DEFAULT_TIMEOUT) //30000
                        .setTimeout(6000)

                        // TODO: comment this in production! (this is only for testing a local HTTP server)
                        //.setHeader("X-Add-Delay", "2s")

                        // LCP / LSD server with message localization
                        .setHeader("Accept-Language", langCode)

                        .setBodyParameter("id", deviceID)
                        .setBodyParameter("name", deviceNAME)

                        .asInputStream()
                        .withResponse()
                        .setCallback(new FutureCallback<Response<InputStream>>() {
                            @Override
                            public void onCompleted(Exception e, Response<InputStream> response) {

                                InputStream inputStream = response != null ? response.getResult() : null;
                                int httpResponseCode = response != null ? response.getHeaders().code() : 0;
                                if (e != null || inputStream == null
                                        || httpResponseCode < 200 || httpResponseCode >= 300) {

                                    m_statusDocumentProcessingListener.onStatusDocumentProcessingComplete();
                                    return;
                                }

                                try {

                                    StringWriter writer = new StringWriter();
                                    IOUtils.copy(inputStream, writer, "UTF-8");
                                    String json = writer.toString().trim();

                                    boolean okay = parseStatusDocumentJson(json);

                                    if (okay && m_statusDocument_STATUS.equals("active")) {

                                        SharedPreferences sharedPrefs = m_context.getSharedPreferences(
                                                PREF_ID, Context.MODE_PRIVATE);
//                                        String pref = sharedPrefs.getString(PREF_ID, null);
                                        SharedPreferences.Editor editor = sharedPrefs.edit();
//                                        if (pref != null) {
//                                            editor.remove(PREF_ID);
//                                        }
                                        editor.putString(PREF_ID, deviceID);
                                        editor.commit();
                                    }

                                    m_statusDocumentProcessingListener.onStatusDocumentProcessingComplete();

                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                    m_statusDocumentProcessingListener.onStatusDocumentProcessingComplete();
                                } finally {
                                    try {
                                        inputStream.close();
                                    } catch (IOException ex) {
                                        ex.printStackTrace();
                                        // ignore
                                    }
                                }
                            }
                        })
//                .write(new File(dstPath))
//                .setCallback(callback)
                        ;


//            }
//        });
            }
        }, 500);
    }

    private void processStatusDocument() {
        boolean licenseNeedsUpdating = mLicense.isOlderThan(m_statusDocument_UPDATED_LICENSE);
        if (false && // TODO: fix the server LCP license HTTP GET ("front end" provider)
                licenseNeedsUpdating) {
            fetchAndInjectUpdatedLicense();
            return;
        }

        if (m_statusDocument_STATUS.equals("revoked")
            || m_statusDocument_STATUS.equals("returned")
            || m_statusDocument_STATUS.equals("cancelled")
            || m_statusDocument_STATUS.equals("expired")
            ) {
            // TODO prevent EPUB load? This should not happen,
            // because in this case(s) the LCP license should not even pass validation due to timestamp
            m_statusDocumentProcessingListener.onStatusDocumentProcessingComplete();
            return;
        }

        if (m_statusDocument_LINK_REGISTER != null) {
            registerDevice();
            return;
        }

        m_statusDocumentProcessingListener.onStatusDocumentProcessingComplete();
    }

    //   https://docs.google.com/document/d/1ErBf0Gl32jNH-QVKWpGPfZDMWMeQP7dH9YY5g7agguQ
    private boolean parseStatusDocumentJson(String json) {
        try {
            JSONObject jsonObject_ROOT = new JSONObject(json);
            m_statusDocument_ID = jsonObject_ROOT.getString("id");
            m_statusDocument_STATUS = jsonObject_ROOT.getString("status");
            m_statusDocument_MESSAGE = jsonObject_ROOT.getString("message");

            JSONObject jsonObject_UPDATED = jsonObject_ROOT.getJSONObject("updated");
            m_statusDocument_UPDATED_LICENSE = jsonObject_UPDATED.getString("license");
            m_statusDocument_UPDATED_STATUS = jsonObject_UPDATED.getString("status");

            JSONArray jsonArray_LINKS = jsonObject_ROOT.getJSONArray("links");
            for (int i = 0; i < jsonArray_LINKS.length(); i++) {
                JSONObject jsonObject_LINK = jsonArray_LINKS.getJSONObject(i);

                String rel = jsonObject_LINK.getString("rel");
                String href = jsonObject_LINK.getString("href");
                String type = jsonObject_LINK.optString("type");
                String templated = jsonObject_LINK.optString("templated");
                String title = jsonObject_LINK.optString("title");
                String profile = jsonObject_LINK.optString("profile");

                StatusDocumentLink link = new StatusDocumentLink(rel, href, type, templated, title, profile);
                if (rel.equals("license")) {
                    m_statusDocument_LINK_LICENSE = link;
                } else if (rel.equals("register")) {
                    m_statusDocument_LINK_REGISTER = link;
                } else if (rel.equals("return")) {
                    m_statusDocument_LINK_RETURN = link;
                } else if (rel.equals("renew")) {
                    m_statusDocument_LINK_RENEW = link;
                } else {
                    boolean breakpoint = true;
                }
            }

            JSONObject jsonObject_POTENTIAL_RIGHTS = jsonObject_ROOT.optJSONObject("potential_rights");
            m_statusDocument_POTENTIAL_RIGHTS_END = jsonObject_POTENTIAL_RIGHTS.getString("end");

            JSONArray jsonArray_EVENTS = jsonObject_ROOT.optJSONArray("events");
            if (jsonArray_EVENTS != null) {
                for (int i = 0; i < jsonArray_EVENTS.length(); i++) {
                    JSONObject jsonObject_EVENT = jsonArray_EVENTS.getJSONObject(i);

                    String type = jsonObject_EVENT.getString("type");
                    String name = jsonObject_EVENT.getString("name");
                    String timestamp = jsonObject_EVENT.getString("timestamp"); // ISO 8601 time and date
                    String id = jsonObject_EVENT.getString("id");
                }
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    public Locale getCurrentLocale(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            return m_context.getResources().getConfiguration().getLocales().get(0);
        } else{
            //noinspection deprecation
            return m_context.getResources().getConfiguration().locale;
        }
    }

    private IListener m_statusDocumentProcessingListener = null;
    public void start(IListener statusDocumentProcessingListener) {

        m_statusDocumentProcessingListener = statusDocumentProcessingListener;

        //#if !ENABLE_NET_PROVIDER

        final String url = mLicense.getLink_Status();

//        final AsyncHttpRequestFactory current = Ion.getDefault(context).configure().getAsyncHttpRequestFactory();
//        Ion.getDefault(context).configure().setAsyncHttpRequestFactory(new AsyncHttpRequestFactory() {
//            @Override
//            public AsyncHttpRequest createAsyncHttpRequest(Uri uri, String method, Headers headers) {
//                AsyncHttpRequest ret = current.createAsyncHttpRequest(uri, method, headers);
//                ret.setTimeout(1000);
//                return ret;
//            }
//        });

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {

                Locale currentLocale = getCurrentLocale();
                String langCode = currentLocale.toString().replace('_', '-');
                langCode = langCode + ",en-US;q=0.7,en;q=0.5";

                Future<Response<InputStream>> request = Ion.with(m_context)
                        .load(url)
                        .setLogging("Readium Ion", Log.VERBOSE)
//                        .progress(new ProgressCallback() {
//                            @Override
//                            public void onProgress(long downloaded, long total) {
//
//                                // total is -1 when HTTP content-length header is not set.
//                                if (total < downloaded) {
//                                    total = downloaded*2;
//                                }
//                                float value = (downloaded / (float)total);
//
//                                //noop
//                            }
//                        }) // not UI thread
                        //.progressHandler(callback) // UI thread
                        //.setTimeout(AsyncHttpRequest.DEFAULT_TIMEOUT) //30000
                        .setTimeout(6000)

                        // TODO: comment this in production! (this is only for testing a local HTTP server)
                        //.setHeader("X-Add-Delay", "2s")

                        // LCP / LSD server with message localization
                        .setHeader("Accept-Language", langCode)

                        .asInputStream()
                        .withResponse()
                        .setCallback(new FutureCallback<Response<InputStream>>() {
                            @Override
                            public void onCompleted(Exception e, Response<InputStream> response) {


                                InputStream inputStream = response != null ? response.getResult() : null;
                                int httpResponseCode = response != null ? response.getHeaders().code() : 0;
                                if (e != null || inputStream == null
                                        || httpResponseCode < 200 || httpResponseCode >= 300) {

                                    m_statusDocumentProcessingListener.onStatusDocumentProcessingComplete();
                                    return;
                                }

                                try {
//                                    int length = 0;
//                                    try {
//                                        String strLength = response.getHeaders().getHeaders().get("Content-Length");
//                                        length = Integer.parseInt(strLength);
//                                    } catch(Exception exc){
//                                        // ignore
//                                    }
//                                    if (length == 0) {
//                                        length = inputStream.available();
//                                        length = 20*1024; // TODO: 20kB max okay?
//                                    }
//
//                                    byte[] buffer = new byte[length];
//                                    int bytesRead = inputStream.read(buffer, 0, length);
//                                    String json = new String(buffer, 0, bytesRead, "UTF-8");
//
//                                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
//                                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
//                                    StringBuilder stringBuilder = new StringBuilder();
//
//                                    String line = null;
//                                    while ((line = bufferedReader.readLine()) != null)
//                                    {
//                                        stringBuilder.append(line + "\n");
//                                    }
//
//                                    String json = stringBuilder.toString();


                                    StringWriter writer = new StringWriter();
                                    IOUtils.copy(inputStream, writer, "UTF-8");
                                    String json = writer.toString().trim();

                                    boolean okay = parseStatusDocumentJson(json);

                                    if (okay) {
                                        processStatusDocument();
                                    } else {
                                        m_statusDocumentProcessingListener.onStatusDocumentProcessingComplete();
                                    }

                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                    m_statusDocumentProcessingListener.onStatusDocumentProcessingComplete();
                                } finally {
                                    try {
                                        inputStream.close();
                                    } catch (IOException ex) {
                                        ex.printStackTrace();
                                        // ignore
                                    }
                                }
                            }
                        })
//                .write(new File(dstPath))
//                .setCallback(callback)
                ;


//            }
//        });
            }
        }, 500);
    }
}
