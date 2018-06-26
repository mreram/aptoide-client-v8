package cm.aptoide.pt.view;

import android.support.v4.app.Fragment;
import cm.aptoide.pt.addressbook.data.Contact;
import cm.aptoide.pt.app.view.NewAppViewFragment;
import cm.aptoide.pt.dataprovider.model.v7.Event;
import cm.aptoide.pt.dataprovider.util.CommentType;
import cm.aptoide.pt.dataprovider.ws.v7.store.StoreContext;
import cm.aptoide.pt.presenter.InviteFriendsContract;
import cm.aptoide.pt.search.model.SearchAdResult;
import cm.aptoide.pt.store.view.StoreFragment;
import java.util.List;

/**
 * Interface from which all fragments should be requested.
 *
 * @deprecated use specific navigator for each presenter/lifecycle manager instead. Inside those
 * navigators instantiate the proper fragment or activity.
 */
@Deprecated public interface FragmentProvider {

  Fragment newSendFeedbackFragment(String screenshotFilePath);

  @Deprecated Fragment newSendFeedbackFragment(String screenshotFilePath, String postId);

  @Deprecated Fragment newStoreFragment(String storeName, String storeTheme);

  Fragment newStoreFragment(String storeName, String storeTheme, StoreFragment.OpenType openType);

  Fragment newStoreFragment(String storeName, String storeTheme, Event.Name defaultTab,
      StoreFragment.OpenType openType);

  Fragment newStoreFragment(long userId, String storeTheme, Event.Name defaultTab,
      StoreFragment.OpenType openType);

  Fragment newStoreFragment(long userId, String storeTheme, StoreFragment.OpenType openType);

  @Deprecated Fragment newAppViewFragment(String packageName, String storeName,
      NewAppViewFragment.OpenType openType);

  Fragment newAppViewFragment(long appId, String packageName, AppViewFragment.OpenType openType,
      String tag);

  Fragment newAppViewFragment(long appId, String packageName, String tag);

  Fragment newAppViewFragment(long appId, String packageName, String storeTheme, String storeName,
      String tag);

  Fragment newAppViewFragment(SearchAdResult searchAdResult, String tag);

  @Deprecated Fragment newAppViewFragment(String packageName, NewAppViewFragment.OpenType openType);

  Fragment newFragmentTopStores();

  Fragment newUpdatesFragment();

  Fragment newLatestReviewsFragment(long storeId, StoreContext storeContext);

  /**
   * @param storeContext is needed to give context to fragment ex: store downloads vs global
   * downloads
   */
  Fragment newStoreTabGridRecyclerFragment(Event event, String storeTheme, String tag,
      StoreContext storeContext);

  /**
   * @param storeContext is needed to give context to fragment ex: store downloads vs global
   * downloads
   */
  Fragment newStoreTabGridRecyclerFragment(Event event, String title, String storeTheme, String tag,
      StoreContext storeContext);

  Fragment newListAppsFragment();

  Fragment newGetStoreFragment();

  Fragment newMyStoresSubscribedFragment();

  Fragment newMyStoresFragment();

  Fragment newGetStoreWidgetsFragment();

  @Deprecated Fragment newGetAdsFragment();

  Fragment newListStoresFragment();

  @Deprecated Fragment newSubscribedStoresFragment(Event event, String storeTheme, String tag,
      StoreContext storeName);

  Fragment newDownloadsFragment();

  Fragment newOtherVersionsFragment(String appName, String appImgUrl, String appPackage);

  @Deprecated Fragment newExcludedUpdatesFragment();

  @Deprecated Fragment newRateAndReviewsFragment(long appId, String appName, String storeName,
      String packageName, String storeTheme);

  Fragment newRateAndReviewsFragment(long appId, String appName, String storeName,
      String packageName, long reviewId);

  Fragment newDescriptionFragment(String appName, String description, String storeTheme);

  Fragment newSocialFragment(String socialUrl, String pageTitle);

  Fragment newSettingsFragment();

  Fragment newTimeLineFollowersUsingUserIdFragment(Long id, String storeTheme, String title,
      StoreContext storeName);

  Fragment newTimeLineFollowingFragmentUsingUserId(Long id, String storeTheme, String title,
      StoreContext storeContext);

  Fragment newTimeLineFollowersUsingStoreIdFragment(Long id, String storeTheme, String title,
      StoreContext storeContext);

  Fragment newTimeLineFollowingFragmentUsingStoreId(Long id, String storeTheme, String title,
      StoreContext storeName);

  Fragment newTimeLineLikesFragment(String cardUid, long numberOfLikes, String storeTheme,
      String title, StoreContext storeContext);

  @Deprecated Fragment newCommentGridRecyclerFragmentUrl(CommentType commentType, String url,
      String storeAnalyticsAction, StoreContext storeContext);

  @Deprecated Fragment newAddressBookFragment();

  Fragment newSyncSuccessFragment(List<Contact> contacts, String tag);

  Fragment newPhoneInputFragment(String tag);

  Fragment newInviteFriendsFragment(InviteFriendsContract.View.OpenMode openMode, String tag);

  @Deprecated Fragment newThankYouConnectingFragment(String tag);

  Fragment newTimeLineFollowersFragment(String storeTheme, String title, StoreContext storeContext);

  Fragment newRecommendedStoresFragment();
}
