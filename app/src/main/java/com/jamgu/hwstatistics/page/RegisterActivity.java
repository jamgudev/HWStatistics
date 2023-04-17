package com.jamgu.hwstatistics.page;

import static com.jamgu.hwstatistics.util.TimeExtensionsKt.getDateOfTodayString;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.jamgu.common.util.log.JLog;
import com.jamgu.common.util.preference.PreferenceUtil;
import com.jamgu.hwstatistics.R;
import com.jamgu.hwstatistics.net.Network;
import com.jamgu.hwstatistics.net.RspModel;
import com.jamgu.hwstatistics.net.model.UserModel;
import com.jamgu.hwstatistics.net.upload.DataUploader;
import com.jamgu.hwstatistics.net.upload.DeviceInfoManager;
import com.jamgu.krouter.annotation.KRouter;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

@KRouter(PageRouterKt.REGISTER_PAGE)
public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private EditText vTxtUsername;

    private EditText vTxtTelephone;

    private EditText vTxtOccupation;

    private Button vBtnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initView();
        initTextEvent();
        initBtnEvent();
    }

    private void initTextEvent() {
        vTxtTelephone.setOnFocusChangeListener(telephoneFocusChangeListener);
    }

    private void initBtnEvent() {
        vBtnRegister.setOnClickListener(view -> {
            // 隐藏软键盘
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
            }

            // 使按钮获得焦点，触发text的格式检验
            clearTextFocus();
            vBtnRegister.requestFocus();

            String username = vTxtUsername.getText().toString();
            String telephone = vTxtTelephone.getText().toString();
            String occupation = vTxtOccupation.getText().toString();
            // 非空检查
            if ("".equals(username) || "".equals(telephone) || "".equals(occupation)) {
                Toast.makeText(RegisterActivity.this, "请填写全部信息再进行注册", Toast.LENGTH_SHORT).show();
                return;
            }

            DeviceInfoManager.PhoneInfo phoneInfo = DeviceInfoManager.getPhoneInfo();
            JLog.d(TAG, "phoneInfo = " + phoneInfo);

            // 输入格式错误检查`
            if (vTxtTelephone.getError() != null) {
                Toast.makeText(RegisterActivity.this, "手机号码格式错误，请重新填写", Toast.LENGTH_SHORT).show();
                return;
            }


            String startFrom = getDateOfTodayString();
            UserModel userModel = new UserModel(username, telephone, occupation, phoneInfo.toString(), startFrom);

            Network.remote().register(userModel)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Observer<RspModel>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onNext(RspModel rspModel) {
                            if (rspModel.getCode() == 0) {
//                                Intent intent = new Intent(RegisterActivity.this, UserActivity.class);
//                                startActivity(intent);
                                SharedPreferences preference = PreferenceUtil.INSTANCE
                                        .getCachePreference(RegisterActivity.this, 0);
                                preference.edit().putString(DataUploader.USER_NAME, userModel.getUsername()).apply();
                                Toast.makeText(getApplicationContext(), "注册成功", Toast.LENGTH_SHORT).show();
                                finish();
                                return;
                            }
                            String msg = rspModel.getMsg();
                            if (msg.equals("用户名已存在")) {
                                Toast.makeText(RegisterActivity.this, "用户名已存在，请替换用户名", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            Toast.makeText(RegisterActivity.this, "服务器错误，请通知管理员", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(Throwable e) {
                            JLog.e(TAG, e.toString());
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        });
    }

    private void clearTextFocus() {
        vTxtUsername.clearFocus();
        vTxtTelephone.clearFocus();
        vTxtOccupation.clearFocus();
    }

    private void initView() {
        vTxtUsername = findViewById(R.id.vTxtUsername);
        vTxtTelephone = findViewById(R.id.vTxtTelephone);
        vTxtOccupation = findViewById(R.id.vTxtOccupation);
        vBtnRegister = findViewById(R.id.vBtnRegister);
    }

    private View.OnFocusChangeListener telephoneFocusChangeListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View view, boolean b) {
            if (!vTxtTelephone.hasFocus()) {
                String telephone = vTxtTelephone.getText().toString();
                if (!isMobile(telephone)) {
                    vTxtTelephone.setError(Html.fromHtml("<font>请输入正确的手机号码</font>", Html.FROM_HTML_MODE_COMPACT));
                }
            }
        }
    };

    public static boolean isMobile(String str) {
        return str.matches("[1][34578]\\d{9}");
    }

}