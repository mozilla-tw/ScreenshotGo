package org.mozilla.scryer.capture;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

public class RequestCaptureActivity extends AppCompatActivity {
    public static final String RESULT_EXTRA_CODE = "code";
    public static final String RESULT_EXTRA_DATA = "data";
    public static final String RESULT_EXTRA_PROMPT_SHOWN = "prompt-shown";

    private final static int REQUEST_CODE_SCREEN_CAPTURE_PERMISSION = 1;

    private long requestStartTime;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestScreenCapturePermission();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE_PERMISSION) {
            Intent intent = new Intent(getResultBroadcastAction(this));
            intent.putExtra(RESULT_EXTRA_CODE, resultCode);
            intent.putExtra(RESULT_EXTRA_DATA, data);
            intent.putExtra(RESULT_EXTRA_PROMPT_SHOWN, promptShown());
            sendBroadcast(intent);
            finish();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void requestScreenCapturePermission() {
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent intent = mediaProjectionManager.createScreenCaptureIntent();

        requestStartTime = System.currentTimeMillis();
        startActivityForResult(intent, REQUEST_CODE_SCREEN_CAPTURE_PERMISSION);
    }

    public static String getResultBroadcastAction(Context context) {
        return context.getPackageName() + ".CAPTURE";
    }

    private boolean promptShown() {
        // Assume that the prompt was shown if the response took 200ms or more to return.
        return System.currentTimeMillis() - requestStartTime > 200;
    }
}
