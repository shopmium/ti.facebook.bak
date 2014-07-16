/**
 * Facebook Module
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Please see the LICENSE included with this distribution for details.
 */

package facebook;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Date;
import java.util.HashMap;
import java.util.Collection;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollObject;
import org.appcelerator.kroll.KrollRuntime;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.CurrentActivityListener;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.titanium.TiBaseActivity;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiBlob;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.io.TiBaseFile;
import org.appcelerator.titanium.util.TiActivityResultHandler;
import org.appcelerator.titanium.util.TiActivitySupport;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiUIHelper;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.AsyncFacebookRunner.RequestListener;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;
import com.facebook.android.Util;
import com.facebook.internal.Utility;
import com.facebook.Settings;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.Settings;
import com.facebook.widget.WebDialog;
import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.widget.FacebookDialog;
import com.facebook.UiLifecycleHelper;




@Kroll.module(name="Facebook", id="facebook")
public class FacebookModule extends KrollModule implements TiActivityResultHandler
{
	protected static final String TAG = "FacebookModule";

    @Kroll.constant public static final int BUTTON_STYLE_NORMAL = 0;
    @Kroll.constant public static final int BUTTON_STYLE_WIDE = 1;

    public static final String EVENT_LOGIN = "login";
    public static final String EVENT_LOGOUT = "logout";
    public static final String PROPERTY_SUCCESS = "success";
    public static final String PROPERTY_CANCELLED = "cancelled";
    public static final String PROPERTY_ERROR = "error";
    public static final String PROPERTY_CODE = "code";
    public static final String PROPERTY_DATA = "data";
    public static final String PROPERTY_UID = "uid";
    public static final String PROPERTY_RESULT = "result";
    public static final String PROPERTY_PATH = "path";
    public static final String PROPERTY_METHOD = "method";
    protected static final int MSG_INVOKE_CALLBACK = KrollModule.MSG_LAST_ID + 100;
    @Kroll.constant public static final int UNKNOWN_ERROR = -1;
    protected static final int MSG_LAST_ID = MSG_INVOKE_CALLBACK;
    public static KrollObject callbackContext;
    public static KrollFunction successCallback, cancelCallback, errorCallback;
    private static final List<String> PERMISSIONS = Arrays.asList("publish_actions");

	protected Facebook facebook = null;
	protected String uid = null;
	protected WeakReference<Context> loginContext = null; // Facebook authorize and logout should use same context.

	private boolean loggedIn = false;
	private ArrayList<TiFacebookStateListener> stateListeners = new ArrayList<TiFacebookStateListener>();
	private SessionListener sessionListener = null;
	private AsyncFacebookRunner fbrunner;
	private String appid = null;
	private String[] permissions = new String[]{};
	private String[] publishPerm = new String[]{"publish_actions"};
	private boolean forceDialogAuth = true;
	private UiLifecycleHelper uiHelper;
	protected int requestCode;

	public FacebookModule()
	{
		super();
		Utility.loadResourceIds(TiApplication.getInstance());
		sessionListener = new SessionListener(this);
		SessionEvents.addAuthListener(sessionListener);
		SessionEvents.addLogoutListener(sessionListener);
		debug("FacebookModule()");
		appid = SessionStore.getSavedAppId(TiApplication.getInstance());
		if (appid != null) {
			debug("Attempting session restore for appid " + appid);
			facebook = new Facebook(appid);
			SessionStore.restore(this, TiApplication.getInstance());
			if (facebook.isSessionValid()) {
				debug("Session restore succeeded.  Now logged in.");
				loggedIn = true;
			} else {
				debug("Session restore failed.  Not logged in.");
				loggedIn = false;
			}
		}
	}

	public FacebookModule(TiContext tiContext)
	{
		this();
	}

	@Kroll.getProperty @Kroll.method
	public boolean getLoggedIn()
	{
		return isLoggedIn();
	}

	@Kroll.getProperty @Kroll.method
	public String getAccessToken()
	{
		if (facebook != null) {
			return facebook.getAccessToken();
		} else {
			return null;
		}
	}

	@Kroll.getProperty @Kroll.method
	public String getAppid()
	{
		return appid;
	}

