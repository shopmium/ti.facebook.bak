
package facebook;

import org.appcelerator.titanium.util.Log;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.widget.FacebookDialog;
import com.facebook.UiLifecycleHelper;

public class LaunchFacebookActivity extends Activity {

    private UiLifecycleHelper uiHelper;
    public static final String URL = "URL";
    public static final String TITLE = "TITLE";
    public static final String MESSAGE = "MESSAGE";
    public static final String URL_IMAGE = "URL_IMAGE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        uiHelper = new UiLifecycleHelper(this, null);
        uiHelper.onCreate(savedInstanceState);
        Intent i = getIntent();
        Bundle extras = i.getExtras();

        String url = extras.getString(URL);
        String title = extras.getString(TITLE);
        String message = extras.getString(MESSAGE);
        String url_image = extras.getString(URL_IMAGE);
        FacebookDialog shareDialog = new FacebookDialog.ShareDialogBuilder(this)
           .setLink(url)
           .setName(title)
           .setDescription(message)
           .setPicture(url_image)
           .build();
        uiHelper.trackPendingDialogCall(shareDialog.present());
    }


    @Override
    protected void onResume() {
        super.onResume();
        uiHelper.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
        uiHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      super.onActivityResult(requestCode, resultCode, data);
      uiHelper.onActivityResult(requestCode, resultCode, data, FacebookModule.nativeDialogCallback);
      setResult(resultCode);
      finish();

    }
}
