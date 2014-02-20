/**
 * Copyright 2010-present Facebook.
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

package com.facebook.internal;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import com.facebook.*;
import com.facebook.android.BuildConfig;
import com.facebook.model.GraphObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for Android. Use of
 * any of the classes in this package is unsupported, and they may be modified or removed without warning at
 * any time.
 */
public final class Utility {
    static final String LOG_TAG = "FacebookSDK";
    private static final String HASH_ALGORITHM_MD5 = "MD5";
    private static final String HASH_ALGORITHM_SHA1 = "SHA-1";
    private static final String URL_SCHEME = "https";
    private static final String SUPPORTS_ATTRIBUTION = "supports_attribution";
    private static final String SUPPORTS_IMPLICIT_SDK_LOGGING = "supports_implicit_sdk_logging";
    private static final String [] APP_SETTING_FIELDS = new String[] {
            SUPPORTS_ATTRIBUTION,
            SUPPORTS_IMPLICIT_SDK_LOGGING
    };
    private static final String APPLICATION_FIELDS = "fields";

    // This is the default used by the buffer streams, but they trace a warning if you do not specify.
    public static final int DEFAULT_STREAM_BUFFER_SIZE = 8192;

    private static Map<String, FetchedAppSettings> fetchedAppSettings =
            new ConcurrentHashMap<String, FetchedAppSettings>();

    public static class FetchedAppSettings {
        private boolean supportsAttribution;
        private boolean supportsImplicitLogging;

        private FetchedAppSettings(boolean supportsAttribution, boolean supportsImplicitLogging) {
            this.supportsAttribution = supportsAttribution;
            this.supportsImplicitLogging = supportsImplicitLogging;
        }

        public boolean supportsAttribution() {
            return supportsAttribution;
        }

        public boolean supportsImplicitLogging() {
            return supportsImplicitLogging;
        }
    }

    // *************** APPCELERATOR TITANIUM CUSTOMIZATION ***************************
    // Set ENABLE_LOG to true to enable log output. Remember to turn this back off
    // before releasing. Sending sensitive data to log is a security risk.
    private static boolean ENABLE_LOG = false;

