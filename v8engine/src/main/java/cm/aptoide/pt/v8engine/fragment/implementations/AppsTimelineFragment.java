/*
 * Copyright (c) 2016.
 * Modified by SithEngineer on 18/08/2016.
 */

package cm.aptoide.pt.v8engine.fragment.implementations;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.UiThread;
import android.support.v7.widget.GridLayoutManager;
import android.view.View;
import cm.aptoide.accountmanager.AptoideAccountManager;
import cm.aptoide.pt.actions.PermissionManager;
import cm.aptoide.pt.crashreports.CrashReport;
import cm.aptoide.pt.database.accessors.AccessorFactory;
import cm.aptoide.pt.database.realm.Installed;
import cm.aptoide.pt.dataprovider.repository.IdsRepositoryImpl;
import cm.aptoide.pt.dataprovider.util.ErrorUtils;
import cm.aptoide.pt.dataprovider.ws.v7.BodyInterceptor;
import cm.aptoide.pt.downloadmanager.AptoideDownloadManager;
import cm.aptoide.pt.model.v7.Datalist;
import cm.aptoide.pt.model.v7.timeline.TimelineCard;
import cm.aptoide.pt.navigation.AccountNavigator;
import cm.aptoide.pt.preferences.secure.SecurePreferencesImplementation;
import cm.aptoide.pt.utils.design.ShowMessage;
import cm.aptoide.pt.v8engine.BaseBodyInterceptor;
import cm.aptoide.pt.v8engine.InstallManager;
import cm.aptoide.pt.v8engine.R;
import cm.aptoide.pt.v8engine.V8Engine;
import cm.aptoide.pt.v8engine.analytics.Analytics;
import cm.aptoide.pt.v8engine.fragment.GridRecyclerSwipeFragment;
import cm.aptoide.pt.v8engine.install.Installer;
import cm.aptoide.pt.v8engine.install.InstallerFactory;
import cm.aptoide.pt.v8engine.interfaces.StoreCredentialsProvider;
import cm.aptoide.pt.v8engine.link.LinksHandlerFactory;
import cm.aptoide.pt.v8engine.repository.PackageRepository;
import cm.aptoide.pt.v8engine.repository.SocialRepository;
import cm.aptoide.pt.v8engine.repository.TimelineAnalytics;
import cm.aptoide.pt.v8engine.repository.TimelineCardFilter;
import cm.aptoide.pt.v8engine.repository.TimelineRepository;
import cm.aptoide.pt.v8engine.util.CardToDisplayable;
import cm.aptoide.pt.v8engine.util.CardToDisplayableConverter;
import cm.aptoide.pt.v8engine.util.DownloadFactory;
import cm.aptoide.pt.v8engine.util.StoreCredentialsProviderImpl;
import cm.aptoide.pt.v8engine.view.recycler.base.BaseAdapter;
import cm.aptoide.pt.v8engine.view.recycler.displayable.Displayable;
import cm.aptoide.pt.v8engine.view.recycler.displayable.SpannableFactory;
import cm.aptoide.pt.v8engine.view.recycler.displayable.implementations.grid.DateCalculator;
import cm.aptoide.pt.v8engine.view.recycler.displayable.implementations.grid.TimeLineStatsDisplayable;
import cm.aptoide.pt.v8engine.view.recycler.displayable.implementations.grid.TimelineLoginDisplayable;
import cm.aptoide.pt.v8engine.view.recycler.listeners.RxEndlessRecyclerView;
import com.facebook.appevents.AppEventsLogger;
import com.jakewharton.rxrelay.BehaviorRelay;
import com.trello.rxlifecycle.android.FragmentEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by marcelobenites on 6/17/16.
 */
public class AppsTimelineFragment<T extends BaseAdapter> extends GridRecyclerSwipeFragment<T> {

  public static final int SEARCH_LIMIT = 20;

