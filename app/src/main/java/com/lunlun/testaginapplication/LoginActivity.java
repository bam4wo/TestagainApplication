package com.lunlun.testaginapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.vishnusivadas.advanced_httpurlconnection.PutData;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


public class LoginActivity extends AppCompatActivity {

    String IMEINumber;
    TextView imei;
    private static final int REQUEST_CODE = 101;
    private CheckBox cb;

//  指紋辨識
    private KeyguardManager mKeyguardManager;
    private FingerprintManager mFingerprintManager;
    private CancellationSignal cancellationSignal;

    //蓉蓉
    TextInputEditText textInputEditTextIDorEmail, textInputEditTextPassword;
    Button buttonLogin;
    TextView textViewSignUp;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

//        getImei();//得到IMEI
//        checkDivicd();
        findView();

//        email = findViewById(R.id.ed_email);
//        eemail = email.getText().toString();
//        password = findViewById(R.id.ed_password);
//        pass = password.getText().toString();

        textInputEditTextIDorEmail = findViewById(R.id.IDorEmail);
        textInputEditTextPassword = findViewById(R.id.password);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewSignUp = findViewById(R.id.signUpText);
        progressBar = findViewById(R.id.progress);

        handleSSLHandshake();

        textViewSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), SignupActivity.class);
                startActivity(intent);
                finish();
            }
        });

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String  email, password;
                email = String.valueOf(textInputEditTextIDorEmail.getText());
                password = String.valueOf(textInputEditTextPassword.getText());

                if (!email.equals("") && !password.equals("")) {
                    //Start ProgressBar first (Set visibility VISIBLE)
                    progressBar.setVisibility(View.VISIBLE);
                    Handler handler = new Handler();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            //Starting Write and Read data with URL
                            //Creating array for parameters
                            String[] field = new String[2];
                            field[0] = "email";
                            field[1] = "password";
                            //Creating array for data
                            String[] data = new String[2];
                            data[0] = email;
                            data[1] = password;
                            PutData putData = new PutData("http://192.168.1.109/Hospital/login.php", "POST", field, data); //網址要改成自己的php檔位置及自己的ip
                            if (putData.startPut()) {
                                if (putData.onComplete()) {
                                    progressBar.setVisibility(View.GONE);
                                    String result = putData.getResult();
                                    if (result.equals("Login Success")) {
                                        Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
                                        verifiedsuccessfully();
                                    } else {
                                        Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
                                    }
                                }
                            }
                        }
                    });
                } else {
                    Toast.makeText(getApplicationContext(), "All fields require", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

//    private void checkDivicd() {
//        //假設是公務機 顯示近期登入人員
//
//        //個人裝置 顯示有指紋登入的頁面
//    }

    private void findView() {
        findViewById(R.id.touchidimageButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(LoginActivity.this,"使用指紋辨識",Toast.LENGTH_SHORT).show();
                checkrequirement();//檢查裝置是否支援指紋辨識
                startFingerprintListening();//開始掃描
            }
        });

        findViewById(R.id.faceidimageButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view,"目前不支援臉部辨識",Snackbar.LENGTH_LONG).show();
            }
        });

        //顯示或隱藏密碼
        cb = findViewById(R.id.checkBox2);
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // TODO Auto-generated method stub
                if (isChecked) {
                    //顯示密碼
                    textInputEditTextPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                } else {
                    //隱藏密碼
                    textInputEditTextPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());

                }
            }
        });
    }

    private void startFingerprintListening() {
        cancellationSignal = new CancellationSignal();
        if (checkSelfPermission(Manifest.permission.USE_FINGERPRINT) == PackageManager.PERMISSION_GRANTED) //In SDK 23, we need to check the permission before we call FingerprintManager API functionality.
        {
            mFingerprintManager.authenticate(
                    null, //crypto objects 的 wrapper class，可以透過它讓 authenticate 過程更為安全，但也可以不使用。
                    cancellationSignal, //用來取消 authenticate 的 object
                    0, //optional flags; should be 0
                    mAuthenticationCallback, //callback 用來接收 authenticate 成功與否，有三個 callback method
                    null); //optional 的參數，如果有使用，FingerprintManager 會透過它來傳遞訊息
        }
    }

    FingerprintManager.AuthenticationCallback mAuthenticationCallback = new FingerprintManager.AuthenticationCallback(){
        @Override
        public void onAuthenticationError(int errorCode, CharSequence errString) {
            Log.e("", "error 辨識錯誤" + errorCode + " " + errString);
            new AlertDialog.Builder(LoginActivity.this)
                    .setTitle("指紋辨識結果")
                    .setMessage("辨識錯誤")
                    .setPositiveButton("OK", null)
                    .show();
        }
        @Override
        public void onAuthenticationFailed() {
            Log.e("", "辨識失敗 onAuthenticationFailed");
            new AlertDialog.Builder(LoginActivity.this)
                    .setTitle("指紋辨識結果")
                    .setMessage("辨識失敗")
                    .setPositiveButton("OK", null)
                    .show();
        }
        @Override
        public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
            Log.i("", "辨識成功 onAuthenticationSucceeded");
            new AlertDialog.Builder(LoginActivity.this)
                    .setTitle("指紋辨識結果")
                    .setMessage("辨識成功")
                    .setPositiveButton("OK",null )
                    .show();
            verifiedsuccessfully();
        }
    };

    @Override
    public void onPause() {
        super.onPause();
//        cancellationSignal.cancel();
        cancellationSignal = null;
    }

    private void checkrequirement() {
        mKeyguardManager = (KeyguardManager)getSystemService(Activity.KEYGUARD_SERVICE);//是否有設定 screen lock
        mFingerprintManager = (FingerprintManager) getSystemService(Activity.FINGERPRINT_SERVICE);//FingerprintManager.class
        if (!mKeyguardManager.isKeyguardSecure()){
            Toast.makeText(this, "是否有設定 fingerprint screen lock", Toast.LENGTH_LONG).show();
            return;
        }
        if (checkSelfPermission(Manifest.permission.USE_FINGERPRINT) == PackageManager.PERMISSION_GRANTED) {
            //In SDK 23, we need to check the permission before we call FingerprintManager API functionality.
            if (!mFingerprintManager.isHardwareDetected()) {
                Toast.makeText(this, "硬體裝置是否支援 fingerprint reader", Toast.LENGTH_LONG).show();
                return;
            }
            if (!mFingerprintManager.hasEnrolledFingerprints()) {
                Toast.makeText(this, "是否有設定至少一枚指紋", Toast.LENGTH_LONG).show();
                return;
            }
        }
    }

    private static final int REQUEST_CODE_Sing = 101;
    public void signup (View view) {
        Intent signupit = new Intent(LoginActivity.this, SignupActivity.class);
        startActivityForResult(signupit,REQUEST_CODE_Sing);
    }

    public void getImei() {
        imei = findViewById(R.id.ed_imei);

        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(LoginActivity.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(LoginActivity.this, new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_CODE);
            return;
        }
        //IMEINumber = telephonyManager.getDeviceId();
        imei.setText(IMEINumber);
//        new AlertDialog.Builder(this)
//                .setTitle("手機識別碼讀取結果")
//                .setMessage("您的裝置編號為:"+IMEINumber)
//                .setPositiveButton("OK", null)
//                .show();
    }

    public static void handleSSLHandshake() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }};

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        } catch (Exception ignored) {
        }
    }

    public void verifiedsuccessfully(){
//        getIntent().putExtra("LOGIN_IMEI",imei.toString());
//        getIntent().putExtra("LOGIN_ID",eemail);
        setResult(RESULT_OK,getIntent());
        finish();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission granted.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


}