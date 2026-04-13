package com.example.workshop6.util;

import android.app.Activity;
import android.content.Intent;

import androidx.core.app.ActivityOptionsCompat;

import com.example.workshop6.R;

/**
 * Consistent horizontal slides: forward = new screen from the right, previous moves left;
 * backward = current exits right, previous returns from the left.
 */
public final class NavTransitions {

    private NavTransitions() {
    }

    public static void startActivityWithForward(Activity from, Intent intent) {
        from.startActivity(intent);
        applyForwardPending(from);
    }

    public static ActivityOptionsCompat forwardLaunchOptions(Activity activity) {
        return ActivityOptionsCompat.makeCustomAnimation(
                activity,
                R.anim.activity_slide_in_right,
                R.anim.activity_slide_out_left);
    }

    /** Call on the activity that started the next one, immediately after {@code startActivity}. */
    public static void applyForwardPending(Activity activity) {
        activity.overridePendingTransition(
                R.anim.activity_slide_in_right,
                R.anim.activity_slide_out_left);
    }

    /** Call immediately after {@link Activity#finish()} when popping back in a flow. */
    public static void applyBackwardPending(Activity activity) {
        activity.overridePendingTransition(
                R.anim.activity_slide_in_left,
                R.anim.activity_slide_out_right);
    }
}
