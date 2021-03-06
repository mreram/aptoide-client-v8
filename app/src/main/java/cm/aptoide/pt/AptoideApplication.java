/*
 * Copyright (c) 2016.
 * Modified on 02/09/2016.
 */

package cm.aptoide.pt;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.XmlResourceParser;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.multidex.MultiDex;
import cm.aptoide.accountmanager.AdultContent;
import cm.aptoide.accountmanager.AptoideAccountManager;
import cm.aptoide.analytics.AnalyticsManager;
import cm.aptoide.analytics.implementation.navigation.NavigationTracker;
import cm.aptoide.pt.account.AccountAnalytics;
import cm.aptoide.pt.account.AccountSettingsBodyInterceptorV7;
import cm.aptoide.pt.account.AdultContentAnalytics;
import cm.aptoide.pt.account.LoginPreferences;
import cm.aptoide.pt.ads.AdsRepository;
import cm.aptoide.pt.ads.MinimalAdMapper;
import cm.aptoide.pt.analytics.FirstLaunchAnalytics;
import cm.aptoide.pt.billing.Billing;
import cm.aptoide.pt.billing.BillingAnalytics;
import cm.aptoide.pt.billing.BillingIdManager;
import cm.aptoide.pt.billing.BillingPool;
import cm.aptoide.pt.billing.external.ExternalBillingSerializer;
import cm.aptoide.pt.billing.payment.Adyen;
import cm.aptoide.pt.billing.purchase.PurchaseFactory;
import cm.aptoide.pt.billing.view.PaymentThrowableCodeMapper;
import cm.aptoide.pt.billing.view.PurchaseBundleMapper;
import cm.aptoide.pt.crashreports.ConsoleLogger;
import cm.aptoide.pt.crashreports.CrashReport;
import cm.aptoide.pt.crashreports.CrashlyticsCrashLogger;
import cm.aptoide.pt.database.AccessorFactory;
import cm.aptoide.pt.database.accessors.Database;
import cm.aptoide.pt.database.accessors.InstalledAccessor;
import cm.aptoide.pt.database.realm.Installed;
import cm.aptoide.pt.database.realm.Notification;
import cm.aptoide.pt.database.realm.Store;
import cm.aptoide.pt.dataprovider.WebService;
import cm.aptoide.pt.dataprovider.cache.L2Cache;
import cm.aptoide.pt.dataprovider.interfaces.TokenInvalidator;
import cm.aptoide.pt.dataprovider.ws.BodyInterceptor;
import cm.aptoide.pt.dataprovider.ws.v2.aptwords.AdsApplicationVersionCodeProvider;
import cm.aptoide.pt.dataprovider.ws.v7.BaseBody;
import cm.aptoide.pt.dataprovider.ws.v7.BaseRequestWithStore;
import cm.aptoide.pt.dataprovider.ws.v7.PostReadRequest;
import cm.aptoide.pt.dataprovider.ws.v7.store.GetStoreMetaRequest;
import cm.aptoide.pt.deprecated.SQLiteDatabaseHelper;
import cm.aptoide.pt.downloadmanager.AptoideDownloadManager;
import cm.aptoide.pt.file.CacheHelper;
import cm.aptoide.pt.file.FileManager;
import cm.aptoide.pt.install.InstallAnalytics;
import cm.aptoide.pt.install.InstallFabricEvents;
import cm.aptoide.pt.install.InstallManager;
import cm.aptoide.pt.install.InstallerFactory;
import cm.aptoide.pt.install.PackageRepository;
import cm.aptoide.pt.install.installer.RootInstallationRetryHandler;
import cm.aptoide.pt.leak.LeakTool;
import cm.aptoide.pt.link.AptoideInstallParser;
import cm.aptoide.pt.logger.Logger;
import cm.aptoide.pt.navigator.Result;
import cm.aptoide.pt.networking.AuthenticationPersistence;
import cm.aptoide.pt.networking.IdsRepository;
import cm.aptoide.pt.notification.NotificationAnalytics;
import cm.aptoide.pt.notification.NotificationCenter;
import cm.aptoide.pt.notification.NotificationInfo;
import cm.aptoide.pt.notification.NotificationPolicyFactory;
import cm.aptoide.pt.notification.NotificationProvider;
import cm.aptoide.pt.notification.NotificationSyncScheduler;
import cm.aptoide.pt.notification.NotificationsCleaner;
import cm.aptoide.pt.notification.SystemNotificationShower;
import cm.aptoide.pt.preferences.PRNGFixes;
import cm.aptoide.pt.preferences.Preferences;
import cm.aptoide.pt.preferences.secure.SecurePreferences;
import cm.aptoide.pt.preferences.secure.SecurePreferencesImplementation;
import cm.aptoide.pt.preferences.toolbox.ToolboxManager;
import cm.aptoide.pt.presenter.View;
import cm.aptoide.pt.repository.RepositoryFactory;
import cm.aptoide.pt.root.RootAvailabilityManager;
import cm.aptoide.pt.search.suggestions.SearchSuggestionManager;
import cm.aptoide.pt.search.suggestions.TrendingManager;
import cm.aptoide.pt.social.data.ReadPostsPersistence;
import cm.aptoide.pt.store.StoreCredentialsProviderImpl;
import cm.aptoide.pt.store.StoreUtilsProxy;
import cm.aptoide.pt.sync.SyncScheduler;
import cm.aptoide.pt.sync.alarm.SyncStorage;
import cm.aptoide.pt.sync.rx.RxSyncScheduler;
import cm.aptoide.pt.timeline.TimelineAnalytics;
import cm.aptoide.pt.util.PreferencesXmlParser;
import cm.aptoide.pt.utils.AptoideUtils;
import cm.aptoide.pt.utils.FileUtils;
import cm.aptoide.pt.utils.SecurityUtils;
import cm.aptoide.pt.utils.q.QManager;
import cm.aptoide.pt.view.ActivityModule;
import cm.aptoide.pt.view.ActivityProvider;
import cm.aptoide.pt.view.BaseActivity;
import cm.aptoide.pt.view.BaseFragment;
import cm.aptoide.pt.view.FragmentModule;
import cm.aptoide.pt.view.FragmentProvider;
import cm.aptoide.pt.view.entry.EntryActivity;
import cm.aptoide.pt.view.entry.EntryPointChooser;
import cm.aptoide.pt.view.recycler.DisplayableWidgetMapping;
import cm.aptoide.pt.view.share.NotLoggedInShareAnalytics;
import com.crashlytics.android.Crashlytics;
import com.flurry.android.FlurryAgent;
import com.jakewharton.rxrelay.BehaviorRelay;
import com.jakewharton.rxrelay.PublishRelay;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import okhttp3.OkHttpClient;
import org.xmlpull.v1.XmlPullParserException;
import rx.Completable;
import rx.Observable;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import static cm.aptoide.pt.preferences.managed.ManagedKeys.CAMPAIGN_SOCIAL_NOTIFICATIONS_PREFERENCE_VIEW_KEY;

