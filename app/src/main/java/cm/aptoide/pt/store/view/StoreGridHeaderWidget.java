package cm.aptoide.pt.store.view;

import android.content.SharedPreferences;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import cm.aptoide.pt.AptoideApplication;
import cm.aptoide.pt.R;
import cm.aptoide.pt.analytics.Analytics;
import cm.aptoide.pt.dataprovider.model.v7.Event;
import cm.aptoide.pt.dataprovider.model.v7.GetStoreWidgets;
import cm.aptoide.pt.dataprovider.util.CommentType;
import cm.aptoide.pt.dataprovider.ws.v7.V7;
import cm.aptoide.pt.dataprovider.ws.v7.store.StoreContext;
import cm.aptoide.pt.store.StoreTheme;
import cm.aptoide.pt.view.Translator;
import cm.aptoide.pt.view.recycler.widget.Widget;
import com.jakewharton.rxbinding.view.RxView;

public class StoreGridHeaderWidget extends Widget<StoreGridHeaderDisplayable> {

  private TextView title;
  private Button more;

  public StoreGridHeaderWidget(View itemView) {
    super(itemView);
  }

  @Override protected void assignViews(View itemView) {
    title = (TextView) itemView.findViewById(R.id.title);
    more = (Button) itemView.findViewById(R.id.more);
  }

  @Override public void bindView(StoreGridHeaderDisplayable displayable) {
    final GetStoreWidgets.WSWidget wsWidget = displayable.getWsWidget();
    final boolean moreIsVisible = wsWidget.hasActions();
    final AptoideApplication application =
        (AptoideApplication) getContext().getApplicationContext();
    final String marketName = application.getMarketName();
    final SharedPreferences sharedPreferences = application.getDefaultSharedPreferences();
    title.setText(Translator.translate(wsWidget.getTitle(), getContext().getApplicationContext(),
        marketName));

    StoreGridHeaderDisplayable.Model model = displayable.getModel();
    more.setTextColor(getContext().getResources()
        .getColor(StoreTheme.get(model.getStoreTheme())
            .getColorLetters()));
    more.setVisibility(moreIsVisible && model.isMoreVisible() ? View.VISIBLE : View.GONE);

    if (moreIsVisible) {
      compositeSubscription.add(RxView.clicks(more)
          .subscribe(a -> {

            final Event event = wsWidget.getActions()
                .get(0)
                .getEvent();
            final String storeTheme = displayable.getStoreTheme();
            final String tag = displayable.getTag();
            final StoreContext storeContext = displayable.getStoreContext();
            final String title = wsWidget.getTitle();
            final Fragment fragment;

            if (event.getName() == Event.Name.listComments) {
              String action = event.getAction();
              String url =
                  action != null ? action.replace(V7.getHost(sharedPreferences), "") : null;
              fragment = AptoideApplication.getFragmentProvider()
                  .newCommentGridRecyclerFragmentUrl(CommentType.STORE, url, "View Comments",
                      storeContext);
            } else {
              displayable.getStoreTabNavigator()
                  .navigateToStoreTabGridRecyclerView(event, title, storeTheme, tag, storeContext);
            }

            getFragmentNavigator().navigateTo(fragment, true);
            Analytics.AppViewViewedFrom.addStepToList(wsWidget.getTag());
          }));
    }
  }
}
