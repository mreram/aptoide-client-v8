package cm.aptoide.pt.networking.image;

import android.app.Service;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import cm.aptoide.pt.utils.AptoideUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.NotificationTarget;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutionException;

/**
 * Created by neuro on 24-05-2016.
 */
public class ImageLoader {

  private static final String TAG = ImageLoader.class.getName();

  private final WeakReference<Context> weakContext;
  private final Resources resources;
  private final WindowManager windowManager;

  private ImageLoader(Context context) {
    this.weakContext = new WeakReference<>(context);
    this.resources = context.getResources();
    this.windowManager = ((WindowManager) context.getSystemService(Service.WINDOW_SERVICE));
  }

  public static ImageLoader with(Context context) {
    return new ImageLoader(context);
  }

  /**
   * Cancel the image loading request
   *
   * @param target Previously returned {@link Target} from {@link ImageLoader}.load...
   */
  public static void cancel(Context context, View target) {
    Glide.with(context)
        .clear(target);
  }

  /**
   * Cancel the image loading request
   *
   * @param target Previously returned {@link Target} from {@link ImageLoader}.load...
   */
  public static <R> void cancel(Context context, FutureTarget<R> target) {
    Glide.with(context)
        .clear(target);
  }

  /**
   * Cancel the image loading request
   *
   * @param target Previously returned {@link Target} from {@link ImageLoader}.load...
   */
  public static <R> void cancel(Context context, Target<R> target) {
    Glide.with(context)
        .clear(target);
  }

  /**
   * Blocking call to load a bitmap.
   *
   * @param uri Path for the bitmap to be loaded.
   *
   * @return Loaded bitmap or null.
   */
  @WorkerThread public @Nullable Bitmap loadBitmap(String uri) {
    Context context = weakContext.get();
    if (context != null) {
      try {
        return Glide.
            with(context)
            .
                asBitmap()
            .
                load(uri)
            .
                into(-1, -1). // full size
            get();
      } catch (InterruptedException e) {
        e.printStackTrace();
      } catch (ExecutionException e) {
        e.printStackTrace();
      }
    } else {
      Log.e(TAG, "::loadBitmap() Context is null");
    }
    return null;
  }

  /**
   * Mutates URL to append "_50x50" to load an avatar image from an image URL.
   *
   * @param url original image URL
   * @param imageView destination container for the image
   * @param placeHolderDrawableId placeholder while the image is loading or when is not loaded
   */
  public Target<Drawable> loadWithCircleTransformAndPlaceHolderAvatarSize(String url,
      ImageView imageView, @DrawableRes int placeHolderDrawableId) {
    return loadWithCircleTransformAndPlaceHolder(
        AptoideUtils.IconSizeU.generateStringAvatar(url, resources, windowManager), imageView,
        placeHolderDrawableId);
  }

  public Target<Drawable> loadWithCircleTransformAndPlaceHolder(String url, ImageView imageView,
      @DrawableRes int placeHolderDrawableId) {
    Context context = weakContext.get();
    if (context != null) {
      return Glide.with(context)
          .load(url)
          .apply(new RequestOptions().transform(new CircleTransform())
              .placeholder(placeHolderDrawableId))
          .into(imageView);
    } else {
      Log.e(TAG, "::loadWithCircleTransformAndPlaceHolder() Context is null");
    }
    return null;
  }

  public Target<Drawable> loadWithCircleTransform(@DrawableRes int drawableId,
      ImageView imageView) {
    Context context = weakContext.get();
    if (context != null) {
      return Glide.with(context)
          .load(drawableId)
          .apply(new RequestOptions().transform(new CircleTransform()))
          .into(imageView);
    } else {
      Log.e(TAG, "::loadWithShadowCircleTransform() Context is null");
    }
    return null;
  }

  public Target<Drawable> loadWithShadowCircleTransform(String url, ImageView imageView) {
    Context context = weakContext.get();
    if (context != null) {
      return Glide.with(context)
          .load(url)
          .apply(new RequestOptions().transform(new ShadowCircleTransformation(context, imageView)))
          .into(imageView);
    } else {
      Log.e(TAG, "::loadWithShadowCircleTransform() Context is null");
    }
    return null;
  }

  public Target<Drawable> loadWithShadowCircleTransformWithPlaceholder(String url,
      ImageView imageView, int drawable) {
    Context context = weakContext.get();
    if (context != null) {
      return Glide.with(context)
          .load(url)
          .apply(new RequestOptions().transform(new ShadowCircleTransformation(context))
              .placeholder(drawable))
          .into(imageView);
    } else {
      Log.e(TAG, "::loadWithShadowCircleTransform() Context is null");
    }
    return null;
  }