public abstract class AptoideApplication extends Application {

  static final String CACHE_FILE_NAME = "aptoide.wscache";
  private static final String TAG = AptoideApplication.class.getName();

  private static FragmentProvider fragmentProvider;
  private static ActivityProvider activityProvider;
  private static DisplayableWidgetMapping displayableWidgetMapping;
  private static boolean autoUpdateWasCalled = false;
  @Inject Database database;
  @Inject AptoideDownloadManager downloadManager;
  @Inject CacheHelper cacheHelper;
  @Inject AptoideAccountManager accountManager;
  @Inject Preferences preferences;
  @Inject @Named("secure") cm.aptoide.pt.preferences.SecurePreferences securePreferences;
  @Inject AdultContent adultContent;
  @Inject IdsRepository idsRepository;
  @Inject @Named("default") OkHttpClient defaultClient;
  @Inject RootAvailabilityManager rootAvailabilityManager;
  @Inject AuthenticationPersistence authenticationPersistence;
  @Inject AccountAnalytics accountAnalytics;
  @Inject Crashlytics crashlytics;
  @Inject @Named("default") SharedPreferences defaultSharedPreferences;
  @Inject SyncScheduler alarmSyncScheduler;
  @Inject @Named("pool-v7") BodyInterceptor<BaseBody> bodyInterceptorPoolV7;
  @Inject @Named("web-v7") BodyInterceptor<BaseBody> bodyInterceptorWebV7;
  @Inject @Named("defaultInterceptorV3") BodyInterceptor<cm.aptoide.pt.dataprovider.ws.v3.BaseBody>
      bodyInterceptorV3;
  @Inject L2Cache httpClientCache;
  @Inject QManager qManager;
  @Inject RootInstallationRetryHandler rootInstallationRetryHandler;
  @Inject TokenInvalidator tokenInvalidator;
  @Inject PackageRepository packageRepository;
  @Inject AdsApplicationVersionCodeProvider applicationVersionCodeProvider;
  @Inject AdsRepository adsRepository;
  @Inject SyncStorage syncStorage;
  @Inject NavigationTracker navigationTracker;
  @Inject @Named("account-settings-pool-v7") BodyInterceptor<BaseBody>
      accountSettingsBodyInterceptorPoolV7;
  @Inject TrendingManager trendingManager;
  @Inject AdultContentAnalytics adultContentAnalytics;
  @Inject NotificationAnalytics notificationAnalytics;
  @Inject SearchSuggestionManager searchSuggestionManager;
  @Inject AnalyticsManager analyticsManager;
  @Inject InstallAnalytics installAnalytics;
  @Inject FirstLaunchAnalytics firstLaunchAnalytics;
  @Inject InvalidRefreshTokenLogoutManager invalidRefreshTokenLogoutManager;
  private LeakTool leakTool;
  private String aptoideMd5sum;
  private BillingAnalytics billingAnalytics;
  private ExternalBillingSerializer inAppBillingSerialzer;
  private PurchaseBundleMapper purchaseBundleMapper;
  private PaymentThrowableCodeMapper paymentThrowableCodeMapper;
  private NotificationCenter notificationCenter;
  private EntryPointChooser entryPointChooser;
  private FileManager fileManager;
  private NotificationProvider notificationProvider;
  private BehaviorRelay<Map<Integer, Result>> fragmentResultRelay;
  private Map<Integer, Result> fragmentResulMap;
  private BillingPool billingPool;
  private NotLoggedInShareAnalytics notLoggedInShareAnalytics;
  private BodyInterceptor<BaseBody> accountSettingsBodyInterceptorWebV7;
  private Adyen adyen;
  private PurchaseFactory purchaseFactory;
  private InstallManager installManager;
  private ApplicationComponent applicationComponent;
  private ReadPostsPersistence readPostsPersistence;
  private PublishRelay<NotificationInfo> notificationsPublishRelay;
  private NotificationsCleaner notificationsCleaner;
  private TimelineAnalytics timelineAnalytics;