  private static final String USER_ID_KEY = "USER_ID_KEY";
  private static final String ACTION_KEY = "ACTION";
  private static final String STORE_ID = "STORE_ID";
  private static final String PACKAGE_LIST_KEY = "PACKAGE_LIST";
  private static final String LIST_STATE_KEY = "LIST_STATE";

  private DownloadFactory downloadFactory;
  private SpannableFactory spannableFactory;
  private LinksHandlerFactory linksHandlerFactory;
  private DateCalculator dateCalculator;
  private boolean isLoading;
  private int offset;
  private int total;
  private TimelineRepository timelineRepository;
  private PackageRepository packageRepository;
  private List<String> packages;
  private TimelineAnalytics timelineAnalytics;
  private AptoideAccountManager accountManager;
  private AccountNavigator accountNavigator;
  private long storeId;
  private CardToDisplayable cardToDisplayable;
  private BehaviorRelay<Boolean> refreshSubject;
  private Parcelable listState;

  public static AppsTimelineFragment newInstance(String action, Long userId, long storeId) {
    AppsTimelineFragment fragment = new AppsTimelineFragment();

    final Bundle args = new Bundle();
    args.putLong(STORE_ID, storeId);
    args.putString(ACTION_KEY, action);
    if (userId != null) {
      args.putLong(USER_ID_KEY, userId);
    }
    fragment.setArguments(args);
    return fragment;
  }

  @Override public void loadExtras(Bundle args) {
    super.loadExtras(args);
    storeId = args.getLong(STORE_ID);
  }

