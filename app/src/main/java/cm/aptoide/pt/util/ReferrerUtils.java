/*
 * Copyright (c) 2016.
 * Modified on 02/09/2016.
 */

package cm.aptoide.pt.util;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import cm.aptoide.pt.AptoideApplication;
import cm.aptoide.pt.ads.AdsRepository;
import cm.aptoide.pt.ads.MinimalAdMapper;
import cm.aptoide.pt.crashreports.CrashReport;
import cm.aptoide.pt.database.AccessorFactory;
import cm.aptoide.pt.database.accessors.StoredMinimalAdAccessor;
import cm.aptoide.pt.database.realm.StoredMinimalAd;
import cm.aptoide.pt.dataprovider.ads.AdNetworkUtils;
import cm.aptoide.pt.dataprovider.util.referrer.SimpleTimedFuture;
import cm.aptoide.pt.dataprovider.ws.v2.aptwords.RegisterAdRefererRequest;
import cm.aptoide.pt.logger.Logger;
import cm.aptoide.pt.networking.IdsRepository;
import cm.aptoide.pt.preferences.managed.ManagerPreferences;
import cm.aptoide.pt.search.model.SearchAdResult;
import cm.aptoide.pt.utils.AptoideUtils;
import cm.aptoide.pt.utils.q.QManager;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Converter;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by neuro on 20-06-2016.
 */
public class ReferrerUtils extends cm.aptoide.pt.dataprovider.util.referrer.ReferrerUtils {

  private static final String TAG = ReferrerUtils.class.getSimpleName();

  public static void extractReferrer(SearchAdResult searchAdResult, final int retries,
      boolean broadcastReferrer, AdsRepository adsRepository, final OkHttpClient httpClient,
      final Converter.Factory converterFactory, final QManager qManager, Context context,
      final SharedPreferences sharedPreferences, MinimalAdMapper adMapper) {
    String packageName = searchAdResult.getPackageName();
    long networkId = searchAdResult.getNetworkId();
    String clickUrl = searchAdResult.getClickUrl();

    if (clickUrl == null) {
      Logger.getInstance().d("ExtractReferrer", "No click_url for packageName " + packageName);
      return;
    }

    if (!AptoideUtils.ThreadU.isUiThread()) {
      throw new RuntimeException("ExtractReferrer must be run on UI thread!");
    }

    try {
      Logger.getInstance().d("ExtractReferrer", "Called for: " + clickUrl + " with packageName " + packageName);

      final String[] internalClickUrl = { clickUrl };
      final SimpleTimedFuture<String> clickUrlFuture = new SimpleTimedFuture<>();

      WindowManager windowManager =
          (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
      WindowManager.LayoutParams params;
      params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
          WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
          WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, PixelFormat.TRANSLUCENT);

      params.gravity = Gravity.TOP | Gravity.LEFT;
      params.x = 0;
      params.y = 0;
      params.width = 0;
      params.height = 0;

      LinearLayout view = new LinearLayout(context);
      view.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
          RelativeLayout.LayoutParams.MATCH_PARENT));