  public static FragmentProvider getFragmentProvider() {
    return fragmentProvider;
  }

  public static ActivityProvider getActivityProvider() {
    return activityProvider;
  }

  public static DisplayableWidgetMapping getDisplayableWidgetMapping() {
    return displayableWidgetMapping;
  }

  public static boolean isAutoUpdateWasCalled() {
    return autoUpdateWasCalled;
  }

  public static void setAutoUpdateWasCalled(boolean autoUpdateWasCalled) {
    AptoideApplication.autoUpdateWasCalled = autoUpdateWasCalled;
  }

  public LeakTool getLeakTool() {
    if (leakTool == null) {
      leakTool = new LeakTool();
    }
    return leakTool;
  }

  @Override public void onCreate() {

    getApplicationComponent().inject(this);

    CrashReport.getInstance()
        .addLogger(new CrashlyticsCrashLogger(crashlytics))
        .addLogger(new ConsoleLogger());
    Logger.setDBG(ToolboxManager.isDebug(getDefaultSharedPreferences()) || BuildConfig.DEBUG);

    try {
      PRNGFixes.apply();
    } catch (Exception e) {
      CrashReport.getInstance()
          .log(e);
    }

    //
    // call super
    //
    super.onCreate();

    //
    // execute custom Application onCreate code with time metric
    //

    long initialTimestamp = System.currentTimeMillis();

    getLeakTool().setup(this);

    //
    // hack to set the debug flag active in case of Debug
    //

    fragmentProvider = createFragmentProvider();
    activityProvider = createActivityProvider();
    displayableWidgetMapping = createDisplayableWidgetMapping();

    //
    // do not erase this code. it is useful to figure out when someone forgot to attach an error handler when subscribing and the app
    // is crashing in Rx without a proper stack trace
    //
    //if (BuildConfig.DEBUG) {
    //  RxJavaPlugins.getInstance().registerObservableExecutionHook(new RxJavaStackTracer());
    //}

    //
    // async app initialization
    // beware! this code could be executed at the same time the first activity is
    // visible
    //
    /**
     * There's not test at the moment
     * TODO change this class in order to accept that there's no test
     * AN-1838
     */
    checkAppSecurity().andThen(generateAptoideUuid())
        .observeOn(Schedulers.computation())
        .andThen(prepareApp(AptoideApplication.this.getAccountManager()).onErrorComplete(err -> {
          // in case we have an error preparing the app, log that error and continue
          CrashReport.getInstance()
              .log(err);
          return true;
        }))
        .andThen(discoverAndSaveInstalledApps())
        .subscribe(() -> { /* do nothing */}, error -> CrashReport.getInstance()
            .log(error));

    //
    // app synchronous initialization
    //

    sendAppStartToAnalytics().doOnCompleted(() -> SecurePreferences.setFirstRun(false,
        SecurePreferencesImplementation.getInstance(getApplicationContext(),
            getDefaultSharedPreferences())))
        .subscribe(() -> {
        }, throwable -> CrashReport.getInstance()
            .log(throwable));

    initializeFlurry(this, BuildConfig.FLURRY_KEY);

    clearFileCache();

    //
    // this will trigger the migration if needed
    //

    SQLiteDatabaseHelper dbHelper = new SQLiteDatabaseHelper(this);
    SQLiteDatabase db = dbHelper.getWritableDatabase();
    if (db.isOpen()) {
      db.close();
    }

    startNotificationCenter();
    startNotificationCleaner();
    getRootInstallationRetryHandler().start();
    AptoideApplicationAnalytics aptoideApplicationAnalytics = new AptoideApplicationAnalytics();
    aptoideApplicationAnalytics.setPackageDimension(getPackageName());
    accountManager.accountStatus()
        .map(account -> account.isLoggedIn())
        .distinctUntilChanged()
        .subscribe(isLoggedIn -> aptoideApplicationAnalytics.updateDimension(isLoggedIn));

    dispatchPostReadEventInterval().subscribe(() -> {
    }, throwable -> CrashReport.getInstance()
        .log(throwable));

    long totalExecutionTime = System.currentTimeMillis() - initialTimestamp;
    Logger.getInstance()
        .v(TAG, String.format("onCreate took %d millis.", totalExecutionTime));
    analyticsManager.setup();
    invalidRefreshTokenLogoutManager.start();
  }

