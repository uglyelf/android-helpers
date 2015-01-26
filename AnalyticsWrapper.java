package com.example.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;

import com.example.R;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

/**
 * Created by gregory randolph on 1/22/15.
 *
 * A singleton to hold the google analytics (v4) functionality.
 * 
 * Currently supports a single Tracker. I'd like to update it to support
 * App, Global, and Commerce trackers.
 * 
 * Created because updating from Google Analytics v2 to v4 was a bitch.
 * Use a wrapper! 
 * Now if I have to update to a future version, I'll be able to affect 
 * most changes here.
 * 
 * Do to a bug in Google Analytics, if you want auto-activity-tracking you must both set the flag
 * in the xml: <bool name="ga_autoActivityTracking">true</bool> 
 * AND ALSO call: GoogleAnalytics.getInstance(this).enableAutoActivityReports(this);
 * I do so in an extension of Application, but you can do it where you like.
 * Make sure you set your screenName tags in your xml as well.
 *
 * For those who care:
 * I instantiate this by calling getAnalyticsWrapper(this) in onCreate() from MyApplication, a class
 * that extends Application, and does nothing but instantiate this and set enableAutoActivityReports
 * 
 */

public class AnalyticsWrapper {
    public static final String USER_CONSENT = "userConsent";
    private static final String TAG = "Example.AnalyticsWrapper";
    private static volatile AnalyticsWrapper mInstance;

    private GoogleAnalytics mAnalytics;
    private Context mContext;
    private Tracker mTracker;

    private AnalyticsWrapper() {
    }

    /**
     * Singleton getter / lazy init.
     * @param context used to initialize the wrapper and get user consent.
     * @return the instance of the AnalyticsWrapper
     */
    public synchronized static AnalyticsWrapper getAnalyticsWrapper(Context context) {
        Log.d(TAG, "Entered getAnalyticsWrapper from context: " + context.getClass().getName());
        
        final SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context);

        /* If you want to enable user opt-out of anonymous data sharing, make sure the
        * app is on a screen that's visible, and probably make sure they're logged in.
        * up to you though.*/
//        AuthManager am = AuthManager.getInstance(this);
//        if((context instanceof <activity on the ui thread>) && !prefs.contains(USER_CONSENT) && am.isSignedIn()){
//            askUserConsent(context, prefs);
//        }
        /* Otherwise, use this to set a user-consent flag to true - make sure your terms of service
         * include data gathering. */
        if (!prefs.contains(USER_CONSENT)){
            prefs.edit().putBoolean(USER_CONSENT, true).apply();
        }

        if (mInstance == null) {
            Log.d(TAG, "Analytics instance null. Creating instance!");
            mInstance = new AnalyticsWrapper();
            mInstance.setContext(context);
            mInstance.setAnalytics(GoogleAnalytics.getInstance(context));
            mInstance.setTracker(mInstance.getAnalytics().newTracker(R.xml.analytics));

                /*
                    Per the documentation:
                    "Sets or resets the application-level opt out flag.
                    If set, no hits will be sent to Google Analytics.
                    The value of this flag will not persist across application starts.
                    The correct value should thus be set in application initialization code."
                */
            boolean userConsent = prefs.getBoolean(USER_CONSENT, true);
            if (userConsent) {
                mInstance.getAnalytics().setAppOptOut(false);
            } else {
                mInstance.getAnalytics().setAppOptOut(true);
            }

            Log.d(TAG, "Finished creating instance.");
        }
        Log.d(TAG, "Exiting AnalyticsWrapper from context: " + context.getClass().getName());
        return mInstance;
    }

    /**
     * send an exception to google analytics
     * used for non-fatal exceptions (calls sendException(description, false))
     *
     * @param description
     */
    public synchronized void sendException(String description) {
        sendException(description, false);
    }

    /**
     * Send an exception to Google Analytics
     * @param description The description of the exception
     * @param isFatal Whether the exception was fatal or caught (probably always false)
     */
    public void sendException(String description, boolean isFatal) {
        mTracker.send(new HitBuilders.ExceptionBuilder()
                .setDescription(description)
                .setFatal(isFatal)
                .build());
    }

    /**
     * Send an event to google analytics. Such as a click, a download, etc.
     * @param category
     * @param action
     * @param label
     */
    public synchronized void sendEvent(String category, String action, String label){
        sendEvent(category, action, label, 1);
    }

    /**
     * Send an event to google analytics. Such as a click, a download, etc.
     *
     * @param category
     * @param action
     * @param label
     * @param value
     */
    public void sendEvent(String category, String action, String label, long value) {
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action)
                .setLabel(label)
                .setValue(value)
                .build());
    }

    /**
     * Send a screen view to Google Analytics.
     * Clear the screen from the tracker before returning.
     *
     * @param path
     */
    public void sendView(String path) {
//        synchronized (AnalyticsWrapper.class) {
            mTracker.setScreenName(path);
            mTracker.send(new HitBuilders.AppViewBuilder().build());
            mTracker.setScreenName(null);
//        }
    }

    /**
     * Sometimes you need to send a view and an event at the same time. This method supports
     * doing so without clearing the screen path from the tracker until after the event is sent.
     *
     * @param path
     * @param category
     * @param action
     * @param label
     */
    public void sendViewAndEvent(String path, String category, String action, String label){
//        synchronized (AnalyticsWrapper.class) {
            mTracker.setScreenName(path);
            mTracker.send(new HitBuilders.AppViewBuilder().build());

            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory(category)
                    .setAction(action)
                    .setLabel(label)
                    .setValue(1)
                    .build());

            mTracker.setScreenName(null);
//        }
    }

    /**
     * If the user hasn't given permission to collect anonymous data, ask them for it.
     *
     * @param context
     * @param prefs
     */
    private static void askUserConsent(Context context, final SharedPreferences prefs) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle("Sending anonymous data").setMessage("Help us improve! Send anonymous usage data?");

        builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                prefs.edit().putBoolean(USER_CONSENT, true).apply();
            }
        }).setNegativeButton(android.R.string.cancel, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                prefs.edit().putBoolean(USER_CONSENT, false).apply();
            }
        });

        builder.show();
    }

    private void setAnalytics(GoogleAnalytics analytics) {
        this.mAnalytics = analytics;
    }

    private GoogleAnalytics getAnalytics() {
        return mAnalytics;
    }

    private void setContext(Context context) {
        this.mContext = context;
    }

    private void setTracker(Tracker tracker) {
        this.mTracker = tracker;
    }
}
