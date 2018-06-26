/*
 * Copyright (c) 2016.
 * Modified by Neurophobic Animal on 30/06/2016.
 */

package cm.aptoide.pt.app.view;

import android.graphics.Typeface;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import cm.aptoide.pt.AptoideApplication;
import cm.aptoide.pt.R;
import cm.aptoide.pt.crashreports.CrashReport;
import cm.aptoide.pt.dataprovider.model.v7.listapp.App;
import cm.aptoide.pt.home.apps.AppsNavigator;
import cm.aptoide.pt.networking.image.ImageLoader;
import cm.aptoide.pt.utils.AptoideUtils;
import cm.aptoide.pt.view.recycler.widget.Widget;
import com.jakewharton.rxbinding.view.RxView;
import java.util.Date;
import javax.inject.Inject;

/**
 * Created by neuro on 29-06-2016.
 */
public class GridAppListWidget extends Widget<GridAppListDisplayable> {

  public TextView name;
  public ImageView icon;
  @Inject AppsNavigator appsNavigator;
  private TextView tvTimeSinceModified;
  private TextView tvStoreName;

  public GridAppListWidget(View itemView) {
    super(itemView);
  }

  @Override protected void assignViews(View itemView) {
    name = (TextView) itemView.findViewById(R.id.name);
    icon = (ImageView) itemView.findViewById(R.id.icon);
    tvTimeSinceModified = (TextView) itemView.findViewById(R.id.timeSinceModified);
    tvStoreName = (TextView) itemView.findViewById(R.id.storeName);
  }

  @Override public void bindView(GridAppListDisplayable displayable) {
    App app = displayable.getPojo();
    name.setText(app.getName());

    Date modified = app.getUpdated();
    if (modified != null) {
      tvTimeSinceModified.setText(AptoideUtils.DateTimeU.getInstance(itemView.getContext())
          .getTimeDiffString(itemView.getContext(), modified.getTime(),
              getContext().getResources()));
    }

    name.setText(app.getName());
    name.setTypeface(null, Typeface.BOLD);

    tvStoreName.setText(app.getStore()
        .getName());
    tvStoreName.setTypeface(null, Typeface.BOLD);
    final FragmentActivity context = getContext();
    compositeSubscription.add(RxView.clicks(itemView)
        .subscribe(v -> {
          // FIXME
          getFragmentNavigator().navigateTo(AptoideApplication.getFragmentProvider()
              .newAppViewFragment(app.getId(), app.getPackageName(), displayable.getTag()), true);
        }, throwable -> CrashReport.getInstance()
            .log(throwable)));

    ImageLoader.with(context)
        .load(app.getIcon(), icon);
  }
}