  public ApplicationComponent getApplicationComponent() {
    if (applicationComponent == null) {
      applicationComponent = DaggerApplicationComponent.builder()
          .applicationModule(new ApplicationModule(this, getAptoideMd5sum()))
          .flavourApplicationModule(new FlavourApplicationModule(this))
          .build();
    }
    return applicationComponent;
  }

  /**
   * <p>Needs to be here, to be mocked for tests. Should be on BaseActivity if there were no
   * tests</p>
   *
   * @return Returns a new Activity Module for the Activity Component
   */
  public ActivityModule getActivityModule(BaseActivity activity, Intent intent,
      NotificationSyncScheduler notificationSyncScheduler, String marketName, String autoUpdateUrl,
      View view, String defaultThemeName, String defaultStoreName, boolean firstCreated,
      String fileProviderAuthority) {

    return new ActivityModule(activity, intent, notificationSyncScheduler, marketName,
        autoUpdateUrl, view, defaultThemeName, defaultStoreName, firstCreated,
        fileProviderAuthority);
  }

  /**
   * Needs to be here, to be mocked for tests. Should be on BaseFragment if there were no tests
   *
   * @return Returns a new Fragment Module for the Fragment Component
   */
  public FragmentModule getFragmentModule(BaseFragment baseFragment, Bundle savedInstanceState,
      Bundle arguments, boolean createStoreUserPrivacyEnabled, String packageName) {
    return new FragmentModule(baseFragment, savedInstanceState, arguments,
        createStoreUserPrivacyEnabled, packageName);
  }

  @Override protected void attachBaseContext(Context base) {
    super.attachBaseContext(base);
    MultiDex.install(this);
  }

  public TokenInvalidator getTokenInvalidator() {
    return tokenInvalidator;
  }

  private void startNotificationCenter() {
    getPreferences().getBoolean(CAMPAIGN_SOCIAL_NOTIFICATIONS_PREFERENCE_VIEW_KEY, true)
        .first()
        .subscribe(enabled -> getNotificationSyncScheduler().setEnabled(enabled),
            throwable -> CrashReport.getInstance()
                .log(throwable));

    getNotificationCenter().setup();
  }

  private void startNotificationCleaner() {
    getNotificationCleaner().setup();
  }

  private NotificationsCleaner getNotificationCleaner() {
    if (notificationsCleaner == null) {
      notificationsCleaner = new NotificationsCleaner(AccessorFactory.getAccessorFor(
          ((AptoideApplication) this.getApplicationContext()).getDatabase(), Notification.class),
          Calendar.getInstance(TimeZone.getTimeZone("UTC")), getAccountManager(),
          getNotificationProvider(), CrashReport.getInstance());
    }
    return notificationsCleaner;
  }

  public abstract String getCachePath();

  public abstract boolean hasMultiStoreSearch();

  public abstract String getDefaultStoreName();

  public abstract String getMarketName();

  public abstract String getFeedbackEmail();

  public abstract String getImageCachePath();

  public abstract String getAccountType();

  public abstract String getAutoUpdateUrl();

  public abstract String getPartnerId();

  public abstract String getExtraId();

  public abstract String getDefaultThemeName();

