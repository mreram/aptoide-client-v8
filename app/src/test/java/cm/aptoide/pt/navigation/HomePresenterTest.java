package cm.aptoide.pt.navigation;

import cm.aptoide.accountmanager.Account;
import cm.aptoide.accountmanager.AptoideAccountManager;
import cm.aptoide.pt.crashreports.CrashReport;
import cm.aptoide.pt.home.AdClick;
import cm.aptoide.pt.home.AdMapper;
import cm.aptoide.pt.home.AppHomeEvent;
import cm.aptoide.pt.home.FakeBundleDataSource;
import cm.aptoide.pt.home.Home;
import cm.aptoide.pt.home.HomeAnalytics;
import cm.aptoide.pt.home.HomeBundle;
import cm.aptoide.pt.home.HomeBundlesModel;
import cm.aptoide.pt.home.HomeEvent;
import cm.aptoide.pt.home.HomeFragment;
import cm.aptoide.pt.home.HomeNavigator;
import cm.aptoide.pt.home.HomePresenter;
import cm.aptoide.pt.presenter.View;
import cm.aptoide.pt.view.app.Application;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rx.Single;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by jdandrade on 07/03/2018.
 */

public class HomePresenterTest {

  @Mock private HomeFragment view;
  @Mock private CrashReport crashReporter;
  @Mock private HomeNavigator homeNavigator;
  @Mock private Home home;
  @Mock private AptoideAccountManager aptoideAccountManager;
  @Mock private Account account;
  @Mock private HomeAnalytics homeAnalytics;

  private HomePresenter presenter;
  private HomeBundlesModel bundlesModel;
  private PublishSubject<View.LifecycleEvent> lifecycleEvent;
  private PublishSubject<AppHomeEvent> appClickEvent;
  private PublishSubject<AdClick> adClickEvent;
  private PublishSubject<HomeEvent> moreClickEvent;
  private PublishSubject<Object> bottomReachedEvent;
  private PublishSubject<Void> pullToRefreshEvent;
  private PublishSubject<Void> retryClickedEvent;
  private HomeBundle localTopAppsBundle;
  private Application aptoide;
  private PublishSubject<Void> imageClickEvent;
  private PublishSubject<Account> accountStatusEvent;
  private PublishSubject<HomeEvent> bundleScrolledEvent;

  @Before public void setupHomePresenter() {
    MockitoAnnotations.initMocks(this);

    lifecycleEvent = PublishSubject.create();
    appClickEvent = PublishSubject.create();
    adClickEvent = PublishSubject.create();
    moreClickEvent = PublishSubject.create();
    bottomReachedEvent = PublishSubject.create();
    pullToRefreshEvent = PublishSubject.create();
    retryClickedEvent = PublishSubject.create();
    imageClickEvent = PublishSubject.create();
    bundleScrolledEvent = PublishSubject.create();
    accountStatusEvent = PublishSubject.create();

    presenter = new HomePresenter(view, home, Schedulers.immediate(), crashReporter, homeNavigator,
        new AdMapper(), aptoideAccountManager, homeAnalytics);
    aptoide =
        new Application("Aptoide", "http://via.placeholder.com/350x150", 0, 1000, "cm.aptoide.pt",
            300, "");
    FakeBundleDataSource fakeBundleDataSource = new FakeBundleDataSource();
    bundlesModel = new HomeBundlesModel(fakeBundleDataSource.getFakeBundles(), false, 0);
    localTopAppsBundle = bundlesModel.getList()
        .get(0);

    when(view.getLifecycle()).thenReturn(lifecycleEvent);
    when(view.appClicked()).thenReturn(appClickEvent);
    when(view.recommendedAppClicked()).thenReturn(appClickEvent);
    when(view.adClicked()).thenReturn(adClickEvent);
    when(view.moreClicked()).thenReturn(moreClickEvent);
    when(view.reachesBottom()).thenReturn(bottomReachedEvent);
    when(view.refreshes()).thenReturn(pullToRefreshEvent);
    when(view.retryClicked()).thenReturn(retryClickedEvent);
    when(view.imageClick()).thenReturn(imageClickEvent);
    when(view.bundleScrolled()).thenReturn(bundleScrolledEvent);
    when(aptoideAccountManager.accountStatus()).thenReturn(accountStatusEvent);
  }

