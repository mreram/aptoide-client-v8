package cm.aptoide.pt.home;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import cm.aptoide.pt.R;
import cm.aptoide.pt.networking.image.ImageLoader;
import cm.aptoide.pt.view.app.FeatureGraphicApplication;
import java.text.DecimalFormat;
import rx.subjects.PublishSubject;

/**
 * Created by jdandrade on 08/03/2018.
 */

class FeatureGraphicInBundleViewHolder extends RecyclerView.ViewHolder {
  private final DecimalFormat oneDecimalFormatter;
  private final PublishSubject<HomeEvent> appClickedEvents;
  private final TextView nameTextView;
  private final ImageView featureGraphic;
  private final TextView rating;

  public FeatureGraphicInBundleViewHolder(View view, DecimalFormat oneDecimalFormatter,
      PublishSubject<HomeEvent> appClickedEvents) {
    super(view);
    this.oneDecimalFormatter = oneDecimalFormatter;
    this.appClickedEvents = appClickedEvents;
    nameTextView = ((TextView) itemView.findViewById(R.id.app_name));
    featureGraphic = ((ImageView) itemView.findViewById(R.id.featured_graphic));
    rating = (TextView) itemView.findViewById(R.id.rating_label);
  }

  public void setFeatureGraphicApplication(FeatureGraphicApplication featureGraphicApplication,
      HomeBundle homeBundle, int bundlePosition, int graphicPosition) {
    nameTextView.setText(featureGraphicApplication.getName());
    ImageLoader.with(itemView.getContext())
        .load(featureGraphicApplication.getFeatureGraphic(), R.drawable.placeholder_brick,
            featureGraphic);

    float rating = featureGraphicApplication.getRating();
    if (rating == 0) {
      this.rating.setText(R.string.appcardview_title_no_stars);
    } else {
      this.rating.setText(oneDecimalFormatter.format(rating));
    }
    itemView.setOnClickListener(v -> appClickedEvents.onNext(
        new AppHomeEvent(featureGraphicApplication, graphicPosition, homeBundle, bundlePosition,
            HomeEvent.Type.APP)));
  }
}
