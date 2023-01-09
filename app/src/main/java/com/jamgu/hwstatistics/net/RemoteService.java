package com.jamgu.hwstatistics.net;

import com.jamgu.hwstatistics.net.model.UserModel;

import java.util.List;

import io.reactivex.Observable;
import okhttp3.MultipartBody;
import retrofit2.http.Body;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

/**
 * 网络请求的所有的接口
 *
 * @version 1.0.0
 */
public interface RemoteService {

    @Multipart
    @POST("upload")
    Observable<RspModel> upload(@Part List<MultipartBody.Part> part);

    @POST("register")
    Observable<RspModel> register(@Body UserModel model);

}
