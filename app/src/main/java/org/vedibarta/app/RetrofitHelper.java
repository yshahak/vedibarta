package org.vedibarta.app;


import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/**
 * Created by Yaakov Shahak on 28/05/2017.
 */

class RetrofitHelper {

    private static final String TAG = RetrofitHelper.class.getSimpleName();

    private final static String BASE_URL_VEDIBARTA = "https://www.vedibarta.org/";

    public interface SendFeedbackService {
        @FormUrlEncoded
        @POST("guestbook/save.asp")
        @Headers({
                "Proxy-Connection: keep-alive",
                "Cache-Control: max-age=0",
                "Accept-Encoding: gzip,deflate",
                "Content-Type: application/x-www-form-urlencoded;charset=UTF-8"
        })
        Call<ResponseBody> sendFeedback(@Field("NAME") String name,
                                        @Field("EMAIL") String email,
                                        @Field("MESSAGE") String message);
    }

    static Call<ResponseBody> sendFeedback(String name, String mail, String text) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.hostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });
        OkHttpClient client = builder.build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL_VEDIBARTA)
                .client(client)
                .build();

        RetrofitHelper.SendFeedbackService service = retrofit.create(RetrofitHelper.SendFeedbackService.class);
        return service.sendFeedback(name, mail, text);

    }


}