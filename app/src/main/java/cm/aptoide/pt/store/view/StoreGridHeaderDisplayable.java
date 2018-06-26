package cm.aptoide.pt.store.view;

import cm.aptoide.pt.R;
import cm.aptoide.analytics.implementation.navigation.NavigationTracker;
import cm.aptoide.pt.dataprovider.model.v7.GetStoreWidgets;
import cm.aptoide.pt.dataprovider.ws.v7.store.StoreContext;
import cm.aptoide.pt.view.recycler.displayable.Displayable;
import lombok.Getter;
import lombok.Setter;

public class StoreGridHeaderDisplayable extends Displayable {

  private final GetStoreWidgets.WSWidget wsWidget;
  private final StoreTabNavigator storeTabNavigator;
  private final Model model;
  private NavigationTracker navigationTracker;

  // this constructor is necessary due to reflection code that generates displayables. that code
  // needs to go as this.
  public StoreGridHeaderDisplayable() {
    this(null, null, null, null, null);
  }

  public StoreGridHeaderDisplayable(GetStoreWidgets.WSWidget wsWidget, String storeTheme,
      String tag, StoreContext storeContext, StoreTabNavigator storeTabNavigator,
      NavigationTracker navigationTracker) {
    this.model = new Model(storeTheme, tag, storeContext);
    this.wsWidget = wsWidget;
    this.storeTabNavigator = storeTabNavigator;
    this.navigationTracker = navigationTracker;
  }

  public StoreGridHeaderDisplayable(GetStoreWidgets.WSWidget wsWidget,
      StoreTabNavigator storeTabNavigator, NavigationTracker navigationTracker) {
    this(wsWidget, null, null, null, storeTabNavigator, navigationTracker);
  }

  @Override protected Displayable.Configs getConfig() {
    return new Configs(1, true);
  }

  @Override public int getViewLayout() {
    return R.layout.displayable_grid_header;
  }

  public GetStoreWidgets.WSWidget getWsWidget() {
    return this.wsWidget;
  }

  public StoreTabNavigator getStoreTabNavigator() {
    return this.storeTabNavigator;
  }

  public Model getModel() {
    return model;
  }

  public NavigationTracker getNavigationTracker() {
    return navigationTracker;
  }
}