  @Override public void onDestroyView() {
    listState = getRecyclerView().getLayoutManager().onSaveInstanceState();
    super.onDestroyView();
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    accountNavigator = new AccountNavigator(getContext(), getNavigationManager(), accountManager);

    if (savedInstanceState != null) {
      if (savedInstanceState.containsKey(PACKAGE_LIST_KEY)) {
        packages = Arrays.asList(savedInstanceState.getStringArray(PACKAGE_LIST_KEY));
      }
    }

    Observable<List<String>> packagesObservable =
        (packages != null) ? Observable.just(packages) : refreshPackages();

    Observable<Datalist<Displayable>> displayableObservable = accountManager.getAccountAsync()
        .map(account -> account.isLoggedIn())
        .onErrorReturn(throwable -> false)
        .flatMapObservable(loggedIn -> packagesObservable.observeOn(AndroidSchedulers.mainThread())
            .flatMap(packages -> Observable.merge(refreshSubject.flatMap(
                refreshed -> clearView().flatMap(
                    refresh -> getFreshDisplayables(refreshed, packages, loggedIn))),
                getNextDisplayables(packages))));

    displayableObservable.observeOn(AndroidSchedulers.mainThread())
        .compose(bindUntilEvent(FragmentEvent.DESTROY_VIEW))
        .doOnNext(items -> {
          addItems(items);
          if (listState != null) {
            getRecyclerView().getLayoutManager().onRestoreInstanceState(listState);
            listState = null;
          }
          if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(LIST_STATE_KEY)) {
              getRecyclerView().getLayoutManager()
                  .onRestoreInstanceState(savedInstanceState.getParcelable(LIST_STATE_KEY));
              savedInstanceState.putParcelable(LIST_STATE_KEY, null);
            }
          }
        })
        .subscribe(__ -> {
        }, err -> {
          CrashReport.getInstance().log(err);
          finishLoading(err);
        });
  }

  @Override public void load(boolean create, boolean refresh, Bundle savedInstanceState) {
    super.load(create, refresh, savedInstanceState);
    if (create || refresh) {
      refreshSubject.call(refresh);
    }
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    if (packages != null) {
      outState.putStringArray(PACKAGE_LIST_KEY, packages.toArray(new String[packages.size()]));
    }

    outState.putParcelable(LIST_STATE_KEY,
        getRecyclerView().getLayoutManager().onSaveInstanceState());

    super.onSaveInstanceState(outState);
  }

  @UiThread private Observable<Void> clearView() {
    return Observable.fromCallable(() -> {
      clearDisplayables();
      return null;
    });
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final Context applicationContext = getContext().getApplicationContext();

    accountManager = ((V8Engine) applicationContext).getAccountManager();

    final IdsRepositoryImpl idsRepository =
        new IdsRepositoryImpl(SecurePreferencesImplementation.getInstance(), getContext());
    final BodyInterceptor bodyInterceptor = new BaseBodyInterceptor(idsRepository, accountManager);

    timelineAnalytics = new TimelineAnalytics(Analytics.getInstance(),
        AppEventsLogger.newLogger(applicationContext), bodyInterceptor);
    dateCalculator = new DateCalculator();
    spannableFactory = new SpannableFactory();
    downloadFactory = new DownloadFactory();
    linksHandlerFactory = new LinksHandlerFactory();
    packageRepository = new PackageRepository(getContext().getPackageManager());

    final PermissionManager permissionManager = new PermissionManager();
    final Installer installer =
        new InstallerFactory().create(getContext(), InstallerFactory.ROLLBACK);
    final SocialRepository socialRepository = new SocialRepository(accountManager, bodyInterceptor);
    final StoreCredentialsProvider storeCredentialsProvider = new StoreCredentialsProviderImpl();
    final InstallManager installManager =
        new InstallManager(AptoideDownloadManager.getInstance(), installer);

    timelineRepository = new TimelineRepository(getArguments().getString(ACTION_KEY),
        new TimelineCardFilter(new TimelineCardFilter.TimelineCardDuplicateFilter(new HashSet<>()),
            AccessorFactory.getAccessorFor(Installed.class)), bodyInterceptor);

    cardToDisplayable =
        new CardToDisplayableConverter(socialRepository, timelineAnalytics, installManager,
            permissionManager, accountManager, idsRepository, bodyInterceptor,
            storeCredentialsProvider);

    refreshSubject = BehaviorRelay.create();
  }

  @NonNull private Observable<List<String>> refreshPackages() {
    return Observable.concat(packageRepository.getLatestInstalledPackages(10),
        packageRepository.getRandomInstalledPackages(10))
        .toList()
        .doOnNext(packages -> setPackages(packages));
  }

  private void setPackages(List<String> packages) {
    this.packages = packages;
  }

  @NonNull private Observable<Datalist<Displayable>> getFreshDisplayables(boolean refresh,
      List<String> packages, boolean loggedIn) {
    return getDisplayableList(packages, 0, refresh).flatMap(displayableDatalist -> {
      Long userId =
          getArguments().containsKey(USER_ID_KEY) ? getArguments().getLong(USER_ID_KEY) : null;

      if (loggedIn || userId != null) {
        return getUserTimelineStats(refresh, displayableDatalist, userId);
      } else {
        displayableDatalist.getList()
            .add(0, new TimelineLoginDisplayable().setAccountNavigator(accountNavigator));
        return Observable.just(displayableDatalist);
      }
    });
  }

  @NonNull private Observable<Datalist<Displayable>> getUserTimelineStats(boolean refresh,
      Datalist<Displayable> displayableDatalist, Long userId) {
    return timelineRepository.getTimelineStats(refresh, userId).map(timelineStats -> {
      TimeLineStatsDisplayable timeLineStatsDisplayable =
          new TimeLineStatsDisplayable(timelineStats, userId, spannableFactory, storeTheme,
              timelineAnalytics, userId == null, storeId);
      displayableDatalist.getList().add(0, timeLineStatsDisplayable);
      return displayableDatalist;
    }).onErrorReturn(throwable -> {
      CrashReport.getInstance().log(throwable);
      return displayableDatalist;
    });
  }

  @Override public void reload() {
    super.reload();
    Analytics.AppsTimeline.pullToRefresh();
  }

  private Observable<Datalist<Displayable>> getNextDisplayables(List<String> packages) {
    return RxEndlessRecyclerView.loadMore(getRecyclerView(), getAdapter())
        .filter(item -> onStartLoadNext())
        .observeOn(AndroidSchedulers.mainThread())
        .concatMap(item -> getDisplayableList(packages, getOffset(), false))
        .delay(1, TimeUnit.SECONDS)
        .retryWhen(
            errors -> errors.delay(1, TimeUnit.SECONDS).filter(error -> onStopLoadNext(error)))
        .subscribeOn(AndroidSchedulers.mainThread());
  }

  @NonNull
  private Observable<Datalist<Displayable>> getDisplayableList(List<String> packages, int offset,
      boolean refresh) {
    return timelineRepository.getTimelineCards(SEARCH_LIMIT, offset, packages, refresh)
        .flatMap(datalist -> Observable.just(datalist)
            .flatMapIterable(dataList -> dataList.getList())
            .map(card -> cardToDisplayable.convert(card, dateCalculator, spannableFactory,
                downloadFactory, linksHandlerFactory))
            .toList()
            .map(list -> createDisplayableDataList(datalist, list)));
  }

  private Datalist<Displayable> createDisplayableDataList(Datalist<TimelineCard> datalist,
      List<Displayable> list) {
    Datalist<Displayable> displayableDataList = new Datalist<>();
    displayableDataList.setNext(datalist.getNext());
    displayableDataList.setCount(datalist.getCount());
    displayableDataList.setHidden(datalist.getHidden());
    displayableDataList.setTotal(datalist.getTotal());
    displayableDataList.setLimit(datalist.getLimit());
    displayableDataList.setOffset(datalist.getOffset());
    displayableDataList.setLoaded(datalist.isLoaded());
    displayableDataList.setList(list);
    return displayableDataList;
  }

  @UiThread private void showErrorSnackbar(Throwable error) {
    @StringRes int errorString;
    if (ErrorUtils.isNoNetworkConnection(error)) {
      errorString = R.string.fragment_social_timeline_no_connection;
    } else {
      errorString = R.string.fragment_social_timeline_general_error;
    }
    ShowMessage.asSnack(getView(), errorString);
  }

  private boolean isTotal() {
    return offset >= total;
  }

  public void setTotal(Datalist<Displayable> dataList) {
    if (dataList != null && dataList.getTotal() != 0) {
      total = dataList.getTotal();
    }
  }

  private int getOffset() {
    return offset;
  }

  private void setOffset(Datalist<Displayable> dataList) {
    if (dataList != null && dataList.getNext() != 0) {
      offset = dataList.getNext();
    }
  }

  protected boolean isLoading() {
    return isLoading;
  }

  protected void setLoading(boolean loading) {
    this.isLoading = loading;
  }

  @UiThread private void addItems(Datalist<Displayable> data) {
    setLoading(false);
    setTotal(data);
    setOffset(data);
    addDisplayables(data.getList(), true);
  }

  @UiThread @NonNull private boolean onStopLoadNext(Throwable error) {
    if (isLoading()) {
      setLoading(false);
      showErrorSnackbar(error);
      return true;
    }
    return false;
  }

  @UiThread @NonNull private boolean onStartLoadNext() {
    if (!isTotal() && !isLoading()) {
      setLoading(true);
      Analytics.AppsTimeline.endlessScrollLoadMore();
      return true;
    } else if (isTotal()) {
      //TODO - When you reach the end of the endless?
      // Snackbar.make(getView(), "No more cards!", Snackbar.LENGTH_SHORT).show();
    }
    return false;
  }

  @UiThread public void goToTop() {
    GridLayoutManager layoutManager = ((GridLayoutManager) getRecyclerView().getLayoutManager());
    int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
    if (lastVisibleItemPosition > 10) {
      getRecyclerView().scrollToPosition(10);
    }
    getRecyclerView().smoothScrollToPosition(0);
  }
}
