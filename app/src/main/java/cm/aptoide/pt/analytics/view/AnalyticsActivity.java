/*
 * Copyright (c) 2016.
 * Modified on 29/08/2016.
 */

package cm.aptoide.pt.analytics.view;

import android.os.Build;
import android.os.Bundle;
import cm.aptoide.pt.analytics.FirstLaunchAnalytics;
import cm.aptoide.analytics.AnalyticsManager;
import cm.aptoide.pt.crashreports.CrashReport;
import cm.aptoide.pt.crashreports.CrashlyticsCrashLogger;
import cm.aptoide.pt.dataprovider.ads.AdNetworkUtils;
import cm.aptoide.pt.permission.PermissionProviderActivity;
import javax.inject.Inject;

public abstract class AnalyticsActivity extends PermissionProviderActivity {

  @Inject AnalyticsManager analyticsManager;
  @Inject FirstLaunchAnalytics firstLaunchAnalytics;
  private boolean _resumed = false;

  public boolean is_resumed() {
    return _resumed;
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getActivityComponent().inject(this);
    if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
      ((CrashlyticsCrashLogger) CrashReport.getInstance()
          .getLogger(CrashlyticsCrashLogger.class)).setLanguage(
          getResources().getConfiguration().locale.getLanguage());
    } else {
      ((CrashlyticsCrashLogger) CrashReport.getInstance()
          .getLogger(CrashlyticsCrashLogger.class)).setLanguage(getResources().getConfiguration()
          .getLocales()
          .get(0)
          .getLanguage());
    }

    firstLaunchAnalytics.setGmsPresent(AdNetworkUtils.isGooglePlayServicesAvailable(this));
  }

  @Override protected void onStart() {
    super.onStart();
    analyticsManager.startSession();
  }

  @Override protected void onResume() {
    super.onResume();
    _resumed = true;
  }

  @Override protected void onPause() {
    super.onPause();
    _resumed = false;
  }

  @Override protected void onStop() {
    super.onStop();
    analyticsManager.endSession();
  }
}