    // *************** APPCELERATOR TITANIUM CUSTOMIZATION ***************************
    // Resource IDs used in com.facebook package. Fetch the resource id using Resources.getIdentifier, since
    // we merge resources into Titanium project and don't have access to R here.
    public static int resId_blueColor = -1;
    public static int resId_chooseFriends = -1;
    public static int resId_close = -1;
    public static int resId_errorMessage = -1;
    public static int resId_errorTitle = -1;
    public static int resId_friendPickerFragment = -1;
    public static int resId_friendPickerFragmentMultiSelect = -1;
    public static int resId_friendPickerFragmentStyleable = -1;
    public static int resId_inverseIcon = -1;
    public static int resId_loading = -1;
    public static int resId_loginActivityLayout = -1;
    public static int resId_loginActivityProgressBar = -1;
    public static int resId_loginButtonImage = -1;
    public static int resId_loginView = -1;
    public static int resId_loginViewCancelAction = -1;
    public static int resId_loginviewCompoundPadding = -1;
    public static int resId_loginViewConfirmLogout = -1;
    public static int resId_loginViewFetchUserInfo = -1;
    public static int resId_loginViewHeight = -1;
    public static int resId_loginViewPaddingBottom = -1;
    public static int resId_loginViewPaddingLeft = -1;
    public static int resId_loginViewPaddingRight = -1;
    public static int resId_loginViewPaddingTop = -1;
    public static int resId_loginViewTextColor = -1;
    public static int resId_loginViewTextSize = -1;
    public static int resId_loginViewLoggedInAs = -1;
    public static int resId_loginViewLoggedUsingFacebook = -1;
    public static int resId_loginViewLoginButton = -1;
    public static int resId_loginViewLoginText = -1;
    public static int resId_loginViewLogoutAction = -1;
    public static int resId_loginViewLogoutButton = -1;
    public static int resId_loginViewLogoutText = -1;
    public static int resId_loginViewWidth = -1;
    public static int resId_nearby = -1;
    public static int resId_pickerActivityCircle = -1;
    public static int resId_pickerCheckbox = -1;
    public static int resId_pickerCheckboxStub = -1;
    public static int resId_pickerDoneButton = -1;
    public static int resId_pickerDoneButtonText = -1;
    public static int resId_pickerImage = -1;
    public static int resId_pickerListRow = -1;
    public static int resId_pickerListSectionHeader = -1;
    public static int resId_pickerListView = -1;
    public static int resId_pickerProfilePicStub = -1;
    public static int resId_pickerRowActivityCircle = -1;
    public static int resId_pickerSearchBox = -1;
    public static int resId_pickerSearchText = -1;
    public static int resId_pickerSubTitle = -1;
    public static int resId_pickerTitle = -1;
    public static int resId_pickerTitleBar = -1;
    public static int resId_pickerTitleBarStub = -1;
    public static int resId_placeDefaultIcon = -1;
    public static int resId_placePickerFragment = -1;
    public static int resId_placePickerFragmentAttrs = -1;
    public static int resId_placePickerFragmentListRow = -1;
    public static int resId_placePickerFragmentRadiusInMeters = -1;
    public static int resId_placePickerFragmentResultsLimit = -1;
    public static int resId_placePickerFragmentSearchBoxStub = -1;
    public static int resId_placePickerFragmentSearchText = -1;
    public static int resId_placePickerFragmentShowSearchBox = -1;
    public static int resId_placePickerSubtitleCatalogOnlyFormat = -1;
    public static int resId_placePickerSubtitleFormat = -1;
    public static int resId_placePickerSubtitleWereHereOnlyFormat = -1;
    public static int resId_profileDefaultIcon = -1;
    public static int resId_profilePictureBlankPortrait = -1;
    public static int resId_profilePictureBlankSquare = -1;
    public static int resId_profilePictureIsCropped = -1;
    public static int resId_profilePictureLarge = -1;
    public static int resId_profilePictureNormal = -1;
    public static int resId_profilePicturePresetSize = -1;
    public static int resId_profilePictureSmall = -1;
    public static int resId_profilePictureView = -1;
    public static int resId_requestErrorPasswordChanged = -1;
    public static int resId_requestErrorPermissions = -1;
    public static int resId_requestErrorReconnect = -1;
    public static int resId_requestErrorRelogin = -1;
    public static int resId_requestErrorWebLogin = -1;
    //public static int resId_searchBox = -1;
    public static int resId_userSettingsFragment = -1;
    public static int resId_userSettingsFragmentConnectedShadowColor = -1;
    public static int resId_userSettingsFragmentConnectedTextColor = -1;
    public static int resId_userSettingsFragmentLoggedIn = -1;
    public static int resId_userSettingsFragmentLoginButton = -1;
    public static int resId_userSettingsFragmentNotConnectedTextColor = -1;
    public static int resId_userSettingsFragmentNotLoggedIn = -1;
    public static int resId_userSettingsFragmentProfileName = -1;
    public static int resId_userSettingsFragmentProfilePictureHeight = -1;
    public static int resId_userSettingsFragmentProfilePictureWidth = -1;

    // Returns true iff all items in subset are in superset, treating null and
    // empty collections as
    // the same.
    public static <T> boolean isSubset(Collection<T> subset, Collection<T> superset) {
        if ((superset == null) || (superset.size() == 0)) {
            return ((subset == null) || (subset.size() == 0));
        }

        HashSet<T> hash = new HashSet<T>(superset);
        for (T t : subset) {
            if (!hash.contains(t)) {
                return false;
            }
        }
        return true;
    }

    public static <T> boolean isNullOrEmpty(Collection<T> c) {
        return (c == null) || (c.size() == 0);
    }

    public static boolean isNullOrEmpty(String s) {
        return (s == null) || (s.length() == 0);
    }

    public static <T> Collection<T> unmodifiableCollection(T... ts) {
        return Collections.unmodifiableCollection(Arrays.asList(ts));
    }

    public static <T> ArrayList<T> arrayList(T... ts) {
        ArrayList<T> arrayList = new ArrayList<T>(ts.length);
        for (T t : ts) {
            arrayList.add(t);
        }
        return arrayList;
    }

