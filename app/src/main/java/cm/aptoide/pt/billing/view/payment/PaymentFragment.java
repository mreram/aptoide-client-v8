package cm.aptoide.pt.billing.view.payment;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import cm.aptoide.pt.AptoideApplication;
import cm.aptoide.pt.R;
import cm.aptoide.analytics.implementation.navigation.ScreenTagHistory;
import cm.aptoide.pt.billing.Billing;
import cm.aptoide.pt.billing.BillingAnalytics;
import cm.aptoide.pt.billing.BillingIdManager;
import cm.aptoide.pt.billing.payment.PaymentService;
import cm.aptoide.pt.billing.product.Product;
import cm.aptoide.pt.billing.view.BillingActivity;
import cm.aptoide.pt.billing.view.BillingNavigator;
import cm.aptoide.pt.navigator.ActivityResultNavigator;
import cm.aptoide.pt.networking.image.ImageLoader;
import cm.aptoide.pt.permission.PermissionServiceFragment;
import cm.aptoide.pt.utils.AptoideUtils;
import cm.aptoide.pt.view.rx.RxAlertDialog;
import cm.aptoide.pt.view.spannable.SpannableFactory;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.widget.RxRadioGroup;
import java.util.HashSet;
import java.util.List;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

public class PaymentFragment extends PermissionServiceFragment implements PaymentView {

  private View overlay;
  private View progressView;
  private RadioGroup serviceRadioGroup;
  private ImageView productIcon;
  private TextView productName;
  private TextView productDescription;
  private TextView noPaymentsText;
  private Button cancelButton;
  private Button buyButton;
  private TextView productPrice;

  private RxAlertDialog networkErrorDialog;
  private RxAlertDialog unknownErrorDialog;
  private SpannableFactory spannableFactory;

  private boolean paymentLoading;
  private boolean transactionLoading;
  private boolean buyLoading;

  private Billing billing;
  private BillingAnalytics billingAnalytics;
  private BillingNavigator billingNavigator;
  private ScrollView scroll;
  private BillingIdManager billingIdManager;

  public static Fragment create(Bundle bundle) {
    final PaymentFragment fragment = new PaymentFragment();
    fragment.setArguments(bundle);
    return fragment;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    billing = ((AptoideApplication) getContext().getApplicationContext()).getBilling(
        getArguments().getString(BillingActivity.EXTRA_MERCHANT_NAME));
    billingAnalytics =
        ((AptoideApplication) getContext().getApplicationContext()).getBillingAnalytics();
    billingNavigator = ((ActivityResultNavigator) getContext()).getBillingNavigator();
    billingIdManager = ((AptoideApplication) getContext().getApplicationContext()).getIdResolver(
        getArguments().getString(BillingActivity.EXTRA_MERCHANT_NAME));
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    spannableFactory = new SpannableFactory();
    overlay = view.findViewById(R.id.fragment_payment_overlay);
    scroll = (ScrollView) view.findViewById(R.id.fragment_payment_scroll);
    progressView = view.findViewById(R.id.fragment_payment_global_progress_bar);
    noPaymentsText = (TextView) view.findViewById(R.id.fragment_payment_no_payments_text);

    productIcon = (ImageView) view.findViewById(R.id.include_payment_product_icon);
    productName = (TextView) view.findViewById(R.id.include_payment_product_name);
    productDescription = (TextView) view.findViewById(R.id.include_payment_product_description);

    productPrice = (TextView) view.findViewById(R.id.include_payment_product_price);
    serviceRadioGroup = (RadioGroup) view.findViewById(R.id.fragment_payment_list);

    cancelButton = (Button) view.findViewById(R.id.include_payment_buttons_cancel_button);
    buyButton = (Button) view.findViewById(R.id.include_payment_buttons_buy_button);

    networkErrorDialog =
        new RxAlertDialog.Builder(getContext()).setMessage(R.string.connection_error)
            .setPositiveButton(android.R.string.ok)
            .build();
    unknownErrorDialog =
        new RxAlertDialog.Builder(getContext()).setMessage(R.string.all_message_general_error)
            .setPositiveButton(android.R.string.ok)
            .build();

    attachPresenter(new PaymentPresenter(this, billing, billingNavigator, billingAnalytics,
        getArguments().getString(BillingActivity.EXTRA_MERCHANT_NAME),
        getArguments().getString(BillingActivity.EXTRA_SKU),
        getArguments().getString(BillingActivity.EXTRA_DEVELOPER_PAYLOAD), new HashSet<>()));
  }