  public abstract boolean isCreateStoreUserPrivacyEnabled();

  public RootInstallationRetryHandler getRootInstallationRetryHandler() {
    return rootInstallationRetryHandler;
  }

  @NonNull protected abstract SystemNotificationShower getSystemNotificationShower();

  public PublishRelay<NotificationInfo> getNotificationsPublishRelay() {
    if (notificationsPublishRelay == null) {
      notificationsPublishRelay = PublishRelay.create();
    }
    return notificationsPublishRelay;
  }

  public NotificationCenter getNotificationCenter() {
    if (notificationCenter == null) {
      final NotificationProvider notificationProvider = getNotificationProvider();
      notificationCenter =
          new NotificationCenter(notificationProvider, getNotificationSyncScheduler(),
              new NotificationPolicyFactory(notificationProvider),
              new NotificationAnalytics(new AptoideInstallParser(), analyticsManager,
                  navigationTracker));
    }
    return notificationCenter;
  }

  public NotificationProvider getNotificationProvider() {
    if (notificationProvider == null) {
      notificationProvider = new NotificationProvider(AccessorFactory.getAccessorFor(
          ((AptoideApplication) this.getApplicationContext()).getDatabase(), Notification.class),
          Schedulers.io());
    }
    return notificationProvider;
  }

  public abstract NotificationSyncScheduler getNotificationSyncScheduler();

  public SharedPreferences getDefaultSharedPreferences() {
    return PreferenceManager.getDefaultSharedPreferences(this);
  }

  public OkHttpClient getDefaultClient() {
    return defaultClient;
  }

  public L2Cache getHttpClientCache() {
    return httpClientCache;
  }

  public AptoideDownloadManager getDownloadManager() {
    return downloadManager;
  }

  public InstallManager getInstallManager() {

    if (installManager == null) {

      installManager = new InstallManager(getApplicationContext(), getDownloadManager(),
          new InstallerFactory(new MinimalAdMapper(),
              new InstallFabricEvents(analyticsManager, installAnalytics,
                  getDefaultSharedPreferences(), rootAvailabilityManager)).create(this),
          getRootAvailabilityManager(), getDefaultSharedPreferences(),
          SecurePreferencesImplementation.getInstance(getApplicationContext(),
              getDefaultSharedPreferences()),
          RepositoryFactory.getDownloadRepository(getApplicationContext().getApplicationContext()),
          RepositoryFactory.getInstalledRepository(
              getApplicationContext().getApplicationContext()));
    }

    return installManager;
  }

  public QManager getQManager() {
    return qManager;
  }

  public EntryPointChooser getEntryPointChooser() {
    if (entryPointChooser == null) {
      entryPointChooser = new EntryPointChooser(() -> getQManager().isSupportedExtensionsDefined());
    }
    return entryPointChooser;
  }

  public AptoideAccountManager getAccountManager() {
    return accountManager;
  }

  public AuthenticationPersistence getAuthenticationPersistence() {
    return authenticationPersistence;
  }

  public Preferences getPreferences() {
    return preferences;
  }

  public cm.aptoide.pt.preferences.SecurePreferences getSecurePreferences() {
    return securePreferences;
  }

  public BillingAnalytics getBillingAnalytics() {
    if (billingAnalytics == null) {
      billingAnalytics =
          new BillingAnalytics(getAptoidePackage(), analyticsManager, navigationTracker);
    }
    return billingAnalytics;
  }

  public Billing getBilling(String merchantName) {
    return getBillingPool().get(merchantName);
  }

  public BillingPool getBillingPool() {
    if (billingPool == null) {
      billingPool =
          new BillingPool(getDefaultSharedPreferences(), getBodyInterceptorV3(), getDefaultClient(),
              getAccountManager(), getDatabase(), getResources(), getPackageRepository(),
              getTokenInvalidator(),
              new RxSyncScheduler(new HashMap<>(), CrashReport.getInstance()),
              getInAppBillingSerializer(), getBodyInterceptorPoolV7(),
              getAccountSettingsBodyInterceptorPoolV7(), new HashMap<>(),
              WebService.getDefaultConverter(), CrashReport.getInstance(), getAdyen(),
              getPurchaseFactory(), Build.VERSION_CODES.JELLY_BEAN, Build.VERSION_CODES.JELLY_BEAN,
              getAuthenticationPersistence(), getPreferences());
    }
    return billingPool;
  }

  public Adyen getAdyen() {
    if (adyen == null) {
      adyen = new Adyen(this, Charset.forName("UTF-8"), Schedulers.io(), PublishRelay.create());
    }
    return adyen;
  }

