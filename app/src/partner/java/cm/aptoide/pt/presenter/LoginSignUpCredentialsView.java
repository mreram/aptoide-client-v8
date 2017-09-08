package cm.aptoide.pt.presenter;

/**
 * Created by danielchen on 08/09/17.
 */

import android.content.Context;
import android.support.annotation.NonNull;
import cm.aptoide.pt.view.account.AptoideAccountViewModel;
import cm.aptoide.pt.view.account.FacebookAccountViewModel;
import rx.Observable;

/**
 * Created by marcelobenites on 07/02/17.
 */

public interface LoginSignUpCredentialsView
        extends View
{

    Observable<Void> showAptoideLoginAreaClick();

    Observable<Void> showAptoideSignUpAreaClick();

    void showAptoideSignUpArea();

    void showAptoideLoginArea();

    void showLoading();

    void hideLoading();

    void showError(Throwable throwable);

    //void showFacebookLogin();
//
    //void showPermissionsRequiredMessage();
//
    //void hideFacebookLogin();

    void showForgotPasswordView();

    void showPassword();

    void hidePassword();

    Observable<Void> showHidePasswordClick();

    Observable<Void> forgotPasswordClick();

    void dismiss();

    void hideKeyboard();

    //Observable<FacebookAccountViewModel> facebookLoginClick();

    Observable<AptoideAccountViewModel> aptoideLoginClick();

    Observable<AptoideAccountViewModel> aptoideSignUpClick();

    boolean tryCloseLoginBottomSheet();

    @NonNull AptoideAccountViewModel getCredentials();

    boolean isPasswordVisible();

    Context getApplicationContext();

    void lockScreenRotation();

    void unlockScreenRotation();
}