    static String md5hash(String key) {
        return hashWithAlgorithm(HASH_ALGORITHM_MD5, key);
    }

    private static String sha1hash(String key) {
        return hashWithAlgorithm(HASH_ALGORITHM_SHA1, key);
    }

    private static String hashWithAlgorithm(String algorithm, String key) {
        MessageDigest hash = null;
        try {
            hash = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }

        hash.update(key.getBytes());
        byte[] digest = hash.digest();
        StringBuilder builder = new StringBuilder();
        for (int b : digest) {
            builder.append(Integer.toHexString((b >> 4) & 0xf));
            builder.append(Integer.toHexString((b >> 0) & 0xf));
        }
        return builder.toString();
    }

    public static Uri buildUri(String authority, String path, Bundle parameters) {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(URL_SCHEME);
        builder.authority(authority);
        builder.path(path);
        for (String key : parameters.keySet()) {
            Object parameter = parameters.get(key);
            if (parameter instanceof String) {
                builder.appendQueryParameter(key, (String) parameter);
            }
        }
        return builder.build();
    }

    public static void putObjectInBundle(Bundle bundle, String key, Object value) {
        if (value instanceof String) {
            bundle.putString(key, (String) value);
        } else if (value instanceof Parcelable) {
            bundle.putParcelable(key, (Parcelable) value);
        } else if (value instanceof byte[]) {
            bundle.putByteArray(key, (byte[]) value);
        } else {
            throw new FacebookException("attempted to add unsupported type to Bundle");
        }
    }

