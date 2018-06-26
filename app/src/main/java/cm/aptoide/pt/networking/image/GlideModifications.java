package cm.aptoide.pt.networking.image;

import android.content.Context;
import android.support.annotation.NonNull;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator;
import com.bumptech.glide.module.GlideModule;
import com.bumptech.glide.request.RequestOptions;

public class GlideModifications implements GlideModule {
  @Override public void applyOptions(Context context, GlideBuilder builder) {

    builder.setDefaultRequestOptions(RequestOptions.formatOf(DecodeFormat.PREFER_RGB_565));
    // disk cache config
    //builder.setDiskCache(new ExternalCacheDiskCacheFactory(context));
    // using defaults

    MemorySizeCalculator calculator = new MemorySizeCalculator.Builder(context).build();

    // size for memory cache
    int defaultMemoryCacheSize = calculator.getMemoryCacheSize();
    builder.setMemoryCache(new LruResourceCache(defaultMemoryCacheSize));

    // size for bitmap pool
    int defaultBitmapPoolSize = calculator.getBitmapPoolSize();
    builder.setBitmapPool(new LruBitmapPool(defaultBitmapPoolSize));
  }

  @Override public void registerComponents(@NonNull Context context, @NonNull Glide glide,
      @NonNull Registry registry) {
    // does nothing
  }
}
