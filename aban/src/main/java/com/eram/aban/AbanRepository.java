package com.eram.aban;


import com.eram.aban.model.Shamad;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;

public class AbanRepository {

    private final AbanInterface abanInterface;

    public AbanRepository() {
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(httpLoggingInterceptor)
                .build();


        Retrofit retrofit = new Retrofit.Builder().baseUrl(BuildConfig.base_url)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient)
                .build();
        abanInterface = retrofit.create(AbanInterface.class);
    }

    public Observable<Shamad> getShamedInfo(String packageName) {
        return abanInterface.getShamed(packageName);
    }

    interface AbanInterface {
        @GET("shamed")
        Observable<Shamad> getShamed(@Query("package_name") String packageName);
    }


}
