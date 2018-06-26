package cm.aptoide.pt.home;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import rx.Single;

/**
 * Created by jdandrade on 08/03/2018.
 */

public class BundlesRepository {
  private static final String HOME_BUNDLE_KEY = "Home_Bundle";
  private final BundleDataSource remoteBundleDataSource;
  private Map<String, List<HomeBundle>> cachedBundles;
  private Map<String, Integer> offset;
  private int limit;

  public BundlesRepository(BundleDataSource remoteBundleDataSource,
      Map<String, List<HomeBundle>> cachedBundles, Map<String, Integer> offset, int limit) {
    this.remoteBundleDataSource = remoteBundleDataSource;
    this.cachedBundles = cachedBundles;
    this.offset = offset;
    this.limit = limit;
  }

  public Single<HomeBundlesModel> loadHomeBundles() {
    if (!cachedBundles.containsKey(HOME_BUNDLE_KEY)) {
      return loadNextHomeBundles();
    } else {
      return Single.just(new HomeBundlesModel(
          cachedBundles.put(HOME_BUNDLE_KEY, new ArrayList<>(cachedBundles.get(HOME_BUNDLE_KEY))),
          false, getOffset(HOME_BUNDLE_KEY)));
    }
  }

  public Single<HomeBundlesModel> loadFreshHomeBundles() {
    return remoteBundleDataSource.loadFreshHomeBundles(HOME_BUNDLE_KEY)
        .doOnSuccess(homeBundlesModel -> updateCache(homeBundlesModel, true, HOME_BUNDLE_KEY))
        .map(this::cloneList);
  }

  private HomeBundlesModel cloneList(HomeBundlesModel homeBundlesModel) {
    if (homeBundlesModel.hasErrors() || homeBundlesModel.isLoading()) {
      return homeBundlesModel;
    }
    return new HomeBundlesModel(new ArrayList<>(homeBundlesModel.getList()),
        homeBundlesModel.isLoading(), homeBundlesModel.getOffset());
  }

  public Single<HomeBundlesModel> loadNextHomeBundles() {
    return remoteBundleDataSource.loadNextHomeBundles(getOffset(HOME_BUNDLE_KEY), limit,
        HOME_BUNDLE_KEY)
        .doOnSuccess(homeBundlesModel -> updateCache(homeBundlesModel, false, HOME_BUNDLE_KEY))
        .map(this::cloneList);
  }

  private void updateCache(HomeBundlesModel homeBundles, boolean cacheIsDirty, String bundleKey) {
    if (!homeBundles.hasErrors() && !homeBundles.isLoading()) {
      offset.put(bundleKey, homeBundles.getOffset());
      if (cacheIsDirty || !cachedBundles.containsKey(bundleKey)) {
        cachedBundles.put(bundleKey, new ArrayList<>(homeBundles.getList()));
      } else {
        List<HomeBundle> homeBundleList = cachedBundles.get(bundleKey);
        homeBundleList.addAll(homeBundles.getList());
        cachedBundles.put(bundleKey, homeBundleList);
      }
    }
  }

  public boolean hasMore() {
    return remoteBundleDataSource.hasMore(getOffset(HOME_BUNDLE_KEY), HOME_BUNDLE_KEY);
  }

  public boolean hasMore(String title) {
    return remoteBundleDataSource.hasMore(getOffset(title), title);
  }

  public Single<HomeBundlesModel> loadBundles(String title, String url) {
    if (!cachedBundles.containsKey(title)) {
      return loadNextBundles(title, url);
    } else {
      return Single.just(
          new HomeBundlesModel(cachedBundles.put(title, new ArrayList<>(cachedBundles.get(title))),
              false, getOffset(title)));
    }
  }

  public Single<HomeBundlesModel> loadNextBundles(String title, String url) {
    return remoteBundleDataSource.loadNextBundleForEvent(url, getOffset(title), title, limit)
        .doOnSuccess(homeBundlesModel -> updateCache(homeBundlesModel, false, title))
        .map(this::cloneList);
  }

  public Single<HomeBundlesModel> loadFreshBundles(String title, String url) {
    return remoteBundleDataSource.loadFreshBundleForEvent(url, title)
        .doOnSuccess(homeBundlesModel -> updateCache(homeBundlesModel, true, title))
        .map(this::cloneList);
  }

  private int getOffset(String bundleKey) {
    if (!offset.containsKey(bundleKey)) {
      return 0;
    } else {
      return offset.get(bundleKey);
    }
  }
}