	@Kroll.setProperty @Kroll.method
	public void setAppid(String appid)
	{
		if (this.appid != null && !this.appid.equals(appid)) {
			if (facebook != null && facebook.isSessionValid()) {
				// A facebook session existed, but the appid was changed.  Any session info
				// should be destroyed.
				Log.w(TAG, "Appid was changed while session active.  Removing session info.");
				destroyFacebookSession();
				facebook = null;
			}
		}
		this.appid = appid;
		if (facebook == null || !facebook.getAppId().equals(appid)) {
			facebook = new Facebook(appid);
		}
	}

	@Kroll.getProperty @Kroll.method
	public String getUid()
	{
		return uid;
	}

	@Kroll.getProperty @Kroll.method
	public String[] getPermissions()
	{
		return permissions;
	}

	@Kroll.setProperty @Kroll.method
	public void setPermissions(String[] permissions)
	{
		this.permissions = permissions;
	}

	@Kroll.getProperty @Kroll.method
	public Date getExpirationDate()
	{
		if (facebook != null) {
			return TiConvert.toDate(facebook.getAccessExpires());
		} else {
			return new Date(0);
		}
	}

	@Kroll.getProperty @Kroll.method
	public boolean getForceDialogAuth()
	{
		return forceDialogAuth;
	}

	@Kroll.setProperty @Kroll.method
	public void setForceDialogAuth(boolean value)
	{
		this.forceDialogAuth = value;
	}

	@Kroll.method
	public TiFacebookModuleLoginButtonProxy createLoginButton(@Kroll.argument(optional=true) KrollDict options)
	{
		TiFacebookModuleLoginButtonProxy login = new TiFacebookModuleLoginButtonProxy(this);
		if (options != null) {
			login.extend(options);
		}
		return login;
	}

	@Kroll.method
	public void authorize()
	{
		debug("authorize; permissions.length == " + permissions.length);
		if (this.isLoggedIn()) {
			// if already authorized, this should do nothing
			debug("Already logged in, ignoring authorize() request");
			return;
		}

		if (appid == null) {
			Log.w(TAG, "authorize() called without appid being set; throwing...");
			throw new IllegalStateException("missing appid");
		}

		// forget session in case this fails.
		SessionStore.clear(TiApplication.getInstance());

		if (facebook == null) {
			facebook = new Facebook(appid);
		}

		// Important to be done on the current activity since it will display dialog.
		TiUIHelper.waitForCurrentActivity(new CurrentActivityListener() {
			@Override
			public void onCurrentActivityReady(Activity activity)
			{
				executeAuthorize(activity);
			}
		});
	}

	@Kroll.method
	public void logout()
	{
		boolean wasLoggedIn = isLoggedIn();
		destroyFacebookSession();
		if (facebook != null && wasLoggedIn) {
			SessionEvents.onLogoutBegin();
			executeLogout();
		} else {
			loginContext = null;
		}
	}

	@Kroll.method
	public void requestWithGraphPath(String path, KrollDict params, String httpMethod, KrollFunction callback)
	{
		if (facebook == null) {
			Log.w(TAG, "requestWithGraphPath called without Facebook being instantiated.  Have you set appid?");
			return;
		}
		AsyncFacebookRunner runner = getFBRunner();
		Bundle paramBundle = Utils.mapToBundle(params);
		if (httpMethod == null || httpMethod.length() == 0) {
			httpMethod = "GET";
		}
		runner.request(path, paramBundle, httpMethod.toUpperCase(), new TiRequestListener(this, path, true, callback), null);
	}

	@Kroll.method
	public void request(String method, KrollDict params, KrollFunction callback)
	{
		if (facebook == null) {
			Log.w(TAG, "request called without Facebook being instantiated.  Have you set appid?");
			return;
		}

		String httpMethod = "GET";
		if (params != null) {
			for (Object v : params.values()) {
				if (v instanceof TiBlob || v instanceof TiBaseFile) {
					httpMethod = "POST";
					break;
				}
			}
		}

		Bundle bundle = Utils.mapToBundle(params);
		if (!bundle.containsKey("method")) {
			bundle.putString("method", method);
		}
		getFBRunner().request(null, bundle, httpMethod, new TiRequestListener(this, method, false, callback), null);
	}

