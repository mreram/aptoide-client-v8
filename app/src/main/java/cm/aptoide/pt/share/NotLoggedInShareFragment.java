package cm.aptoide.pt.share;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import cm.aptoide.accountmanager.AptoideAccountManager;
import cm.aptoide.pt.AptoideApplication;
import cm.aptoide.pt.R;
import cm.aptoide.pt.account.AccountAnalytics;
import cm.aptoide.pt.account.ErrorsMapper;
import cm.aptoide.pt.account.view.AccountErrorMapper;
import cm.aptoide.pt.account.view.GooglePlayServicesFragment;
import cm.aptoide.analytics.implementation.navigation.ScreenTagHistory;
import cm.aptoide.pt.crashreports.CrashReport;
import cm.aptoide.pt.navigator.ActivityResultNavigator;
import cm.aptoide.pt.navigator.FragmentNavigator;
import cm.aptoide.pt.utils.GenericDialogs;
import cm.aptoide.pt.view.ThrowableToStringMapper;
import cm.aptoide.pt.view.rx.RxAlertDialog;
import cm.aptoide.pt.view.share.NotLoggedInShareAnalytics;
import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxrelay.PublishRelay;
import java.util.Arrays;
import javax.inject.Inject;
import rx.Observable;

public class NotLoggedInShareFragment extends GooglePlayServicesFragment
    implements NotLoggedInShareView {

  private static final String PACKAGE_NAME = "PACKAGE_NAME";
  @Inject AccountAnalytics accountAnalytics;
  @Inject NotLoggedInShareAnalytics analytics;
  private ProgressDialog progressDialog;
  private Button facebookLoginButton;
  private Button googleLoginButton;
  private Button closeButton;
  private ThrowableToStringMapper errorMapper;
  private RxAlertDialog facebookEmailRequiredDialog;
  private AptoideAccountManager accountManager;
  private int requestCode;
  private PublishRelay<Void> backButtonPress;
  private View outerLayout;
  private ClickHandler backClickHandler;
  private String packageName;

  public static NotLoggedInShareFragment newInstance(String packageName) {
    NotLoggedInShareFragment fragment = new NotLoggedInShareFragment();
    Bundle bundle = new Bundle();
    bundle.putString(PACKAGE_NAME, packageName);
    fragment.setArguments(bundle);
    return fragment;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getFragmentComponent(savedInstanceState).inject(this);
    if (getArguments() != null) {
      this.packageName = getArguments().getString(PACKAGE_NAME, "");
    }
    errorMapper = new AccountErrorMapper(getContext(), new ErrorsMapper());
    accountManager =
        ((AptoideApplication) getContext().getApplicationContext()).getAccountManager();
    requestCode = getArguments().getInt(FragmentNavigator.REQUEST_CODE_EXTRA);
    backButtonPress = PublishRelay.create();
  }

  @Override public ScreenTagHistory getHistoryTracker() {
    return ScreenTagHistory.Builder.build(this.getClass()
        .getSimpleName());
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.not_logged_in_share, container, false);
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    closeButton = (Button) view.findViewById(R.id.close_button);
    facebookLoginButton = (Button) view.findViewById(R.id.not_logged_in_share_facebook_button);
    googleLoginButton = (Button) view.findViewById(R.id.not_logged_in_share_google_button);

    progressDialog = GenericDialogs.createGenericPleaseWaitDialog(getContext());
    outerLayout = view.findViewById(R.id.outer_layout);

    facebookEmailRequiredDialog = new RxAlertDialog.Builder(getContext()).setMessage(
        R.string.facebook_email_permission_regected_message)
        .setPositiveButton(R.string.facebook_grant_permission_button)
        .setNegativeButton(android.R.string.cancel)
        .build();

    setupBackClick();

    attachPresenter(new NotLoggedInSharePresenter(this, CrashReport.getInstance(), accountManager,
        ((ActivityResultNavigator) getContext()).getAccountNavigator(),
        Arrays.asList("email", "user_friends"), Arrays.asList("email"), requestCode, errorMapper,
        analytics, packageName));
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    unregisterClickHandler(backClickHandler);
    backClickHandler = null;
  }

  @Override public void onDestroy() {
    super.onDestroy();
    backButtonPress = null;
  }

  private void setupBackClick() {
    backClickHandler = () -> {
      backButtonPress.call(null);
      return true;
    };
    registerClickHandler(backClickHandler);
  }

  private AccountAnalytics.StartupClickOrigin getStartupClickOrigin() {
    return AccountAnalytics.StartupClickOrigin.NOT_LOGGED_IN_DIALOG;
  }

  private View getRootView() {
    return getActivity().findViewById(android.R.id.content);
  }

  @Override public Observable<Void> closeEvent() {
    return RxView.clicks(closeButton);
  }

  @Override public Observable<Void> facebookSignUpEvent() {
    return RxView.clicks(facebookLoginButton)
        .doOnNext(__ -> accountAnalytics.clickIn(AccountAnalytics.StartupClick.CONNECT_FACEBOOK,
            getStartupClickOrigin()));
  }

  @Override public Observable<Void> googleSignUpEvent() {
    return RxView.clicks(googleLoginButton)
        .doOnNext(__ -> accountAnalytics.clickIn(AccountAnalytics.StartupClick.CONNECT_GOOGLE,
            getStartupClickOrigin()));
  }

  @Override public Observable<Void> facebookSignUpWithRequiredPermissionsInEvent() {
    return facebookEmailRequiredDialog.positiveClicks()
        .map(dialog -> null);
  }

  @Override public void showError(String message) {
    Snackbar.make(getRootView(), message, Snackbar.LENGTH_LONG)
        .show();
  }

  @Override public void showFacebookPermissionsRequiredError(Throwable throwable) {
    if (!facebookEmailRequiredDialog.isShowing()) {
      facebookEmailRequiredDialog.show();
    }
  }

  @Override public void showLoading() {
    progressDialog.show();
  }

  @Override public void hideLoading() {
    progressDialog.dismiss();
  }

  @Override public void showFacebookLogin() {
    facebookLoginButton.setVisibility(View.VISIBLE);
  }

  @Override public void hideFacebookLogin() {
    facebookLoginButton.setVisibility(View.GONE);
  }

  @Override public void showGoogleLogin() {
    googleLoginButton.setVisibility(View.VISIBLE);
  }

  @Override public void hideGoogleLogin() {
    googleLoginButton.setVisibility(View.GONE);
  }

  @Override public Observable<Void> backEvent() {
    return backButtonPress;
  }

  @Override public Observable<Void> getOutsideClick() {
    return RxView.clicks(outerLayout);
  }
}