  @Override public ScreenTagHistory getHistoryTracker() {
    return ScreenTagHistory.Builder.build(this.getClass()
        .getSimpleName());
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_payment, container, false);
  }

  @Override public void onDestroyView() {
    scroll = null;
    spannableFactory = null;
    overlay = null;
    progressView = null;
    noPaymentsText = null;
    productIcon = null;
    productName = null;
    productDescription = null;
    productPrice = null;
    serviceRadioGroup = null;
    cancelButton = null;
    buyButton = null;
    networkErrorDialog.dismiss();
    networkErrorDialog = null;
    unknownErrorDialog.dismiss();
    unknownErrorDialog = null;
    paymentLoading = false;
    transactionLoading = false;
    buyLoading = false;
    super.onDestroyView();
  }

  @Override public Observable<String> selectServiceEvent() {
    return RxRadioGroup.checkedChanges(serviceRadioGroup)
        .filter(serviceId -> serviceId != -1)
        .map(serviceId -> billingIdManager.generateServiceId(serviceId));
  }

  @Override public Observable<Void> cancelEvent() {
    return Observable.merge(RxView.clicks(cancelButton), RxView.clicks(overlay))
        .subscribeOn(AndroidSchedulers.mainThread())
        .unsubscribeOn(AndroidSchedulers.mainThread());
  }

  @Override public Observable<Void> buyEvent() {
    return RxView.clicks(buyButton)
        .subscribeOn(AndroidSchedulers.mainThread())
        .unsubscribeOn(AndroidSchedulers.mainThread());
  }

  @Override public void showPaymentLoading() {
    paymentLoading = true;
    progressView.setVisibility(View.VISIBLE);
  }

  @Override public void showPurchaseLoading() {
    transactionLoading = true;
    progressView.setVisibility(View.VISIBLE);
  }

  @Override public void showBuyLoading() {
    buyLoading = true;
    progressView.setVisibility(View.VISIBLE);
  }

  @Override
  public void showPayments(List<PaymentService> services, PaymentService selectedService) {
    serviceRadioGroup.removeAllViews();
    noPaymentsText.setVisibility(View.GONE);
    buyButton.setVisibility(View.VISIBLE);

    RadioButton radioButton;
    CharSequence radioText;
    for (PaymentService service : services) {

      radioButton = (RadioButton) getActivity().getLayoutInflater()
          .inflate(R.layout.payment_item, serviceRadioGroup, false);
      radioButton.setId((int) billingIdManager.resolveServiceId(service.getId()));

      Glide.with(this)
          .load(service.getIcon())
          .into(new RadioButtonTarget(AptoideUtils.ScreenU.getPixelsForDip(16, getResources()),
              radioButton));

      if (TextUtils.isEmpty(service.getDescription())) {
        radioText = service.getName();
      } else {
        radioText = spannableFactory.createTextAppearanceSpan(getContext(),
            R.style.TextAppearance_Aptoide_Caption,
            service.getName() + "\n" + service.getDescription(), service.getDescription());
      }
      radioButton.setText(radioText);

      radioButton.setChecked(selectedService.getId()
          .equals(service.getId()));

      serviceRadioGroup.addView(radioButton);
    }
  }

  @Override public void showProduct(Product product) {
    ImageLoader.with(getContext())
        .load(product.getIcon(), productIcon);
    productName.setText(product.getTitle());
    productDescription.setText(product.getDescription());
    productPrice.setText(product.getPrice()
        .getCurrencySymbol() + " " + product.getPrice()
        .getAmount());
  }

  @Override public void hidePaymentLoading() {
    paymentLoading = false;
    if (!transactionLoading && !buyLoading) {
      progressView.setVisibility(View.GONE);
    }
  }

  @Override public void hidePurchaseLoading() {

    if (transactionLoading) {
      scroll.postDelayed(new Runnable() {
        @Override public void run() {
          if (scroll != null) {
            scroll.fullScroll(View.FOCUS_DOWN);
          }
        }
      }, 500);
    }

    transactionLoading = false;
    if (!paymentLoading && !buyLoading) {
      progressView.setVisibility(View.GONE);
    }
  }

  @Override public void hideBuyLoading() {
    buyLoading = false;
    if (!paymentLoading && !transactionLoading) {
      progressView.setVisibility(View.GONE);
    }
  }

  @Override public void showPaymentsNotFoundMessage() {
    noPaymentsText.setVisibility(View.VISIBLE);
    buyButton.setVisibility(View.GONE);
  }

  @Override public void showNetworkError() {
    if (!networkErrorDialog.isShowing() && !unknownErrorDialog.isShowing()) {
      networkErrorDialog.show();
    }
  }

  @Override public void showUnknownError() {
    if (!networkErrorDialog.isShowing() && !unknownErrorDialog.isShowing()) {
      unknownErrorDialog.show();
    }
  }

  private static class RadioButtonTarget extends SimpleTarget<Drawable> {

    private RadioButton radioButton;

    public RadioButtonTarget(int pixels, RadioButton radioButton) {
      super(pixels, pixels);
      this.radioButton = radioButton;
    }

    @Override public void onResourceReady(Drawable glideDrawable,
        Transition<? super Drawable> glideAnimation) {
      radioButton.setCompoundDrawablesWithIntrinsicBounds(null, null, glideDrawable.getCurrent(),
          null);
    }

    @Override public void onDestroy() {
      radioButton = null;
      super.onDestroy();
    }
  }
}