// Contributor(s): Owen
// Main: Owen - Shared activity transition options for tab and stack navigation.

package com.example.workshop6.util;

import android.app.Activity;
import android.content.Intent;

import androidx.core.app.ActivityOptionsCompat;

import com.example.workshop6.R;

/**
 * Consistent horizontal slide animations between activities.
 * Forward brings the next screen in from the right.
 * Backward brings the previous screen in from the left for flows such as returning to sign-in.
 */
public final class NavTransitions {

    private NavTransitions() {
    }

    /**
     * Starts {@code intent} from {@code from} then applies {@link #applyForwardPending(Activity)}.
     */
    public static void startActivityWithForward(Activity from, Intent intent) {
        from.startActivity(intent);
        applyForwardPending(from);
    }

    /**
     * Starts {@code intent} with a back-stack feel then applies {@link #applyBackwardPending(Activity)}.
     */
    public static void startActivityWithBackward(Activity from, Intent intent) {
        from.startActivity(intent);
        applyBackwardPending(from);
    }

    /**
     * {@link ActivityOptionsCompat} for launching with the same slide-in-right animation as forward navigation.
     */
    public static ActivityOptionsCompat forwardLaunchOptions(Activity activity) {
        return ActivityOptionsCompat.makeCustomAnimation(
                activity,
                R.anim.activity_slide_in_right,
                R.anim.activity_slide_out_left);
    }

    /** Call on the activity that called {@code startActivity} so the pending transition runs. */
    public static void applyForwardPending(Activity activity) {
        activity.overridePendingTransition(
                R.anim.activity_slide_in_right,
                R.anim.activity_slide_out_left);
    }

    /** Call immediately after {@code finish} when popping back inside a flow. */
    public static void applyBackwardPending(Activity activity) {
        activity.overridePendingTransition(
                R.anim.activity_slide_in_left,
                R.anim.activity_slide_out_right);
    }
}
