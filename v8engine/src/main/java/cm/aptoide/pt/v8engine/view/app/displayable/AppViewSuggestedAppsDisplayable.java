/*
 * Copyright (c) 2016.
 * Modified on 04/08/2016.
 */

package cm.aptoide.pt.v8engine.view.app.displayable;

import cm.aptoide.pt.database.realm.MinimalAd;
import cm.aptoide.pt.dataprovider.model.v7.listapp.App;
import cm.aptoide.pt.v8engine.R;
import cm.aptoide.pt.v8engine.view.recycler.displayable.Displayable;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created on 04/05/16.
 */
@EqualsAndHashCode(callSuper = true) @Data public class AppViewSuggestedAppsDisplayable
    extends Displayable {

  private List<MinimalAd> minimalAds;
  private List<App> appsList;
  private String appName;

  public AppViewSuggestedAppsDisplayable() {
  }

  public AppViewSuggestedAppsDisplayable(List<MinimalAd> minimalAds, List<App> appsList,
      String appName) {
    this.minimalAds = minimalAds;
    this.appsList = appsList;
    this.appName = appName;
  }

  @Override protected Displayable.Configs getConfig() {
    return new Configs(1, true);
  }

  @Override public int getViewLayout() {
    return R.layout.displayable_app_view_suggested_apps;
  }

  public String getAppName() {
    return appName;
  }
}