  public BillingIdManager getIdResolver(String merchantName) {
    return getBillingPool().getIdResolver(merchantName);
  }

  public Database getDatabase() {
    return database;
  }

  public PackageRepository getPackageRepository() {
    return packageRepository;
  }

  public PaymentThrowableCodeMapper getPaymentThrowableCodeMapper() {
    if (paymentThrowableCodeMapper == null) {
      paymentThrowableCodeMapper = new PaymentThrowableCodeMapper();
    }
    return paymentThrowableCodeMapper;
  }

  public PurchaseBundleMapper getPurchaseBundleMapper() {
    if (purchaseBundleMapper == null) {
      purchaseBundleMapper =
          new PurchaseBundleMapper(getPaymentThrowableCodeMapper(), getPurchaseFactory());
    }
    return purchaseBundleMapper;
  }

  public ExternalBillingSerializer getInAppBillingSerializer() {
    if (inAppBillingSerialzer == null) {
      inAppBillingSerialzer = new ExternalBillingSerializer();
    }
    return inAppBillingSerialzer;
  }

  private void clearFileCache() {
    getFileManager().purgeCache()
        .first()
        .toSingle()
        .subscribe(cleanedSize -> Logger.getInstance()
                .d(TAG, "cleaned size: " + AptoideUtils.StringU.formatBytes(cleanedSize, false)),
            err -> CrashReport.getInstance()
                .log(err));
  }

  public FileManager getFileManager() {
    if (fileManager == null) {
      fileManager = new FileManager(getCacheHelper(), new FileUtils(), new String[] {
          getApplicationContext().getCacheDir().getPath(), getCachePath()
      }, getDownloadManager(), getHttpClientCache());
    }
    return fileManager;
  }

  private CacheHelper getCacheHelper() {
    return cacheHelper;
  }

  private void initializeFlurry(Context context, String flurryKey) {
    new FlurryAgent.Builder().withLogEnabled(false)
        .build(context, flurryKey);
  }

  private Completable sendAppStartToAnalytics() {
    return firstLaunchAnalytics.sendAppStart(this, defaultSharedPreferences,
        WebService.getDefaultConverter(), getDefaultClient(),
        getAccountSettingsBodyInterceptorPoolV7(), getTokenInvalidator());
  }

  private Completable checkAppSecurity() {
    return Completable.fromAction(() -> {
      if (SecurityUtils.checkAppSignature(this) != SecurityUtils.VALID_APP_SIGNATURE) {
        Logger.getInstance()
            .w(TAG, "app signature is not valid!");
      }

      if (SecurityUtils.checkEmulator()) {
        Logger.getInstance()
            .w(TAG, "application is running on an emulator");
      }

      if (SecurityUtils.checkDebuggable(this)) {
        Logger.getInstance()
            .w(TAG, "application has debug flag active");
      }
    });
  }

  protected DisplayableWidgetMapping createDisplayableWidgetMapping() {
    return DisplayableWidgetMapping.getInstance();
  }

  private Completable generateAptoideUuid() {
    return Completable.fromAction(() -> getIdsRepository().getUniqueIdentifier())
        .subscribeOn(Schedulers.newThread());
  }

  private Completable prepareApp(AptoideAccountManager accountManager) {
    return accountManager.accountStatus()
        .first()
        .toSingle()
        .flatMapCompletable(account -> {
          if (SecurePreferences.isFirstRun(
              SecurePreferencesImplementation.getInstance(getApplicationContext(),
                  getDefaultSharedPreferences()))) {

            setSharedPreferencesValues();

            return setupFirstRun().andThen(getRootAvailabilityManager().updateRootAvailability())
                .andThen(Completable.merge(accountManager.updateAccount(), createShortcut()));
          }

          return Completable.complete();
        });
  }

  private void setSharedPreferencesValues() {
    PreferencesXmlParser preferencesXmlParser = new PreferencesXmlParser();

    XmlResourceParser parser = getResources().getXml(R.xml.settings);
    try {
      List<String[]> parsedPrefsList = preferencesXmlParser.parse(parser);
      for (String[] keyValue : parsedPrefsList) {
        getDefaultSharedPreferences().edit()
            .putBoolean(keyValue[0], Boolean.valueOf(keyValue[1]))
            .apply();
      }
    } catch (IOException | XmlPullParserException e) {
      e.printStackTrace();
    }
  }