    public static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException ioe) {
            // ignore
        }
    }

    public static void disconnectQuietly(URLConnection connection) {
        if (connection instanceof HttpURLConnection) {
            ((HttpURLConnection)connection).disconnect();
        }
    }

    public static String getMetadataApplicationId(Context context) {
        Validate.notNull(context, "context");

        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);
            if (ai.metaData != null) {
                return ai.metaData.getString(Session.APPLICATION_ID_PROPERTY);
            }
        } catch (PackageManager.NameNotFoundException e) {
            // if we can't find it in the manifest, just return null
        }

        return null;
    }

    static Map<String, Object> convertJSONObjectToHashMap(JSONObject jsonObject) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        JSONArray keys = jsonObject.names();
        for (int i = 0; i < keys.length(); ++i) {
            String key;
            try {
                key = keys.getString(i);
                Object value = jsonObject.get(key);
                if (value instanceof JSONObject) {
                    value = convertJSONObjectToHashMap((JSONObject) value);
                }
                map.put(key, value);
            } catch (JSONException e) {
            }
        }
        return map;
    }

    // Returns either a JSONObject or JSONArray representation of the 'key' property of 'jsonObject'.
    public static Object getStringPropertyAsJSON(JSONObject jsonObject, String key, String nonJSONPropertyKey)
            throws JSONException {
        Object value = jsonObject.opt(key);
        if (value != null && value instanceof String) {
            JSONTokener tokener = new JSONTokener((String) value);
            value = tokener.nextValue();
        }

        if (value != null && !(value instanceof JSONObject || value instanceof JSONArray)) {
            if (nonJSONPropertyKey != null) {
                // Facebook sometimes gives us back a non-JSON value such as
                // literal "true" or "false" as a result.
                // If we got something like that, we present it to the caller as
                // a GraphObject with a single
                // property. We only do this if the caller wants that behavior.
                jsonObject = new JSONObject();
                jsonObject.putOpt(nonJSONPropertyKey, value);
                return jsonObject;
            } else {
                throw new FacebookException("Got an unexpected non-JSON object.");
            }
        }

        return value;

    }

    public static String readStreamToString(InputStream inputStream) throws IOException {
        BufferedInputStream bufferedInputStream = null;
        InputStreamReader reader = null;
        try {
            bufferedInputStream = new BufferedInputStream(inputStream);
            reader = new InputStreamReader(bufferedInputStream);
            StringBuilder stringBuilder = new StringBuilder();

            final int bufferSize = 1024 * 2;
            char[] buffer = new char[bufferSize];
            int n = 0;
            while ((n = reader.read(buffer)) != -1) {
                stringBuilder.append(buffer, 0, n);
            }

            return stringBuilder.toString();
        } finally {
            closeQuietly(bufferedInputStream);
            closeQuietly(reader);
        }
    }

    public static boolean stringsEqualOrEmpty(String a, String b) {
        boolean aEmpty = TextUtils.isEmpty(a);
        boolean bEmpty = TextUtils.isEmpty(b);

        if (aEmpty && bEmpty) {
            // Both null or empty, they match.
            return true;
        }
        if (!aEmpty && !bEmpty) {
            // Both non-empty, check equality.
            return a.equals(b);
        }
        // One empty, one non-empty, can't match.
        return false;
    }

    private static void clearCookiesForDomain(Context context, String domain) {
        // This is to work around a bug where CookieManager may fail to instantiate if CookieSyncManager
        // has never been created.
        CookieSyncManager syncManager = CookieSyncManager.createInstance(context);
        syncManager.sync();

        CookieManager cookieManager = CookieManager.getInstance();

        String cookies = cookieManager.getCookie(domain);
        if (cookies == null) {
            return;
        }

        String[] splitCookies = cookies.split(";");
        for (String cookie : splitCookies) {
            String[] cookieParts = cookie.split("=");
            if (cookieParts.length > 0) {
                String newCookie = cookieParts[0].trim() + "=;expires=Sat, 1 Jan 2000 00:00:01 UTC;";
                cookieManager.setCookie(domain, newCookie);
            }
        }
        cookieManager.removeExpiredCookie();
    }

    public static void clearFacebookCookies(Context context) {
        // setCookie acts differently when trying to expire cookies between builds of Android that are using
        // Chromium HTTP stack and those that are not. Using both of these domains to ensure it works on both.
        clearCookiesForDomain(context, "facebook.com");
        clearCookiesForDomain(context, ".facebook.com");
        clearCookiesForDomain(context, "https://facebook.com");
        clearCookiesForDomain(context, "https://.facebook.com");
    }

    public static void logd(String tag, Exception e) {
        if (BuildConfig.DEBUG && tag != null && e != null) {
            Log.d(tag, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    public static void logd(String tag, String msg) {
        if (BuildConfig.DEBUG && tag != null && msg != null) {
            Log.d(tag, msg);
        }
    }

    public static <T> boolean areObjectsEqual(T a, T b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    // Note that this method makes a synchronous Graph API call, so should not be called from the main thread.
    public static FetchedAppSettings queryAppSettings(final String applicationId, final boolean forceRequery) {

        // Cache the last app checked results.
        if (!forceRequery && fetchedAppSettings.containsKey(applicationId)) {
            return fetchedAppSettings.get(applicationId);
        }

        Bundle appSettingsParams = new Bundle();
        appSettingsParams.putString(APPLICATION_FIELDS, TextUtils.join(",", APP_SETTING_FIELDS));

        Request request = Request.newGraphPathRequest(null, applicationId, null);
        request.setParameters(appSettingsParams);

        GraphObject supportResponse = request.executeAndWait().getGraphObject();
        FetchedAppSettings result = new FetchedAppSettings(
                safeGetBooleanFromResponse(supportResponse, SUPPORTS_ATTRIBUTION),
                safeGetBooleanFromResponse(supportResponse, SUPPORTS_IMPLICIT_SDK_LOGGING));

        fetchedAppSettings.put(applicationId, result);

        return result;
    }

    private static boolean safeGetBooleanFromResponse(GraphObject response, String propertyName) {
        Object result = false;
        if (response != null) {
            result = response.getProperty(propertyName);
        }
        if (!(result instanceof Boolean)) {
            result = false;
        }
        return (Boolean) result;
    }

    public static void clearCaches(Context context) {
        ImageDownloader.clearCache(context);
    }

    public static void deleteDirectory(File directoryOrFile) {
        if (!directoryOrFile.exists()) {
            return;
        }

        if (directoryOrFile.isDirectory()) {
            for (File child : directoryOrFile.listFiles()) {
                deleteDirectory(child);
            }
        }
        directoryOrFile.delete();
    }

    public static <T> List<T> asListNoNulls(T... array) {
        ArrayList<T> result = new ArrayList<T>();
        for (T t : array) {
            if (t != null) {
                result.add(t);
            }
        }
        return result;
    }

    public static void loadResourceIds(Context context) {
       String packageName = context.getPackageName();
       Resources resources = context.getResources();

       resId_blueColor = resources.getIdentifier("com_facebook_blue", "color", packageName);
       resId_chooseFriends = resources.getIdentifier("com_facebook_choose_friends", "string", packageName);
       resId_close = resources.getIdentifier("com_facebook_close", "drawable", packageName);
       resId_errorMessage = resources.getIdentifier("com_facebook_internet_permission_error_message", "string", packageName);
       resId_errorTitle = resources.getIdentifier("com_facebook_internet_permission_error_title", "string", packageName);
       resId_friendPickerFragment = resources.getIdentifier("com_facebook_friendpickerfragment", "layout", packageName);
       resId_friendPickerFragmentMultiSelect = resources.getIdentifier("com_facebook_friend_picker_fragment_multi_select", "styleable", packageName);
       resId_friendPickerFragmentStyleable = resources.getIdentifier("com_facebook_friend_picker_fragment", "styleable", packageName);
       resId_inverseIcon = resources.getIdentifier("com_facebook_inverse_icon", "drawable", packageName);
       resId_loading = resources.getIdentifier("com_facebook_loading", "string", packageName);
       resId_loginActivityLayout = resources.getIdentifier("com_facebook_login_activity_layout", "layout", packageName);
       resId_loginActivityProgressBar = resources.getIdentifier("com_facebook_login_activity_progress_bar", "id", packageName);
       resId_loginButtonImage = resources.getIdentifier("com_facebook_loginbutton_blue", "drawable", packageName);
       resId_loginView = resources.getIdentifier("com_facebook_login_view", "styleable", packageName);
       resId_loginViewCancelAction = resources.getIdentifier("com_facebook_loginview_cancel_action", "string", packageName);
       resId_loginviewCompoundPadding = resources.getIdentifier("com_facebook_loginview_compound_drawable_padding", "dimen", packageName);
       resId_loginViewConfirmLogout = resources.getIdentifier("com_facebook_login_view_confirm_logout", "styleable", packageName);
       resId_loginViewFetchUserInfo = resources.getIdentifier("com_facebook_login_view_fetch_user_info", "styleable", packageName);
       resId_loginViewHeight = resources.getIdentifier("com_facebook_loginview_height", "dimen", packageName);
       resId_loginViewPaddingBottom = resources.getIdentifier("com_facebook_loginview_padding_bottom", "dimen", packageName);
       resId_loginViewPaddingLeft = resources.getIdentifier("com_facebook_loginview_padding_left", "dimen", packageName);
       resId_loginViewPaddingRight = resources.getIdentifier("com_facebook_loginview_padding_right", "dimen", packageName);
       resId_loginViewPaddingTop = resources.getIdentifier("com_facebook_loginview_padding_top", "dimen", packageName);
       resId_loginViewTextColor = resources.getIdentifier("com_facebook_loginview_text_color", "color", packageName);
       resId_loginViewTextSize = resources.getIdentifier("com_facebook_loginview_text_size", "dimen", packageName);
       resId_loginViewLoggedInAs = resources.getIdentifier("com_facebook_loginview_logged_in_as", "string", packageName);
       resId_loginViewLoggedUsingFacebook = resources.getIdentifier("com_facebook_loginview_logged_in_using_facebook", "string", packageName);
       resId_loginViewLoginButton = resources.getIdentifier("com_facebook_loginview_log_in_button", "string", packageName);
       resId_loginViewLoginText = resources.getIdentifier("com_facebook_login_view_login_text", "styleable", packageName);
       resId_loginViewLogoutAction = resources.getIdentifier("com_facebook_loginview_log_out_action", "string", packageName);
       resId_loginViewLogoutButton = resources.getIdentifier("com_facebook_loginview_log_out_button", "string", packageName);
       resId_loginViewLogoutText = resources.getIdentifier("com_facebook_login_view_logout_text", "styleable", packageName);
       resId_loginViewWidth = resources.getIdentifier("com_facebook_loginview_width", "dimen", packageName);
       resId_nearby = resources.getIdentifier("com_facebook_nearby", "string", packageName);
       resId_pickerActivityCircle = resources.getIdentifier("com_facebook_picker_activity_circle", "id", packageName);
       resId_pickerCheckbox = resources.getIdentifier("com_facebook_picker_checkbox", "id", packageName);
       resId_pickerCheckboxStub = resources.getIdentifier("com_facebook_picker_checkbox_stub", "id", packageName);
       resId_pickerDoneButton = resources.getIdentifier("com_facebook_picker_done_button", "id", packageName);
       resId_pickerDoneButtonText = resources.getIdentifier("com_facebook_picker_done_button_text", "id", packageName);
       resId_pickerImage = resources.getIdentifier("com_facebook_picker_image", "image", packageName);
       resId_pickerListRow = resources.getIdentifier("com_facebook_picker_list_row", "layout", packageName);
       resId_pickerListSectionHeader = resources.getIdentifier("com_facebook_picker_list_section_header", "layout", packageName);
       resId_pickerListView = resources.getIdentifier("com_facebook_picker_list_view", "id", packageName);
       resId_pickerProfilePicStub = resources.getIdentifier("com_facebook_picker_profile_pic_stub", "id", packageName);
       resId_pickerRowActivityCircle = resources.getIdentifier("com_facebook_picker_row_activity_circle", "id", packageName);
       resId_pickerSearchBox = resources.getIdentifier("com_facebook_picker_search_box", "id", packageName);
       resId_pickerSearchText = resources.getIdentifier("com_facebook_picker_search_text", "id", packageName);
       resId_pickerSubTitle = resources.getIdentifier("picker_subtitle", "id", packageName);
       resId_pickerTitle = resources.getIdentifier("com_facebook_picker_title", "id", packageName);
       resId_pickerTitleBar = resources.getIdentifier("com_facebook_picker_title_bar", "id", packageName);
       resId_pickerTitleBarStub = resources.getIdentifier("com_facebook_picker_title_bar_stub", "id", packageName);
       resId_placeDefaultIcon = resources.getIdentifier("com_facebook_place_default_icon", "drawable", packageName);
       resId_placePickerFragment = resources.getIdentifier("com_facebook_placepickerfragment", "layout", packageName);
       resId_placePickerFragmentAttrs = resources.getIdentifier("com_facebook_place_picker_fragment", "styleable", packageName);
       resId_placePickerFragmentListRow = resources.getIdentifier("com_facebook_placepickerfragment_list_row", "layout", packageName);
       resId_placePickerFragmentRadiusInMeters = resources.getIdentifier("com_facebook_place_picker_fragment_radius_in_meters", "styleable", packageName);
       resId_placePickerFragmentResultsLimit = resources.getIdentifier("com_facebook_place_picker_fragment_results_limit", "styleable", packageName);
       //resId_placePickerFragmentSearchBoxStub = resources.getIdentifier("com_facebook_placepickerfragment_search_box_stub", "id", packageName);
       resId_placePickerFragmentSearchText = resources.getIdentifier("com_facebook_place_picker_fragment_search_text", "styleable", packageName);
       resId_placePickerFragmentShowSearchBox = resources.getIdentifier("com_facebook_place_picker_fragment_show_search_box", "styleable", packageName);
       resId_placePickerSubtitleCatalogOnlyFormat = resources.getIdentifier("com_facebook_placepicker_subtitle_catetory_only_format", "string", packageName);
       resId_placePickerSubtitleFormat = resources.getIdentifier("com_facebook_placepicker_subtitle_format", "string", packageName);
       resId_placePickerSubtitleWereHereOnlyFormat = resources.getIdentifier("com_facebook_placepicker_subtitle_were_here_only_format", "string", packageName);
       resId_profileDefaultIcon = resources.getIdentifier("com_facebook_profile_default_icon", "drawable", packageName);
       resId_profilePictureBlankPortrait = resources.getIdentifier("com_facebook_profile_picture_blank_portrait", "drawable", packageName);
       resId_profilePictureBlankSquare = resources.getIdentifier("com_facebook_profile_picture_blank_square", "drawable", packageName);
       resId_profilePictureIsCropped = resources.getIdentifier("com_facebook_profile_picture_view_is_cropped", "styleable", packageName);
       resId_profilePictureLarge = resources.getIdentifier("com_facebook_profilepictureview_preset_size_large", "dimen", packageName);
       resId_profilePictureNormal = resources.getIdentifier("com_facebook_profilepictureview_preset_size_normal", "dimen", packageName);
       resId_profilePicturePresetSize = resources.getIdentifier("com_facebook_profile_picture_view_preset_size", "styleable", packageName);
       resId_profilePictureSmall = resources.getIdentifier("com_facebook_profilepictureview_preset_size_small", "dimen", packageName);
       resId_profilePictureView = resources.getIdentifier("com_facebook_profile_picture_view", "styleable", packageName);
       resId_requestErrorPasswordChanged = resources.getIdentifier("com_facebook_requesterror_password_changed", "string", packageName);
       resId_requestErrorPermissions = resources.getIdentifier("com_facebook_requesterror_permissions", "string", packageName);
       resId_requestErrorReconnect = resources.getIdentifier("com_facebook_requesterror_reconnect", "string", packageName);
       resId_requestErrorRelogin = resources.getIdentifier("com_facebook_requesterror_relogin", "string", packageName);
       resId_requestErrorWebLogin = resources.getIdentifier("com_facebook_requesterror_web_login", "string", packageName);
       //resId_searchBox = resources.getIdentifier("search_box", "id", packageName);
       resId_userSettingsFragment = resources.getIdentifier("com_facebook_usersettingsfragment", "layout", packageName);
       resId_userSettingsFragmentConnectedShadowColor = resources.getIdentifier("com_facebook_usersettingsfragment_connected_shadow_color", "color", packageName);
       resId_userSettingsFragmentConnectedTextColor = resources.getIdentifier("com_facebook_usersettingsfragment_connected_text_color", "color", packageName);
       resId_userSettingsFragmentLoggedIn = resources.getIdentifier("com_facebook_usersettingsfragment_logged_in", "string", packageName);
       resId_userSettingsFragmentLoginButton = resources.getIdentifier("com_facebook_usersettingsfragment_login_button", "id", packageName);
       resId_userSettingsFragmentNotConnectedTextColor = resources.getIdentifier("com_facebook_usersettingsfragment_not_connected_text_color", "color", packageName);
       resId_userSettingsFragmentNotLoggedIn = resources.getIdentifier("com_facebook_usersettingsfragment_not_logged_in", "string", packageName);
       resId_userSettingsFragmentProfileName = resources.getIdentifier("com_facebook_usersettingsfragment_profile_name", "id", packageName);
       resId_userSettingsFragmentProfilePictureHeight = resources.getIdentifier("com_facebook_usersettingsfragment_profile_picture_height", "dimen", packageName);
       resId_userSettingsFragmentProfilePictureWidth = resources.getIdentifier("com_facebook_usersettingsfragment_profile_picture_width", "dimen", packageName);
   }
    // Return a hash of the android_id combined with the appid.  Intended to dedupe requests on the server side
    // in order to do counting of users unknown to Facebook.  Because we put the appid into the key prior to hashing,
    // we cannot do correlation of the same user across multiple apps -- this is intentional.  When we transition to
    // the Google advertising ID, we'll get rid of this and always send that up.
    public static String getHashedDeviceAndAppID(Context context, String applicationId) {
        String androidId = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);

        if (androidId == null) {
            return null;
        } else {
            return sha1hash(androidId + applicationId);
        }
    }

    public static void setAppEventAttributionParameters(GraphObject params,
            String attributionId, String hashedDeviceAndAppId, boolean limitEventUsage) {
        // Send attributionID if it exists, otherwise send a hashed device+appid specific value as the advertiser_id.
        if (attributionId != null) {
            params.setProperty("attribution", attributionId);
        } else if (hashedDeviceAndAppId != null) {
            params.setProperty("advertiser_id", hashedDeviceAndAppId);
        }

        params.setProperty("application_tracking_enabled", !limitEventUsage);
    }
}