  public Target<Drawable> loadWithShadowCircleTransform(String url, ImageView imageView,
      @ColorInt int color, float spaceBetween, float strokeSize) {
    Context context = weakContext.get();
    if (context != null) {
      return Glide.with(context)
          .load(url)
          .apply(new RequestOptions().transform(
              new ShadowCircleTransformation(context, imageView, color, spaceBetween, strokeSize)))
          .into(imageView);
    } else {
      Log.e(TAG, "::loadWithShadowCircleTransform() Context is null");
    }
    return null;
  }

  public Target<Drawable> loadWithShadowCircleTransform(@DrawableRes int drawableId,
      ImageView imageView) {
    Context context = weakContext.get();
    if (context != null) {
      return Glide.with(context)
          .load(drawableId)
          .apply(new RequestOptions().transform(new ShadowCircleTransformation(context, imageView)))
          .into(imageView);
    } else {
      Log.e(TAG, "::loadWithShadowCircleTransform() Context is null");
    }
    return null;
  }

  public Target<Drawable> loadWithShadowCircleTransform(String url, ImageView imageView,
      @ColorInt int shadowColor) {
    Context context = weakContext.get();
    if (context != null) {
      return Glide.with(context)
          .load(AptoideUtils.IconSizeU.generateSizeStoreString(url, resources, windowManager))
          .apply(new RequestOptions().transform(
              new ShadowCircleTransformation(context, imageView, shadowColor)))
          .into(imageView);
    } else {
      Log.e(TAG, "::loadWithShadowCircleTransform() Context is null");
    }
    return null;
  }

  public Target<Drawable> loadWithShadowCircleTransform(String url, ImageView imageView,
      float strokeSize) {
    Context context = weakContext.get();
    if (context != null) {
      return Glide.with(context)
          .load(AptoideUtils.IconSizeU.generateSizeStoreString(url, resources, windowManager))
          .apply(new RequestOptions().transform(
              new ShadowCircleTransformation(context, imageView, strokeSize)))
          .into(imageView);
    } else {
      Log.e(TAG, "::loadWithShadowCircleTransform() Context is null");
    }
    return null;
  }

  public Target<Drawable> loadWithShadowCircleTransformWithPlaceholder(String url,
      ImageView imageView, float strokeSize, int drawable) {
    Context context = weakContext.get();
    if (context != null) {
      return Glide.with(context)
          .load(AptoideUtils.IconSizeU.generateSizeStoreString(url, resources, windowManager))
          .apply(new RequestOptions().transform(
              new ShadowCircleTransformation(context, imageView, strokeSize))
              .placeholder(drawable))
          .into(imageView);
    } else {
      Log.e(TAG, "::loadWithShadowCircleTransform() Context is null");
    }
    return null;
  }

  public Target<Drawable> loadWithShadowCircleTransform(@DrawableRes int drawableId,
      ImageView imageView, @ColorInt int shadowColor) {
    Context context = weakContext.get();
    if (context != null) {
      return Glide.with(context)
          .load(drawableId)
          .apply(new RequestOptions().transform(
              new ShadowCircleTransformation(context, imageView, shadowColor)))
          .into(imageView);
    } else {
      Log.e(TAG, "::loadWithShadowCircleTransform() Context is null");
    }
    return null;
  }

  public NotificationTarget loadImageToNotification(NotificationTarget notificationTarget,
      String url) {
    Context context = weakContext.get();
    if (context != null) {
      return Glide.with(context.getApplicationContext())
          .asBitmap()
          .load(url)
          .into(notificationTarget);
    } else {
      Log.e(TAG, "::loadImageToNotification() Context is null");
    }
    return notificationTarget;
  }

  public Target<Drawable> load(@DrawableRes int drawableId, ImageView imageView) {
    Context context = weakContext.get();
    if (context != null) {
      return Glide.with(context)
          .load(drawableId)
          .into(imageView);
    } else {
      Log.e(TAG, "::load() Context is null");
    }
    return null;
  }

  public Target<Drawable> loadScreenshotToThumb(String url, String orientation,
      @DrawableRes int loadingPlaceHolder, ImageView imageView) {
    Context context = weakContext.get();
    if (context != null) {
      return Glide.with(context)
          .load(
              AptoideUtils.IconSizeU.screenshotToThumb(url, orientation, windowManager, resources))
          .apply(new RequestOptions().placeholder(loadingPlaceHolder))
          .into(imageView);
    } else {
      Log.e(TAG, "::loadScreenshotToThumb() Context is null");
    }
    return null;
  }

  public Target<Drawable> load(String url, @DrawableRes int loadingPlaceHolder,
      ImageView imageView) {
    Context context = weakContext.get();
    if (context != null) {
      return Glide.with(context)
          .load(url)
          .apply(new RequestOptions().placeholder(loadingPlaceHolder))
          .into(imageView);
    } else {
      Log.e(TAG, "::load() Context is null");
    }
    return null;
  }