	@Kroll.method
	public void dialog(final String action, final KrollDict params, final KrollFunction callback)
	{
		if (facebook == null) {
			Log.w(TAG, "dialog called without Facebook being instantiated.  Have you set appid?");
			return;
		}

		TiUIHelper.waitForCurrentActivity(new CurrentActivityListener() {
			@Override
			public void onCurrentActivityReady(Activity activity)
			{
				final Activity fActivity = activity;
				fActivity.runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						facebook.dialog(fActivity, action, Utils.mapToBundle(params),
							new TiDialogListener(FacebookModule.this, callback, action));
					}
				});
			}
		});
	}

	@Kroll.method
	public void publishInstall()
	{
		if (appid == null) {
			Log.w(TAG, "Trying publishInstall without appid. Have you set appid?");
			return;
		}

		Context context = TiApplication.getInstance().getApplicationContext();

		Log.d(TAG, " == Facebook publishInstall", Log.DEBUG_MODE);
		Log.d(TAG, " ==== appid: " + appid, Log.DEBUG_MODE);
		Log.d(TAG, " ==== context: " + context, Log.DEBUG_MODE);
		Settings.publishInstallAsync(context, appid);
	}

	protected void completeLogin()
	{
		getFBRunner().request("me", new RequestListener()
		{
			@Override
			public void onMalformedURLException(MalformedURLException e, Object state)
			{
				loginError(e);
			}

			@Override
			public void onIOException(IOException e, Object state)
			{
				loginError(e);
			}

			@Override
			public void onFileNotFoundException(FileNotFoundException e, Object state)
			{
				loginError(e);
			}

			@Override
			public void onFacebookError(FacebookError e, Object state)
			{
				loginError(e);
			}

			@Override
			public void onComplete(String response, Object state)
			{
				try {
					debug("onComplete (getting 'me'): " + response);
					JSONObject json = Util.parseJson(response);
					uid = json.getString("id");
					loggedIn = true;
					SessionStore.save(FacebookModule.this, TiApplication.getInstance());
					KrollDict data = new KrollDict();
					data.put(PROPERTY_CANCELLED, false);
					data.put(PROPERTY_SUCCESS, true);
					data.put(PROPERTY_UID, uid);
					data.put(PROPERTY_DATA, response);
					data.put(PROPERTY_CODE, 0);
					fireLoginChange();
					fireEvent(EVENT_LOGIN, data);
				} catch (JSONException e) {
					Log.e(TAG, e.getMessage(), e);
				} catch (FacebookError e) {
					Log.e(TAG, e.getMessage(), e);
				}
			}
		});
	}

	protected void completeLogout()
	{
		destroyFacebookSession();
		loginContext = null;
		fireLoginChange();
		fireEvent(EVENT_LOGOUT, new KrollDict());
	}

	protected void debug(String message)
	{
		Log.d(TAG, message, Log.DEBUG_MODE);
	}

	protected void addListener(TiFacebookStateListener listener)
	{
		if (!stateListeners.contains(listener)) {
			stateListeners.add(listener);
		}
	}

	protected void removeListener(TiFacebookStateListener listener)
	{
		stateListeners.remove(listener);
	}

	protected void executeAuthorize(final Activity activity)
	{
		loginContext = new WeakReference<Context>(activity);
		final TiActivitySupport activitySupport  = (TiActivitySupport) activity;
		int activityCode;
		if (forceDialogAuth) {
			// Single sign-on support
			activityCode = Facebook.FORCE_DIALOG_AUTH;
		} else {
			activityCode = activitySupport.getUniqueResultCode();
		}
		final TiActivityResultHandler resultHandler = new TiActivityResultHandler()
		{
			@Override
			public void onResult(Activity activity, int requestCode, int resultCode, Intent data)
			{
				Log.d(TAG, "onResult from Facebook login attempt. resultCode: " + resultCode, Log.DEBUG_MODE);
				facebook.authorizeCallback(requestCode, resultCode, data);
			}
			@Override
			public void onError(Activity activity, int requestCode, Exception e)
			{
				Log.e(TAG, e.getLocalizedMessage(), e);
			}
		};

		if (TiApplication.isUIThread()) {
			//facebook.authorize(activity, permissions, activityCode, new LoginDialogListener());
			facebook.authorize(activity, activitySupport, permissions, activityCode, new LoginDialogListener(), resultHandler);
		} else {
			final int code = activityCode;
			TiMessenger.postOnMain(new Runnable(){
				@Override
				public void run()
				{
					//facebook.authorize(activity, permissions, code, new LoginDialogListener());
					facebook.authorize(activity, activitySupport, permissions, code, new LoginDialogListener(), resultHandler);
				}
			});
		}
	}

	protected void executeLogout()
	{
		Context logoutContext = null;

		// Try to use the same context as was used for login.
		if (loginContext != null) {
			logoutContext = loginContext.get();
		}

		if (logoutContext == null) {
			// Fallback by using the application context.  The reason facebook.authorize and facebook.logout
			// want a Context is because they use the CookieSyncManager, which needs a Context.
			// The CookieSyncManager anyway takes that Context and calls context.getApplicationContext()
			// for its use.
			logoutContext = TiApplication.getInstance().getApplicationContext();
		}

		getFBRunner().logout(logoutContext, new LogoutRequestListener());
	}

	private boolean isLoggedIn()
	{
		return loggedIn && facebook != null && facebook.isSessionValid();
	}

	private void loginError(Throwable t)
	{
		Log.e(TAG, t.getMessage(), t);
		loggedIn = false;
		KrollDict data = new KrollDict();
		data.put(PROPERTY_CANCELLED, false);
		data.put(PROPERTY_SUCCESS, false);
		data.put(PROPERTY_ERROR, t.getMessage());
		data.put(PROPERTY_CODE, -1);
		fireEvent(EVENT_LOGIN, data);
	}

	private void loginCancel()
	{
		debug("login canceled");
		loggedIn = false;
		KrollDict data = new KrollDict();
		data.put(PROPERTY_CANCELLED, true);
		data.put(PROPERTY_SUCCESS, false);
		data.put(PROPERTY_CODE, -1);
		fireEvent(EVENT_LOGIN, data);
	}

	private AsyncFacebookRunner getFBRunner()
	{
		if (fbrunner == null) {
			fbrunner = new AsyncFacebookRunner(facebook);
		}
		return fbrunner;
	}

	private void destroyFacebookSession()
	{
		SessionStore.clear(TiApplication.getInstance());
		uid = null;
		loggedIn = false;
	}

	private void fireLoginChange()
	{
		for (TiFacebookStateListener listener : stateListeners) {
			if (getLoggedIn()) {
				listener.login();
			} else {
				listener.logout();
			}
		}
	}

	@Override
	public void onDestroy(Activity activity)
	{
		super.onDestroy(activity);
		if (sessionListener != null) {
			SessionEvents.removeAuthListener(sessionListener);
			SessionEvents.removeLogoutListener(sessionListener);
			sessionListener = null;
		}
	}

	private final class LoginDialogListener implements DialogListener
	{
		public void onComplete(Bundle values)
		{
			debug("LoginDialogListener onComplete");
			SessionEvents.onLoginSuccess();
		}

		public void onFacebookError(FacebookError error)
		{
			String errorMessage = error.getMessage();
			// There is a bug in Facebook Android SDK 3.0. When the user cancels the login by pressing the
			// "X" button, the onFacebookError callback is called instead of onCancel.
			// http://stackoverflow.com/questions/14237157/facebookoperationcanceledexception-not-called
			if (errorMessage != null && errorMessage.indexOf("User canceled log in") > -1) {
				FacebookModule.this.loginCancel();
			} else {
				Log.e(TAG, "LoginDialogListener onFacebookError: " + error.getMessage(), error);
				SessionEvents.onLoginError(error.getMessage());
			}
			loginContext = null;
		}

		public void onError(DialogError error)
		{
			Log.e(TAG, "LoginDialogListener onError: " + error.getMessage(), error);
			loginContext = null;
			SessionEvents.onLoginError(error.getMessage());
		}

		public void onCancel()
		{
			loginContext = null;
			FacebookModule.this.loginCancel();
		}
	}

	private final class LogoutRequestListener implements RequestListener
	{
		@Override
		public void onComplete(String response, Object state)
		{
			debug("Logout request complete: " + response);
			SessionEvents.onLogoutFinish();
		}

		@Override
		public void onFacebookError(FacebookError e, Object state)
		{
			Log.e(TAG, "Logout failure: " + e.getMessage(), e);
		}

		@Override
		public void onFileNotFoundException(FileNotFoundException e, Object state)
		{
			Log.e(TAG, "Logout failure: " + e.getMessage(), e);
		}

		@Override
		public void onIOException(IOException e, Object state)
		{
			Log.e(TAG, "Logout failure: " + e.getMessage(), e);
		}

		@Override
		public void onMalformedURLException(MalformedURLException e, Object state)
		{
			Log.e(TAG, "Logout failure: " + e.getMessage(), e);
		}
	}

	/**
	 * Object that is used to wrap required fields for async processing when invoking
	 * success, error , etc callbacks
	 */
	private class CallbackWrapper
	{
		public TiBaseActivity callbackActivity;
		public KrollFunction callback;
		public KrollObject krollObject;
		public KrollDict callbackArgs;

		CallbackWrapper(TiBaseActivity callbackActivity, KrollFunction callback, KrollObject krollObject, KrollDict callbackArgs)
		{
			this.callbackActivity = callbackActivity;
			this.callback = callback;
			this.krollObject = krollObject;
			this.callbackArgs = callbackArgs;
		}
	}

	/**
	 * @see org.appcelerator.kroll.KrollProxy#handleMessage(android.os.Message)
	 */
	@Override
	public boolean handleMessage(Message message) {
		switch (message.what) {
			case MSG_INVOKE_CALLBACK: {
				CallbackWrapper callbackWrapper = (CallbackWrapper) message.obj;
				doInvokeCallback(callbackWrapper.callbackActivity, callbackWrapper.callback, callbackWrapper.krollObject, callbackWrapper.callbackArgs);

				return true;
			}
		}

		return super.handleMessage(message);
	}

	private void invokeCallback(TiBaseActivity callbackActivity, KrollFunction callback, KrollObject krollObject, KrollDict callbackArgs) {
		if (KrollRuntime.getInstance().isRuntimeThread()) {
			doInvokeCallback(callbackActivity, callback, krollObject, callbackArgs);

		} else {
			CallbackWrapper callbackWrapper = new CallbackWrapper(callbackActivity, callback, krollObject, callbackArgs);
			Message message = getRuntimeHandler().obtainMessage(MSG_INVOKE_CALLBACK, callbackWrapper);
			message.sendToTarget();
		}
	}

	private void doInvokeCallback(TiBaseActivity callbackActivity, KrollFunction callback, KrollObject krollObject, KrollDict callbackArgs) {
		if (callbackActivity.isResumed) {
			callback.callAsync(krollObject, callbackArgs);
		} else {
			CallbackWrapper callbackWrapper = new CallbackWrapper(callbackActivity, callback, krollObject, callbackArgs);
			Message message = getRuntimeHandler().obtainMessage(MSG_INVOKE_CALLBACK, callbackWrapper);
			message.sendToTarget();
		}
	}

	private boolean isSubsetOf(Collection<String> subset, Collection<String> superset) {
			for (String string : subset) {
				if (!superset.contains(string)) {
					return false;
				}
			}
		return true;
	}

	private static void errorCallbackMessage(String message) {
			if (errorCallback != null) {
				KrollDict response = new KrollDict();
				response.putCodeAndMessage(UNKNOWN_ERROR, message);
				errorCallback.callAsync(callbackContext, response);
			}
	}

	private static void cancelCallbackMessage(String message) {
			if (cancelCallback != null) {
				KrollDict response = new KrollDict();
				response.putCodeAndMessage(UNKNOWN_ERROR, message);
				cancelCallback.callAsync(callbackContext, response);
			}
	}

	private final class PublishDialogListener implements DialogListener
	{
		public void onComplete(Bundle values)
		{
			Session session = facebook.getSession();
			if (session != null) {
					List<String> permissions = session.getPermissions();
					if (isSubsetOf(PERMISSIONS, permissions)) {
						HashMap<String, String> myMap = new HashMap<String, String>();
						myMap.put("success", "facebook publish permission success");
						successCallback.callAsync(callbackContext, myMap);
					} else {
							//Cancel
							cancelCallbackMessage("user cancelled");
					}
			} else {
					errorCallbackMessage("An unexpected error, session is null");
			}
			debug("facebook PublishDialogListener onComplete");
		}

		public void onFacebookError(FacebookError error)
		{
			String errorMessage = error.getMessage();
			Log.e(TAG, "facebook PublishDialogListener onFacebookError: " + error.getMessage(), error);
			// There is a bug in Facebook Android SDK 3.0. When the user cancels the login by pressing the
			// "X" button, the onFacebookError callback is called instead of onCancel.
			// http://stackoverflow.com/questions/14237157/facebookoperationcanceledexception-not-called
			if (errorMessage != null && errorMessage.indexOf("User canceled") > -1) {
				//Cancel
				cancelCallbackMessage("user cancelled");
				return;
			} else {
				Log.e(TAG, "facebook PublishDialogListener onFacebookError: " + error.getMessage(), error);;
			}
			errorCallbackMessage("An unexpected error : "+ error.getMessage());

		}

		public void onError(DialogError error) {
			Log.e(TAG, "facebook PublishDialogListener onError: " + error.getMessage(), error);
			errorCallbackMessage("An unexpected error : "+ error.getMessage());
		}

		public void onCancel() {
			cancelCallbackMessage("user cancelled");
		}
	}


	public static FacebookDialog.Callback nativeDialogCallback = new FacebookDialog.Callback() {
			@Override
			public void onComplete(FacebookDialog.PendingCall pendingCall, Bundle data) {
				boolean resetSelections = true;
				if (FacebookDialog.getNativeDialogDidComplete(data)) {
					if (FacebookDialog.COMPLETION_GESTURE_CANCEL.equals(FacebookDialog.getNativeDialogCompletionGesture(data))) {
						// Leave selections alone if user canceled.
						resetSelections = false;
						cancelCallbackMessage("user cancelled");
					} else {
						resetSelections = false;
						HashMap<String, String> myMap = new HashMap<String, String>();
						myMap.put("success", "facebook share dialog permission success");
						successCallback.callAsync(callbackContext, myMap);
					}
				}

				if (resetSelections) {
					errorCallbackMessage("An unexpected error...");
				}
			}
			@Override
			public void onError(FacebookDialog.PendingCall pendingCall, Exception error, Bundle data) {
				errorCallbackMessage("An unexpected error : "+ error.getMessage());
			}
	};

	@Override
	public void onResult(Activity activity, int requestCode, int resultCode, Intent data)
	{
		Log.e(TAG, "facebook onResult Called");
	}

	@Override
	public void onError(Activity activity, int requestCode, Exception e)
	{
		Log.e(TAG, "facebook onError Called "+ e.getMessage());
		errorCallbackMessage("An unexpected error : "+ e.getMessage());
	}

	@Kroll.method
	public void shareDialog(HashMap options) {
		if (options.containsKey("error")) {
			errorCallback = (KrollFunction) options.get("error");
		}
		if (options.containsKey("cancel")) {
			cancelCallback = (KrollFunction) options.get("cancel");
		}
		if (options.containsKey("success")) {
			successCallback = (KrollFunction) options.get("success");
			callbackContext = getKrollObject();
			try {
				if (FacebookDialog.canPresentShareDialog(TiApplication.getInstance().getCurrentActivity(),
					FacebookDialog.ShareDialogFeature.SHARE_DIALOG)) {

						Activity activity = getTiContext().getTiApp().getCurrentActivity();
						TiActivitySupport support = (TiActivitySupport) activity;
						requestCode = support.getUniqueResultCode();

						Intent fbActivity = new Intent(activity, LaunchFacebookActivity.class);
						fbActivity.putExtra(LaunchFacebookActivity.URL, (String) options.get("url"));
						fbActivity.putExtra(LaunchFacebookActivity.TITLE, (String) options.get("title"));
						fbActivity.putExtra(LaunchFacebookActivity.MESSAGE, (String) options.get("message"));
						fbActivity.putExtra(LaunchFacebookActivity.URL_IMAGE, (String) options.get("url_image"));
						support.launchActivityForResult(fbActivity, requestCode, this);
					} else {
						// Fallback. For example, publish the post using the Feed Dialog
						Bundle params = new Bundle();
						params.putString("link", (String) options.get("url"));
						params.putString("name",  (String) options.get("title"));
						params.putString("description", (String) options.get("message"));
						params.putString("picture", (String) options.get("url_image"));
						WebDialog feedDialog = (
							new WebDialog.FeedDialogBuilder(TiApplication.getInstance().getCurrentActivity(),
								facebook.getSession(),
								params)).setOnCompleteListener(new WebDialog.OnCompleteListener() {
										@Override
										public void onComplete(Bundle values,
											FacebookException error) {
												if (error == null) {
														// When the story is posted, echo the success
														// and the post Id.
													final String postId = values.getString("post_id");
													if (postId != null) {
														Log.e(TAG, "Posted story, id: " + postId, Log.DEBUG_MODE);
															HashMap<String, String> myMap = new HashMap<String, String>();
															myMap.put("success", "facebook shareDialog success");
															successCallback.callAsync(callbackContext, myMap);
													} else {
															// User clicked the Cancel button
															Log.e(TAG, "User clicked the Cancel button ", Log.DEBUG_MODE);
															cancelCallbackMessage("user cancelled");
													}
												} else if (error instanceof FacebookOperationCanceledException) {
														// User clicked the "x" button
														Log.e(TAG, "User clicked the x button", Log.DEBUG_MODE);
														cancelCallbackMessage("user cancelled");
												} else {
													// Generic, ex: network error
													Log.e(TAG, "Error posting story", Log.DEBUG_MODE);
													errorCallbackMessage("An unexpected error");
												}
											}
										})
								.build();
							feedDialog.show();
						}
					} catch (Throwable t) {
						Log.e(TAG, "facebook catch error => "+t);
						errorCallbackMessage("An unexpected error");
					}
		} else {
			Log.e(TAG, "FacebookModule shareDialog no success method");
		}
	}

	@Kroll.method
	public void checkPublishPermission(HashMap options)
	{
		if (options.containsKey("error")) {
			errorCallback = (KrollFunction) options.get("error");
		}
		if (options.containsKey("cancel")) {
			cancelCallback = (KrollFunction) options.get("cancel");
		}
		if (options.containsKey("success")) {
			successCallback = (KrollFunction) options.get("success");
			callbackContext = getKrollObject();
					try {
						if (successCallback != null) {
							Session session = facebook.getSession();
							if (session != null) {
									List<String> permissions = session.getPermissions();
									if (!isSubsetOf(PERMISSIONS, permissions)) {

											final int REAUTH_ACTIVITY_CODE = 100;

											final TiActivityResultHandler resultHandler = new TiActivityResultHandler() {
													@Override
													public void onResult(Activity activity, int requestCode, int resultCode, Intent data) {
														Log.e(TAG, "onResult from Facebook publish request attempt. resultCode: " + resultCode, Log.DEBUG_MODE);
														facebook.authorizeCallback(requestCode, resultCode, data);
													}

													@Override
													public void onError(Activity activity, int requestCode, Exception e) {
														Log.e(TAG, e.getLocalizedMessage(), e);
													}
											};

											if (TiApplication.isUIThread()) {
												final TiActivitySupport activitySupport  = (TiActivitySupport) TiApplication.getInstance().getCurrentActivity();
												facebook.authorizePublish(TiApplication.getInstance().getCurrentActivity(), activitySupport, publishPerm, REAUTH_ACTIVITY_CODE, new PublishDialogListener(), resultHandler);
											} else {
													TiMessenger.postOnMain(new Runnable(){
														@Override
														public void run() {
															final TiActivitySupport activitySupport  = (TiActivitySupport) TiApplication.getInstance().getCurrentActivity();
															facebook.authorizePublish(TiApplication.getInstance().getCurrentActivity(), activitySupport, publishPerm, REAUTH_ACTIVITY_CODE, new PublishDialogListener(), resultHandler);
														}
													});
												}
								} else {
										HashMap<String, String> myMap = new HashMap<String, String>();
										myMap.put("success", "facebook publish permission success (already permission)");
										successCallback.callAsync(callbackContext, myMap);
								}
							} else {
									errorCallbackMessage("An unexpected error, session is null");
							}
						}
					} catch (Throwable t) {
						Log.e(TAG, "facebook catch error => "+t);
						errorCallbackMessage("An unexpected error");
					}
		} else {
			Log.e(TAG, "FacebookModule checkPublishPermission no success method");
		}
	}
}
