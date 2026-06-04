package com.sotaynauan.ai.util;

import android.app.Activity;
import android.content.Intent;

public final class AppNavigator {
    private AppNavigator() {
    }

    public static void openTopLevel(Activity activity, Class<? extends Activity> target) {
        if (activity.getClass().equals(target)) {
            return;
        }
        Intent intent = new Intent(activity, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
        activity.finish();
    }
}