  // todo re-factor all this code to proper Rx
  private Completable setupFirstRun() {
    return Completable.defer(() -> {

      final StoreCredentialsProviderImpl storeCredentials = new StoreCredentialsProviderImpl(
          AccessorFactory.getAccessorFor(
              ((AptoideApplication) this.getApplicationContext()).getDatabase(), Store.class));

      StoreUtilsProxy proxy =
          new StoreUtilsProxy(getAccountManager(), getAccountSettingsBodyInterceptorPoolV7(),
              storeCredentials, AccessorFactory.getAccessorFor(
              ((AptoideApplication) this.getApplicationContext()).getDatabase(), Store.class),
              getDefaultClient(), WebService.getDefaultConverter(), getTokenInvalidator(),
              getDefaultSharedPreferences());

      BaseRequestWithStore.StoreCredentials defaultStoreCredentials =
          storeCredentials.get(getDefaultStoreName());

      return generateAptoideUuid().andThen(proxy.addDefaultStore(
          GetStoreMetaRequest.of(defaultStoreCredentials, getAccountSettingsBodyInterceptorPoolV7(),
              getDefaultClient(), WebService.getDefaultConverter(), getTokenInvalidator(),
              getDefaultSharedPreferences()), getAccountManager(), defaultStoreCredentials)
          .andThen(refreshUpdates()))
          .doOnError(err -> CrashReport.getInstance()
              .log(err));
    });
  }

  public abstract String getDefaultStore();

  public abstract String getMarketName();

  /**
   * BaseBodyInterceptor for v7 ws calls with CDN = pool configuration
   */
  public BodyInterceptor<BaseBody> getBodyInterceptorPoolV7() {
    return bodyInterceptorPoolV7;
  }

  /**
   * BaseBodyInterceptor for v7 ws calls with CDN = web configuration
   */
  public BodyInterceptor<BaseBody> getBodyInterceptorWebV7() {
    return bodyInterceptorWebV7;
  }

  public BodyInterceptor<BaseBody> getAccountSettingsBodyInterceptorPoolV7() {
    return accountSettingsBodyInterceptorPoolV7;
  }

  public BodyInterceptor<BaseBody> getAccountSettingsBodyInterceptorWebV7() {
    if (accountSettingsBodyInterceptorWebV7 == null) {
      accountSettingsBodyInterceptorWebV7 =
          new AccountSettingsBodyInterceptorV7(getBodyInterceptorWebV7(), getAdultContent());
    }
    return accountSettingsBodyInterceptorWebV7;
  }

  public BodyInterceptor<cm.aptoide.pt.dataprovider.ws.v3.BaseBody> getBodyInterceptorV3() {
    return bodyInterceptorV3;
  }

  public String getAptoideMd5sum() {
    if (aptoideMd5sum == null) {
      synchronized (this) {
        if (aptoideMd5sum == null) {
          aptoideMd5sum = calculateMd5Sum();
        }
      }
    }
    return aptoideMd5sum;
  }

  private String calculateMd5Sum() {
    try {
      return AptoideUtils.AlgorithmU.computeMd5(
          getPackageManager().getPackageInfo(getAptoidePackage(), 0));
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }
    return null;
  }

  protected String getAptoidePackage() {
    return BuildConfig.APPLICATION_ID;
  }

  public AdultContent getAdultContent() {
    return adultContent;
  }

  public Completable createShortcut() {
    return Completable.defer(() -> {
      createAppShortcut();
      return null;
    });
  }

  private Completable discoverAndSaveInstalledApps() {
    InstalledAccessor installedAccessor = AccessorFactory.getAccessorFor(
        ((AptoideApplication) this.getApplicationContext()).getDatabase(), Installed.class);
    return Observable.fromCallable(() -> {
      // remove the current installed apps
      //AccessorFactory.getAccessorFor(Installed.class).removeAll();

      // get the installed apps
      List<PackageInfo> installedApps =
          AptoideUtils.SystemU.getAllInstalledApps(getPackageManager());
      Logger.getInstance()
          .v(TAG, "Found " + installedApps.size() + " user installed apps.");

      // Installed apps are inserted in database based on their firstInstallTime. Older comes first.
      Collections.sort(installedApps,
          (lhs, rhs) -> (int) ((lhs.firstInstallTime - rhs.firstInstallTime) / 1000));

      // return sorted installed apps
      return installedApps;
    })  // transform installation package into Installed table entry and save all the data
        .flatMapIterable(list -> list)
        .map(packageInfo -> new Installed(packageInfo, getPackageManager()))
        .toList()
        .flatMap(appsInstalled -> installedAccessor.getAll()
            .first()
            .map(installedFromDatabase -> combineLists(appsInstalled, installedFromDatabase,
                installed -> installed.setStatus(Installed.STATUS_UNINSTALLED))))
        .doOnNext(list -> {
          installedAccessor.removeAll();
          installedAccessor.insertAll(list);
        })
        .toCompletable();
  }

