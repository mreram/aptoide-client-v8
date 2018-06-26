/*
 * Copyright (c) 2016.
 * Modified by Neurophobic Animal on 09/05/2016.
 */

package cm.aptoide.pt.app.view;

import android.support.annotation.LayoutRes;
import cm.aptoide.pt.R;
import cm.aptoide.analytics.implementation.navigation.NavigationTracker;
import cm.aptoide.pt.dataprovider.model.v7.Type;
import cm.aptoide.pt.dataprovider.model.v7.listapp.App;
import cm.aptoide.pt.dataprovider.ws.v7.store.StoreContext;
import cm.aptoide.pt.view.recycler.displayable.DisplayablePojo;

/**
 * Created on 28/04/16.
 *
 * @author SithEngineer
 */
public class GridAppDisplayable extends DisplayablePojo<App> {

  private String tag;
  private NavigationTracker navigationTracker;
  private StoreContext storeContext;

  public GridAppDisplayable() {
  }

  public GridAppDisplayable(App pojo, String tag, NavigationTracker navigationTracker,
      StoreContext storeContext) {
    super(pojo);
    this.tag = tag;
    this.navigationTracker = navigationTracker;
    this.storeContext = storeContext;
  }

  @Override protected Configs getConfig() {
    return new Configs(Type.APPS_GROUP.getDefaultPerLineCount(),
        Type.APPS_GROUP.isFixedPerLineCount());
  }

  @LayoutRes @Override public int getViewLayout() {
    return R.layout.displayable_grid_app;
  }

  public NavigationTracker getNavigationTracker() {
    return navigationTracker;
  }

  public StoreContext getStoreContext() {
    return storeContext;
  }

  public String getTag() {
    return this.tag;
  }
}