  @Test public void loadAllBundlesFromRepositoryAndLoadIntoView() {
    //Given an initialised HomePresenter
    presenter.onCreateLoadBundles();
    //When the user clicks the Home menu item
    //And loading of bundlesModel are requested
    when(home.loadHomeBundles()).thenReturn(Single.just(bundlesModel));
    lifecycleEvent.onNext(View.LifecycleEvent.CREATE);
    //Then the progress indicator should be shown
    verify(view).showLoading();
    //Then the home should be displayed
    verify(view).showBundles(bundlesModel.getList());
    //Then the progress indicator should be hidden
    verify(view).hideLoading();
  }

  @Test public void errorLoadingBundles_ShowsError() {
    //Given an initialised HomePresenter
    presenter.onCreateLoadBundles();
    //When the loading of bundlesModel is requested
    //And an unexpected error occured
    when(home.loadHomeBundles()).thenReturn(
        Single.just(new HomeBundlesModel(HomeBundlesModel.Error.GENERIC)));
    lifecycleEvent.onNext(View.LifecycleEvent.CREATE);
    //Then the generic error message should be shown in the UI
    verify(view).showGenericError();
  }

  @Test public void errorLoadingBundles_ShowsNetworkError() {
    //Given an initialised HomePresenter
    presenter.onCreateLoadBundles();
    //When the loading of bundlesModel is requested
    //And an unexpected error occured
    when(home.loadHomeBundles()).thenReturn(
        Single.just(new HomeBundlesModel(HomeBundlesModel.Error.NETWORK)));
    lifecycleEvent.onNext(View.LifecycleEvent.CREATE);
    //Then the generic error message should be shown in the UI
    verify(view).showNetworkError();
  }

  @Test public void appClicked_NavigateToAppView() {
    //Given an initialised HomePresenter
    presenter.handleAppClick();
    lifecycleEvent.onNext(View.LifecycleEvent.CREATE);
    //When an app is clicked
    appClickEvent.onNext(new AppHomeEvent(aptoide, 1, localTopAppsBundle, 3, HomeEvent.Type.APP));
    //then it should navigate to the App's detail View
    verify(homeNavigator).navigateToAppView(aptoide.getAppId(), aptoide.getPackageName(),
        aptoide.getTag());
  }

  @Test public void adClicked_NavigateToAppView() {
    //Given an initialised HomePresenter
    presenter.handleAdClick();
    lifecycleEvent.onNext(View.LifecycleEvent.CREATE);
    //When an app is clicked
    adClickEvent.onNext(null);
    //then it should navigate to the App's detail View
    verify(homeNavigator).navigateToAppView(any());
  }

  @Test public void recommendsClicked_NavigateToAppView() {
    //Given an initialised HomePresenter
    presenter.handleRecommendedAppClick();
    lifecycleEvent.onNext(View.LifecycleEvent.CREATE);
    //When an app is clicked
    appClickEvent.onNext(
        new AppHomeEvent(aptoide, 3, localTopAppsBundle, 0, HomeEvent.Type.SOCIAL_CLICK));
    //then it should navigate to the App's detail View
    verify(homeNavigator).navigateToRecommendsAppView(aptoide.getAppId(), aptoide.getPackageName(),
        aptoide.getTag(), HomeEvent.Type.SOCIAL_CLICK);
  }

  @Test public void moreClicked_NavigateToActionView() {
    HomeEvent click = new HomeEvent(localTopAppsBundle, 0, HomeEvent.Type.MORE);
    //Given an initialised HomePresenter
    presenter.handleMoreClick();
    lifecycleEvent.onNext(View.LifecycleEvent.CREATE);
    //When more in a bundle is clicked
    moreClickEvent.onNext(click);
    //Then it should send a more clicked analytics event
    verify(homeAnalytics).sendTapOnMoreInteractEvent(0, localTopAppsBundle.getTag(),
        localTopAppsBundle.getContent()
            .size());
    //Then it should navigate with the specific action behaviour
    verify(homeNavigator).navigateWithAction(click);
  }

  @Test public void bottomReached_ShowNextBundles() {
    //Given an initialised presenter with already loaded bundlesModel into the UI before
    presenter.handleBottomReached();
    when(home.loadNextHomeBundles()).thenReturn(Single.just(bundlesModel));
    when(home.hasMore()).thenReturn(true);
    lifecycleEvent.onNext(View.LifecycleEvent.CREATE);
    //When scrolling to the end of the view is reached
    //And there are more bundlesModel available to load
    bottomReachedEvent.onNext(new Object());
    //Then it should show the load more progress indicator
    verify(view).showLoadMore();
    //Then it should request the next bundlesModel to the bundlesModel repository
    verify(home).loadNextHomeBundles();
    //Then it should send a endless scroll analytics event
    verify(homeAnalytics).sendLoadMoreInteractEvent();
    //Then it should hide the load more progress indicator
    verify(view).hideShowMore();
    //Then it should show the view again with old bundlesModel and added bundlesModel, retaining list position
    verify(view).showMoreHomeBundles(bundlesModel.getList());
  }

