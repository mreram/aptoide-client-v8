package cm.aptoide.pt.v8engine.payment;

import android.os.Bundle;
import cm.aptoide.pt.v8engine.analytics.Analytics;
import cm.aptoide.pt.v8engine.analytics.Event;
import cm.aptoide.pt.v8engine.analytics.events.FacebookEvent;
import com.facebook.appevents.AppEventsLogger;

public class PaymentAnalytics {

  private final Analytics analytics;
  private final AppEventsLogger facebook;
  private final String aptoidePackageName;

  public PaymentAnalytics(Analytics analytics, AppEventsLogger facebook,
      String aptoidePackageName) {
    this.analytics = analytics;
    this.facebook = facebook;
    this.aptoidePackageName = aptoidePackageName;
  }

  public void sendPaidAppBuyButtonPressedEvent(Product product) {
    analytics.sendEvent(
        new FacebookEvent(facebook, "Clicked_On_Buy_Button", getProductBundle(product)));
  }

  public void sendPaymentCancelButtonPressedEvent(Product product, Payment payment) {
    analytics.sendEvent(getPaymentEvent("Payment_Pop_Up", "Cancel", payment, product));
  }

  public void sendPaymentBuyButtonPressedEvent(Product product, Payment payment) {
    analytics.sendEvent(getPaymentEvent("Payment_Pop_Up", "Buy", payment, product));
  }

  public void sendPaymentTapOutsideEvent(Product product, Payment payment) {
    analytics.sendEvent(getPaymentEvent("Payment_Pop_Up", "Tap Outside", payment, product));
  }

  public void sendBackToStoreButtonPressedEvent(Product product) {
    final Bundle bundle = getProductBundle(product);
    bundle.putString("action", "Voltar para loja button");
    analytics.sendEvent(new FacebookEvent(facebook, "Payment_Authorization_Confirmation", bundle));
  }

  public void sendPaymentAuthorizationBackButtonPressedEvent(Product product) {
    final Bundle bundle = getProductBundle(product);
    bundle.putString("action", "Android back button");
    analytics.sendEvent(new FacebookEvent(facebook, "Payment_Authorization_Confirmation", bundle));
  }

  public void sendPurchaseNetworkRetryEvent() {
    final Bundle bundle = new Bundle();
    bundle.putString("package_name_seller", aptoidePackageName);
    analytics.sendEvent(new FacebookEvent(facebook, "Payment_Purchase_Retry", bundle));
  }

  public void sendPurchaseCompleteEvent(PaymentConfirmation paymentConfirmation, Product product) {

    // We only send analytics about failed or completed payment confirmations
    if (paymentConfirmation.isPending() || paymentConfirmation.isNew()) {
      return;
    }

    final Bundle bundle = getProductBundle(product);
    bundle.putString("status", getPurchaseStatus(paymentConfirmation));
    analytics.sendEvent(new FacebookEvent(facebook, "Payment_Purchase_Complete", bundle));
  }

  private String getPurchaseStatus(PaymentConfirmation paymentConfirmation) {

    if (paymentConfirmation.isFailed()) {
      return "failed";
    }

    if (paymentConfirmation.isCompleted()) {
      return "success";
    }

    throw new IllegalArgumentException("Can NOT determine payment confirmation analytics status.");
  }

  private Event getPaymentEvent(String eventName, String action, Payment payment, Product product) {
    final Bundle bundle = getProductBundle(product);
    bundle.putString("action", action);
    bundle.putString("payment_method", payment.getName());
    return new FacebookEvent(facebook, eventName, bundle);
  }

  private Bundle getProductBundle(Product product) {
    final Bundle bundle = new Bundle();
    bundle.putDouble("purchase_value", product.getPrice().getAmount());
    bundle.putString("purchase_currency", product.getPrice().getCurrency());
    bundle.putString("package_name_seller", aptoidePackageName);
    return bundle;
  }
}