      AptoideUtils.ThreadU.runOnIoThread(() -> {
        final IdsRepository idsRepository =
            ((AptoideApplication) context.getApplicationContext()).getIdsRepository();
        internalClickUrl[0] = AdNetworkUtils.parseMacros(clickUrl, idsRepository.getAndroidId(),
            idsRepository.getUniqueIdentifier(), idsRepository.getAdvertisingId());
        clickUrlFuture.set(internalClickUrl[0]);
        Logger.getInstance().d("ExtractReferrer", "Parsed clickUrl: " + internalClickUrl[0]);
      });
      clickUrlFuture.get();
      WebView wv = new WebView(context);
      wv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
          LinearLayout.LayoutParams.MATCH_PARENT));
      view.addView(wv);
      wv.getSettings()
          .setJavaScriptEnabled(true);
      wv.setWebViewClient(new WebViewClient() {

        Future<Void> future;

        @Override public boolean shouldOverrideUrlLoading(WebView view, String clickUrl) {

          Logger.getInstance().d("ExtractReferrer", "ClickUrl redirect: " + clickUrl);

          if (clickUrl.startsWith("market://")
              || clickUrl.startsWith("https://play.google.com")
              || clickUrl.startsWith("http://play.google.com")) {
            Logger.getInstance().d("ExtractReferrer", "Clickurl landed on market");
            final String referrer = getReferrer(clickUrl);
            if (!TextUtils.isEmpty(referrer)) {
              Logger.getInstance().d("ExtractReferrer", "Referrer successfully extracted");

              if (broadcastReferrer) {
                broadcastReferrer(packageName, referrer, context);
              } else {
                //@Cleanup Realm realm = DeprecatedDatabase.get();
                //DeprecatedDatabase.save(
                //    new StoredMinimalAd(packageName, referrer, searchAdResult.getCpiUrl(),
                //        searchAdResult.getAdId()), realm);

                StoredMinimalAdAccessor storedMinimalAdAccessor = AccessorFactory.getAccessorFor(
                    ((AptoideApplication) context.getApplicationContext()
                        .getApplicationContext()).getDatabase(), StoredMinimalAd.class);
                storedMinimalAdAccessor.insert(adMapper.map(searchAdResult, referrer));
              }

              future.cancel(false);
              postponeReferrerExtraction(searchAdResult, 0, true, httpClient, converterFactory,
                  qManager);
            }
          }

          return false;
        }

        @Override public void onPageStarted(WebView view, String url, Bitmap favicon) {
          super.onPageStarted(view, url, favicon);

          Logger.getInstance().d("ExtractReferrer", "Openened clickUrl: " + url);

          if (future == null) {
            future = postponeReferrerExtraction(searchAdResult, TIME_OUT, retries, httpClient,
                converterFactory, qManager);
          }
        }

        private ScheduledFuture<Void> postponeReferrerExtraction(SearchAdResult searchAdResult,
            int delta, int retries, OkHttpClient httpClient, Converter.Factory converterFactory,
            QManager qManager) {
          return postponeReferrerExtraction(searchAdResult, delta, false, retries, httpClient,
              converterFactory,
              qManager.getFilters(ManagerPreferences.getHWSpecsFilter(sharedPreferences)),
              qManager);
        }

        private ScheduledFuture<Void> postponeReferrerExtraction(SearchAdResult searchAdResult,
            int delta, boolean success, OkHttpClient httpClient, Converter.Factory converterFactory,
            QManager qManager) {
          return postponeReferrerExtraction(searchAdResult, delta, success, 0, httpClient,
              converterFactory,
              qManager.getFilters(ManagerPreferences.getHWSpecsFilter(sharedPreferences)),
              qManager);
        }

        private ScheduledFuture<Void> postponeReferrerExtraction(SearchAdResult searchAdResult,
            int delta, final boolean success, final int retries, OkHttpClient httpClient,
            Converter.Factory converterFactory, String q, QManager qManager) {
          Logger.getInstance().d("ExtractReferrer", "Referrer postponed " + delta + " seconds.");

          Callable<Void> callable = () -> {
            Logger.getInstance().d("ExtractReferrer", "Sending RegisterAdRefererRequest with value " + success);

            RegisterAdRefererRequest.of(searchAdResult.getAdId(), searchAdResult.getAppId(),
                searchAdResult.getClickUrl(), success, httpClient, converterFactory,
                qManager.getFilters(ManagerPreferences.getHWSpecsFilter(sharedPreferences)),
                sharedPreferences)
                .execute();

            Logger.getInstance().d("ExtractReferrer", "Retries left: " + retries);

            if (!success) {
              excludedNetworks.add(packageName, networkId);

              try {

                if (retries > 0) {
                  adsRepository.getAdsFromSecondTry(packageName)
                      .observeOn(AndroidSchedulers.mainThread())
                      .onErrorReturn(throwable -> null)
                      .filter(minimalAd1 -> searchAdResult != null)
                      .subscribe(
                          minimalAd1 -> extractReferrer(new SearchAdResult(minimalAd1), retries - 1,
                              broadcastReferrer, adsRepository, httpClient, converterFactory,
                              qManager, context, sharedPreferences, new MinimalAdMapper()),
                          throwable -> clearExcludedNetworks(packageName));
                } else {
                  // A lista de excluded networks deve ser limpa a cada "ronda"
                  clearExcludedNetworks(packageName);
                }
              } catch (Exception e) {
                e.printStackTrace();
              }
              // TODO: 28-07-2016 Baikova Failed to extract referrer.
            } else {
              // A lista de excluded networks deve ser limpa a cada "ronda"
              // TODO: 28-07-2016 Baikova referrer successfully extracted.
              clearExcludedNetworks(packageName);
            }

            return null;
          };

          return executorService.schedule(callable, delta, TimeUnit.SECONDS);
        }
      });

      wv.loadUrl(internalClickUrl[0]);

      // TODO: 28-07-2016 Baikova Opened click_url

      windowManager.addView(view, params);
    } catch (Exception e) {
      // TODO: 09-06-2016 neuro
      CrashReport.getInstance()
          .log(e);
    }
  }

  private static List<Long> clearExcludedNetworks(String packageName) {
    return excludedNetworks.remove(packageName);
  }

  private static String getReferrer(String uriAsString) {
    Uri uri = Uri.parse(uriAsString);
    String referrer = uri.getQueryParameter("referrer");
    if (!TextUtils.isEmpty(referrer)) {
      Logger.getInstance().v(TAG, "Found referrer: " + referrer);
    } else {
      Logger.getInstance().v(TAG, "Didn't find any referrer: " + uriAsString);
    }
    return referrer;
  }

  public static void broadcastReferrer(String packageName, String referrer, Context context) {
    Intent i = new Intent("com.android.vending.INSTALL_REFERRER");
    i.setPackage(packageName);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
      i.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
    }
    i.putExtra("referrer", referrer);
    context.sendBroadcast(i);
    Logger.getInstance().d(TAG, "Sent broadcast to " + packageName + " with referrer " + referrer);
    // TODO: 28-07-2016 Baikova referrer broadcasted.
  }
}