  @Test public void bottomReached_NoMoreBundlesAvailableToShow() {
    //Given an initialised presenter with already loaded bundlesModel into the UI before
    presenter.handleBottomReached();
    when(home.loadNextHomeBundles()).thenReturn(Single.just(bundlesModel));
    when(home.hasMore()).thenReturn(false);
    lifecycleEvent.onNext(View.LifecycleEvent.CREATE);
    //When scrolling to the end of the view is reached
    //And there are no more bundlesModel available to load
    bottomReachedEvent.onNext(new Object());
    //Then it should do nothing
    verify(view, never()).showLoadMore();
    verify(home, never()).loadNextHomeBundles();
    verify(view, never()).hideShowMore();
    verify(view, never()).showMoreHomeBundles(bundlesModel.getList());
  }

  @Test public void pullToRefresh_GetFreshBundles() {
    //Given an initialised presenter with already loaded bundlesModel into the UI before
    presenter.handlePullToRefresh();
    when(home.loadFreshHomeBundles()).thenReturn(Single.just(bundlesModel));
    lifecycleEvent.onNext(View.LifecycleEvent.CREATE);
    //When pull to refresh is done
    pullToRefreshEvent.onNext(null);
    //Then a pull refresh analytics event should be sent
    verify(homeAnalytics).sendPullRefreshInteractEvent();
    //Then the progress indicator should be hidden
    verify(view).hideRefresh();
  }

  @Test public void retryClicked_LoadNextBundles() {
    //Given an initialised presenter with already loaded bundlesModel into the UI before
    presenter.handleRetryClick();
    when(home.loadNextHomeBundles()).thenReturn(Single.just(bundlesModel));
    lifecycleEvent.onNext(View.LifecycleEvent.CREATE);
    //When pull to refresh is done
    retryClickedEvent.onNext(null);
    //Then bundles should be shown
    verify(view).showMoreHomeBundles(bundlesModel.getList());
    //Then it should hide the load more indicator (if exists)
    verify(view).hideShowMore();
    //Then it should hide the loading indicator
    verify(view).hideLoading();
  }

  @Test public void loadLoggedInUserImageUserTest() {
    //When the user is logged in
    when(account.getAvatar()).thenReturn("A string");
    when(account.isLoggedIn()).thenReturn(true);
    //Given an initialised HomePresenter
    presenter.loadUserImage();
    lifecycleEvent.onNext(View.LifecycleEvent.CREATE);
    //And AccountManager returns an account
    accountStatusEvent.onNext(account);
    //Then it should show the image
    verify(view).setUserImage("A string");
    verify(view).showAvatar();
  }

  @Test public void loadNotLoggedInUserImageUserTest() {
    //When the user is logged in
    when(account.isLoggedIn()).thenReturn(false);
    //Given an initialised HomePresenter
    presenter.loadUserImage();
    lifecycleEvent.onNext(View.LifecycleEvent.CREATE);
    //And AccountManager returns an account
    accountStatusEvent.onNext(account);
    //Then it should show the image
    verify(view).showAvatar();
  }

  @Test public void handleUserImageClick() {
    //Given an initialised HomePresenter
    presenter.handleUserImageClick();
    lifecycleEvent.onNext(View.LifecycleEvent.CREATE);
    //When an user clicks the profile image
    imageClickEvent.onNext(null);
    //Then it should navigate to the Settings Fragment
    verify(homeNavigator).navigateToMyAccount();
  }

  @Test public void onBundleScrolledRight_SendScrollEvent() {
    //Given an initialised HomePresenter
    presenter.handleBundleScrolledRight();
    lifecycleEvent.onNext(View.LifecycleEvent.CREATE);
    //When an user scrolls a bundle with items to the right
    bundleScrolledEvent.onNext(new HomeEvent(localTopAppsBundle, 2, HomeEvent.Type.SCROLL_RIGHT));
    //Then a scroll right analytics event should be sent
    verify(homeAnalytics).sendScrollRightInteractEvent(2, localTopAppsBundle.getTag(),
        localTopAppsBundle.getContent()
            .size());
  }
}