  public <T> List<T> combineLists(List<T> list1, List<T> list2, @Nullable Action1<T> transformer) {
    List<T> toReturn = new ArrayList<>(list1.size() + list2.size());
    toReturn.addAll(list1);
    for (T item : list2) {
      if (!toReturn.contains(item)) {
        if (transformer != null) {
          transformer.call(item);
        }
        toReturn.add(item);
      }
    }

    return toReturn;
  }

  private Completable refreshUpdates() {
    return RepositoryFactory.getUpdateRepository(this, getDefaultSharedPreferences())
        .sync(true, false);
  }

  private void createAppShortcut() {
    Intent shortcutIntent = new Intent(this, EntryActivity.class);
    shortcutIntent.setAction(Intent.ACTION_MAIN);
    Intent intent = new Intent();
    intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
    intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getResources().getString(R.string.app_name));
    intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
        Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.mipmap.ic_launcher));
    intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
    getApplicationContext().sendBroadcast(intent);
  }

  public RootAvailabilityManager getRootAvailabilityManager() {
    return rootAvailabilityManager;
  }

  public AdsApplicationVersionCodeProvider getVersionCodeProvider() {
    return applicationVersionCodeProvider;
  }

  public AdsRepository getAdsRepository() {
    return adsRepository;
  }

  public SyncStorage getSyncStorage() {
    return syncStorage;
  }

  public BehaviorRelay<Map<Integer, Result>> getFragmentResultRelay() {
    if (fragmentResultRelay == null) {
      fragmentResultRelay = BehaviorRelay.create();
    }
    return fragmentResultRelay;
  }

  @SuppressLint("UseSparseArrays") public Map<Integer, Result> getFragmentResulMap() {
    if (fragmentResulMap == null) {
      fragmentResulMap = new HashMap<>();
    }
    return fragmentResulMap;
  }

  public NavigationTracker getNavigationTracker() {
    return navigationTracker;
  }

  public abstract LoginPreferences getLoginPreferences();

  public abstract FragmentProvider createFragmentProvider();

  public abstract ActivityProvider createActivityProvider();

  public NotLoggedInShareAnalytics getNotLoggedInShareAnalytics() {
    if (notLoggedInShareAnalytics == null) {
      notLoggedInShareAnalytics =
          new NotLoggedInShareAnalytics(analyticsManager, navigationTracker, getAccountAnalytics());
    }
    return notLoggedInShareAnalytics;
  }

  public AccountAnalytics getAccountAnalytics() {
    return accountAnalytics;
  }

  public PurchaseFactory getPurchaseFactory() {
    if (purchaseFactory == null) {
      purchaseFactory = new PurchaseFactory();
    }
    return purchaseFactory;
  }

  public ReadPostsPersistence getReadPostsPersistence() {
    if (readPostsPersistence == null) {
      readPostsPersistence = new ReadPostsPersistence(new ArrayList<>());
    }
    return readPostsPersistence;
  }

  private Completable dispatchPostReadEventInterval() {
    return Observable.interval(10, TimeUnit.SECONDS)
        .switchMap(__ -> getReadPostsPersistence().getPosts(10)
            .toObservable()
            .filter(postReads -> !postReads.isEmpty())
            .flatMap(postsRead -> PostReadRequest.of(postsRead, getBodyInterceptorPoolV7(),
                getDefaultClient(), WebService.getDefaultConverter(), getTokenInvalidator(),
                getDefaultSharedPreferences())
                .observe()
                .flatMapCompletable(___ -> getReadPostsPersistence().removePosts(postsRead)))
            .repeatWhen(completed -> completed.takeWhile(
                ____ -> !getReadPostsPersistence().isPostsEmpty())))
        .toCompletable();
  }

  public SyncScheduler getAlarmSyncScheduler() {
    return alarmSyncScheduler;
  }

  public TrendingManager getTrendingManager() {
    return trendingManager;
  }

  public NotificationAnalytics getNotificationAnalytics() {
    return notificationAnalytics;
  }

  public IdsRepository getIdsRepository() {
    return idsRepository;
  }

  public SearchSuggestionManager getSearchSuggestionManager() {
    return searchSuggestionManager;
  }

  public TimelineAnalytics getTimelineAnalytics() {
    if (timelineAnalytics == null) {
      timelineAnalytics = new TimelineAnalytics(getNavigationTracker(), analyticsManager);
    }
    return timelineAnalytics;
  }

  public AnalyticsManager getAnalyticsManager() {
    return analyticsManager;
  }

  public AdultContentAnalytics getAdultContentAnalytics() {
    return adultContentAnalytics;
  }
}

