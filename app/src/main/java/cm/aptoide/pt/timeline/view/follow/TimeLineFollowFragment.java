package cm.aptoide.pt.timeline.view.follow;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import cm.aptoide.analytics.implementation.navigation.ScreenTagHistory;
import cm.aptoide.pt.R;
import cm.aptoide.pt.dataprovider.model.v7.GetFollowers;
import cm.aptoide.pt.dataprovider.ws.v7.V7;
import cm.aptoide.pt.dataprovider.ws.v7.store.StoreContext;
import cm.aptoide.pt.view.fragment.GridRecyclerSwipeWithToolbarFragment;
import cm.aptoide.pt.view.recycler.EndlessRecyclerOnScrollListener;
import cm.aptoide.pt.view.recycler.displayable.Displayable;
import cm.aptoide.pt.view.recycler.displayable.MessageWhiteBgDisplayable;
import java.util.LinkedList;
import java.util.List;
import rx.functions.Action1;

/**
 * Created by trinkes on 16/12/2016.
 */

public abstract class TimeLineFollowFragment extends GridRecyclerSwipeWithToolbarFragment {

  private EndlessRecyclerOnScrollListener endlessRecyclerOnScrollListener;
  private StoreContext storeContext;

  @NonNull protected static Bundle buildBundle(StoreContext storeContext) {
    Bundle args = new Bundle();
    args.putSerializable(BundleKeys.STORE_CONTEXT, storeContext);
    return args;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }

  @Override protected boolean displayHomeUpAsEnabled() {
    return true;
  }

  @Override public void loadExtras(Bundle args) {
    super.loadExtras(args);
    storeContext = ((StoreContext) args.getSerializable(BundleKeys.STORE_CONTEXT));
  }

  @Override public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.menu_empty, menu);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    int itemId = item.getItemId();
    if (itemId == android.R.id.home) {
      getActivity().onBackPressed();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override public ScreenTagHistory getHistoryTracker() {
    return ScreenTagHistory.Builder.build(this.getClass()
        .getSimpleName(), "", storeContext != null ? storeContext.name() : null);
  }

  @Override public void onDestroyView() {
    endlessRecyclerOnScrollListener.removeListeners();
    super.onDestroyView();
  }

  @Override public void load(boolean create, boolean refresh, Bundle savedInstanceState) {
    super.load(create, refresh, savedInstanceState);
    if (create || refresh) {

      V7 request = buildRequest();
      LinkedList<Displayable> dispList = new LinkedList<>();

      final int[] hidden = { 0 };
      Action1<GetFollowers> action = (followersList) -> {
        hidden[0] += followersList.getDataList()
            .getHidden();
        for (GetFollowers.TimelineUser user : followersList.getDataList()
            .getList()) {
          dispList.add(createUserDisplayable(user));
        }
        addDisplayables(dispList);
        dispList.clear();
      };

      EndlessRecyclerOnScrollListener.BooleanAction<GetFollowers> firstRequest =
          getFirstResponseAction(dispList);
      getRecyclerView().clearOnScrollListeners();
      endlessRecyclerOnScrollListener =
          new EndlessRecyclerOnScrollListener(this.getAdapter(), request, action,
              Throwable::printStackTrace, 6, true, firstRequest, null);
      endlessRecyclerOnScrollListener.addOnEndlessFinishListener(
          __ -> addDisplayable(new MessageWhiteBgDisplayable(getFooterMessage(hidden[0]))));
      getRecyclerView().addOnScrollListener(endlessRecyclerOnScrollListener);
      endlessRecyclerOnScrollListener.onLoadMore(refresh, refresh);
    } else {
      getRecyclerView().addOnScrollListener(endlessRecyclerOnScrollListener);
    }
  }

  abstract protected V7 buildRequest();

  protected abstract Displayable createUserDisplayable(GetFollowers.TimelineUser user);

  protected abstract EndlessRecyclerOnScrollListener.BooleanAction<GetFollowers> getFirstResponseAction(
      List<Displayable> dispList);

  protected abstract String getFooterMessage(int hidden);

  protected abstract String getHeaderMessage();

  public class BundleKeys {
    public static final String USER_ID = "user_id";
    public static final String CARD_UID = "CARDUID";
    public static final String NUMBER_LIKES = "NUMBER_LIKES";
    public static final String STORE_ID = "STORE_ID";
    public static final String STORE_CONTEXT = "STORE_CONTEXT";
  }
}