  public Target<Drawable> load(String url, ImageView imageView) {
    Context context = weakContext.get();
    if (context != null) {
      Uri uri = Uri.parse(AptoideUtils.IconSizeU.getNewImageUrl(url, resources, windowManager));
      return Glide.with(context)
          .load(uri)
          .into(imageView);
    } else {
      Log.e(TAG, "::load() Context is null");
    }
    return null;
  }

  public Target<Drawable> loadWithCenterCrop(String url, ImageView imageView) {
    return loadWithoutResizeCenterCrop(
        AptoideUtils.IconSizeU.getNewImageUrl(url, resources, windowManager), imageView);
  }

  public Target<Drawable> loadWithoutResizeCenterCrop(String url, ImageView imageView) {
    Context context = weakContext.get();
    if (context != null) {
      return Glide.with(context)
          .load(url)
          .apply(new RequestOptions().centerCrop())
          .into(imageView);
    } else {
      Log.e(TAG, "::load() Context is null");
    }
    return null;
  }

  /**
   * Loads a Drawable resource from the app bundle.
   *
   * @param drawableId drawable id
   *
   * @return {@link Drawable} with the passing drawable id or null if id = 0
   */
  public Drawable load(@DrawableRes int drawableId) {
    if (drawableId == 0) {
      return null;
    }

    Context context = weakContext.get();
    if (context != null) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        return context.getResources()
            .getDrawable(drawableId, context.getTheme());
      }
      return context.getResources()
          .getDrawable(drawableId);
    } else {
      Log.e(TAG, "::load() Context is null");
    }
    return null;
  }

  /**
   * Blocking call to load a bitmap.
   *
   * @return Loaded bitmap or null.
   */
  @WorkerThread public @Nullable Bitmap load(String apkIconPath) {
    Context context = weakContext.get();
    if (context != null) {
      try {
        return Glide.
            with(context)
            .asBitmap()
            .load(apkIconPath)
            .into(-1, -1) // full size
            .get();
      } catch (InterruptedException e) {
        e.printStackTrace();
      } catch (ExecutionException e) {
        e.printStackTrace();
      }
    } else {
      Log.e(TAG, "::load() Context is null");
    }
    return null;
  }

  public Target<Drawable> loadUsingCircleTransform(@DrawableRes int drawableId,
      ImageView imageView) {
    Context context = weakContext.get();
    if (context != null) {
      return Glide.with(context)
          .load(drawableId)
          .apply(new RequestOptions().transform(new CircleTransform()))
          .into(imageView);
    } else {
      Log.e(TAG, "::loadUsingCircleTransform() Context is null");
    }
    return null;
  }

  public Target<Drawable> loadUsingCircleTransform(@NonNull String url,
      @NonNull ImageView imageView) {
    Context context = weakContext.get();
    if (context != null) {
      return Glide.with(context)
          .load(url)
          .apply(new RequestOptions().transform(new CircleTransform()))
          .into(imageView);
    } else {
      Log.e(TAG, "::loadUsingCircleTransform() Context is null");
    }
    return null;
  }

  public Target<Drawable> loadUsingCircleTransformAndPlaceholder(String url, ImageView imageView,
      int defaultImagePlaceholder) {
    Context context = weakContext.get();
    if (context != null) {
      return Glide.with(context)
          .load(url)
          .apply(new RequestOptions().transform(new CircleTransform())
              .placeholder(defaultImagePlaceholder))
          .into(imageView);
    } else {
      Log.e(TAG, "::loadUsingCircleTransformAndPlaceholder() Context is null");
    }
    return null;
  }

  public void loadWithRoundCorners(String image, int radius, int margin, ImageView previewImage) {
    Context context = weakContext.get();

    if (context != null) {
      Glide.with(context)
          .load(image)
          .apply(new RequestOptions().transforms(new CenterCrop(),
              new RoundedCornersTransform(context, radius, margin,
                  RoundedCornersTransform.CornerType.LEFT)))
          .into(previewImage);
    }
  }

  public void loadWithRoundCorners(String image, int radius, ImageView previewImage,
      @DrawableRes int placeHolderDrawableId) {
    Context context = weakContext.get();
    if (context != null) {
      Glide.with(context)
          .load(image)
          .apply(new RequestOptions().centerCrop()
              .placeholder(placeHolderDrawableId)
              .transforms(new CenterCrop(), new RoundedCornersTransform(context, radius, 0,
                  RoundedCornersTransform.CornerType.ALL)))
          .into(previewImage);
    }
  }

  public void loadIntoTarget(String imageUrl, SimpleTarget<Drawable> simpleTarget) {
    Context context = weakContext.get();
    if (context != null) {
      Glide.with(context)
          .load(imageUrl)
          .into(simpleTarget);
    }
  }
}
