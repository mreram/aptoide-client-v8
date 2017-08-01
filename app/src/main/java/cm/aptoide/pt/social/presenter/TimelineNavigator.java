package cm.aptoide.pt.social.presenter;

import cm.aptoide.pt.dataprovider.model.v7.Event;
import cm.aptoide.pt.dataprovider.util.CommentType;
import cm.aptoide.pt.V8Engine;
import cm.aptoide.pt.view.account.LoginSignUpFragment;
import cm.aptoide.pt.view.account.MyAccountFragment;
import cm.aptoide.pt.view.app.AppViewFragment;
import cm.aptoide.pt.view.navigator.FragmentNavigator;
import cm.aptoide.pt.view.store.StoreFragment;

/**
 * Created by jdandrade on 30/06/2017.
 */

public class TimelineNavigator implements TimelineNavigation {

  private final FragmentNavigator fragmentNavigator;
  private String likesTitle;

  public TimelineNavigator(FragmentNavigator fragmentNavigator, String likesTitle) {
    this.fragmentNavigator = fragmentNavigator;
    this.likesTitle = likesTitle;
  }

  @Override
  public void navigateToAppView(long appId, String packageName, AppViewFragment.OpenType openType) {
    fragmentNavigator.navigateTo(
        AppViewFragment.newInstance(appId, packageName, AppViewFragment.OpenType.OPEN_ONLY));
  }

  @Override public void navigateToAppView(String packageName, AppViewFragment.OpenType openType) {
    fragmentNavigator.navigateTo(
        AppViewFragment.newInstance(packageName, AppViewFragment.OpenType.OPEN_ONLY));
  }

  @Override public void navigateToStoreHome(String storeName, String storeTheme) {
    fragmentNavigator.navigateTo(V8Engine.getFragmentProvider()
        .newStoreFragment(storeName, storeTheme));
  }

  @Override public void navigateToStoreTimeline(long userId, String storeTheme) {
    fragmentNavigator.navigateTo(V8Engine.getFragmentProvider()
        .newStoreFragment(userId, storeTheme, Event.Name.getUserTimeline,
            StoreFragment.OpenType.GetHome));
  }

  @Override public void navigateToStoreTimeline(String storeName, String storeTheme) {
    fragmentNavigator.navigateTo(V8Engine.getFragmentProvider()
        .newStoreFragment(storeName, storeTheme, Event.Name.getUserTimeline,
            StoreFragment.OpenType.GetHome));
  }

  @Override public void navigateToAddressBook() {
    fragmentNavigator.navigateTo(V8Engine.getFragmentProvider()
        .newAddressBookFragment());
  }

  @Override public void navigateToLoginView() {
    fragmentNavigator.navigateTo(LoginSignUpFragment.newInstance(false, false, true));
  }

  @Override public void navigateToMyAccountView() {
    fragmentNavigator.navigateTo(MyAccountFragment.newInstance());
  }

  @Override public void navigateToCommentsWithCommentDialogOpen(String cardId) {
    fragmentNavigator.navigateTo(V8Engine.getFragmentProvider()
        .newCommentGridRecyclerFragmentWithCommentDialogOpen(CommentType.TIMELINE, cardId));
  }

  // FIXME what should happen if storeId <= 0 ?
  @Override public void navigateToFollowersViewStore(Long storeId, String title) {
    if (storeId > 0) {
      fragmentNavigator.navigateTo(V8Engine.getFragmentProvider()
          .newTimeLineFollowersUsingStoreIdFragment(storeId, "DEFAULT", title));
    }
  }

  @Override public void navigateToFollowersViewStore(String title) {
    fragmentNavigator.navigateTo(V8Engine.getFragmentProvider()
        .newTimeLineFollowersFragment("DEFAULT", title));
  }

  @Override public void navigateToFollowersViewUser(Long userId, String title) {
    fragmentNavigator.navigateTo(V8Engine.getFragmentProvider()
        .newTimeLineFollowersUsingUserIdFragment(userId, "DEFAULT", title));
  }

  // FIXME what should happen if storeId <= 0 ?
  @Override public void navigateToFollowingViewStore(Long storeId, String title) {
    if (storeId > 0) {
      fragmentNavigator.navigateTo(V8Engine.getFragmentProvider()
          .newTimeLineFollowingFragmentUsingStoreId(storeId, "DEFAULT", title));
    }
  }

  @Override public void navigateToFollowingViewUser(Long userId, String title) {
    fragmentNavigator.navigateTo(V8Engine.getFragmentProvider()
        .newTimeLineFollowingFragmentUsingUserId(userId, "DEFAULT", title));
  }

  @Override public void navigateToLikesView(String cardId, long numberOfLikes) {
    fragmentNavigator.navigateTo(V8Engine.getFragmentProvider()
        .newTimeLineLikesFragment(cardId, numberOfLikes, "default", likesTitle));
  }

  @Override public void navigateToComments(String cardId) {
    fragmentNavigator.navigateTo(V8Engine.getFragmentProvider()
        .newCommentGridRecyclerFragment(CommentType.TIMELINE, cardId));
  }
}