package jp.shuri.pushshogis;

import android.Manifest;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private String mUrlString;
    private Context context;

    private final int REQUEST_PERMISSION = 128;

    private Intent mIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        context = this;
        mIntent = getIntent();

        if(Build.VERSION.SDK_INT >= 23){
            checkPermission();
        }
        else{
            getKifData();
        }
    }

    private void getKifData()
    {
        if (mIntent.getClipData() != null) {
            ClipData cd = mIntent.getClipData();
            Uri uri = cd.getItemAt(0).getUri();

            mUrlString = getKifText(uri);

            postKifData();
        }
    }

    private void postKifData()
    {
        RequestBody formBody = new FormBody.Builder()
                .add("kif", mUrlString)
                .build();

        Request request = new Request.Builder()
                .url("http://shogi-s.com/upload-text")
                .post(formBody)
                .build();

        OkHttpClient client = new OkHttpClient();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 200) {
                    // show result
                    HttpUrl url = response.request().url();
                    String urlStr = url.toString();
                    Uri uri = Uri.parse(urlStr);
                    Intent i = new Intent(Intent.ACTION_VIEW, uri);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                    finish();
                } else {
                    finish();
                    Toast.makeText(getApplicationContext(), "通信異常 HTTP Status : " + response.code(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // i will check the authority
    public void checkPermission() {
        // already allowed
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED){
            getKifData();
        }
        // when refusing
        else{
            requestLocationPermission();
        }
    }

    // ask for permission
    private void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);

        } else {
            Toast toast = Toast.makeText(this, "許可されないとアプリが実行できません", Toast.LENGTH_SHORT);
            toast.show();

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,}, REQUEST_PERMISSION);

        }
    }

    // receipt of results
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION) {
            // approved
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getKifData();
                return;

            } else {
                // rejected
                Toast toast = Toast.makeText(this, "これ以上なにもできません", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }

    private String getKifText(Uri uri)
    {
        File f = new File(uri.getPath());
        InputStream inputStream = null;
        InputStreamReader reader = null;
        BufferedReader br = null;
        try {
            inputStream = context.getContentResolver().openInputStream(uri);
            reader = new InputStreamReader(inputStream, "sjis");
            br = new BufferedReader(reader);
        } catch (Exception e) {
            e.printStackTrace();
        }

        int i = -1;
        int DEFAULT_BUFFER_SIZE = 1024 * 4;
        char[] buffer = new char[DEFAULT_BUFFER_SIZE];
        StringBuilder sb = new StringBuilder();

        try {
            String line = br.readLine();

            while (line != null) {
                line += "\r\n";
                sb.append(line);
                line = br.readLine();
            }
            br.close();
            reader.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }
}
