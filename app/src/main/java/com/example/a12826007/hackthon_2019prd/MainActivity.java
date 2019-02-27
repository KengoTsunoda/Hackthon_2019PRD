package com.example.a12826007.hackthon_2019prd;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        // カメラアプリとの連携からの戻りでかつ撮影成功の場合
        if (requestCode == 200 && resultCode == RESULT_OK){
            // 撮影された画像のビットマップデータを取得
            Bitmap bitmap = data.getParcelableExtra("data");
            new PostBmpAsyncHttpRequest(this).execute(bitmap);
        }
    }
    /**
     * 画像部分がタップされた時の処理メソッド
     */
    public void onCameraImageClick(View view){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            // WRITE_EXTERNAL_STORAGEの許可を求めるダイアログを表示
            // その際、リクエストコードに2000を設定
            String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
            ActivityCompat.requestPermissions(this, permissions, 2000);
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        Date now = new Date(System.currentTimeMillis());
        String nowStr = dateFormat.format(now);
        // ストレージに格納する画像のファイル名を生成。ファイル名の一位を確保するためにタイムスタンプの値を利用。
        String fileName = "UseCameraActivityPicture_" + nowStr + ".jpg";

        ContentValues contentValues = new ContentValues();
        // 画像のファイル名を設定
        contentValues.put(MediaStore.Images.Media.TITLE, fileName);
        // 画像ファイルの種類を設定
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        // ContentResolverを使ってURIオブジェクトを生成
        ContentResolver contentResolver = getContentResolver();
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        // カメラのIntentオブジェクトを生成
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Extra情報としてimageUriを設定
        // intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        // アクティビティを起動
        startActivityForResult(intent, 200);
    }

    /**
     * 今日の薬の画像部分がタップされた時の処理メソッド
     */
    public void onDrugImageClick(View view) {
        Intent intent = new Intent(MainActivity.this, TodaysDrug.class);
        startActivity(intent);
    }
    /**
     * カレンダーの画像部分がタップされた時の処理メソッド
     */
    public void onCalendarImageClick(View view) {
        Intent intent = new Intent(MainActivity.this, Calendar.class);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requsetCode, String[] permissions, int[] grantResults){
        // WRITE_EXTERNAL_STORAGEに対するパーミッションダイアログかつ許可を選択したなら…
        if (requsetCode == 2000 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            // もう一度カメラアプリを起動
            ImageView ivCamera = findViewById(R.id.ivCamera);
            onCameraImageClick(ivCamera);
        }
    }



    /**
     * 非同期で撮った画像処理をするクラス。
     */
    private class PostBmpAsyncHttpRequest extends AsyncTask<Bitmap, Void, String> {

        private Activity mActivity;

        public PostBmpAsyncHttpRequest(Activity activity) {
            mActivity = activity;
        }

        @Override
        protected String doInBackground(Bitmap... params) {

            Bitmap prescriptionImage = params[0];
            HttpURLConnection connection = null;
            StringBuilder sb = new StringBuilder();
            String uri = "http://20.43.90.37/api/analyser";
            try {
                // 画像をjpeg形式でstreamに保存
                ByteArrayOutputStream jpg = new ByteArrayOutputStream();
                prescriptionImage.compress(Bitmap.CompressFormat.JPEG, 100, jpg);

                URL url = new URL(uri);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(3000);//接続タイムアウトを設定する。
                connection.setReadTimeout(3000);//レスポンスデータ読み取りタイムアウトを設定する。
                connection.setRequestMethod("POST");//HTTPのメソッドをPOSTに設定する。
                //ヘッダーを設定する
                connection.setRequestProperty("User-Agent", "Android");
                connection.setRequestProperty("Content-Type","application/octet-stream");
                connection.setDoInput(true);//リクエストのボディ送信を許可する
                connection.setDoOutput(true);//レスポンスのボディ受信を許可する
                connection.setUseCaches(false);//キャッシュを使用しない
                connection.connect();

                // データを投げる
                OutputStream out = new BufferedOutputStream(connection.getOutputStream());
                out.write(jpg.toByteArray());
                out.flush();

                // データを受け取る
                InputStream is = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                String line = "";
                while ((line = reader.readLine()) != null)
                    sb.append(line);
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally{
                connection.disconnect();
            }
            //JSON文字列を返す。
            return uri;
        }

        @Override
        public void onPostExecute(String result) {
            //天気情報用文字列変数を用意。
            String telop = "";
            String desc = "";

            try {
                //JSON文字列からJSONObjectオブジェクトを生成。これをルートJSONオブジェクトとする。
                JSONObject rootJSON = new JSONObject(result);
                //ルートJSON直下の「description」JSONオブジェクトを取得。
                JSONObject descriptionJSON = rootJSON.getJSONObject("description");
                //「description」プロパティ直下の「text」文字列(天気概況文)を取得。
                desc = descriptionJSON.getString("text");
                //ルートJSON直下の「forecasts」JSON配列を取得。
                JSONArray forecasts = rootJSON.getJSONArray("forecasts");
                //「forecasts」JSON配列のひとつ目(インデックス0)のJSONオブジェクトを取得。
                JSONObject forecastNow = forecasts.getJSONObject(0);
                //「forecasts」ひとつ目のJSONオブジェクトから「telop」文字列(天気)を取得。
                telop = forecastNow.getString("telop");
            }
            catch(JSONException ex) {
            }
        }

        /**
         * InputStreamオブジェクトを文字列に変換するメソッド。変換文字コードはUTF-8。
         *
         * @param is 変換対象のInputStreamオブジェクト。
         * @return 変換された文字列。
         * @throws IOException 変換に失敗した時に発生。
         */
        private String is2String(InputStream is) throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuffer sb = new StringBuffer();
            char[] b = new char[1024];
            int line;
            while(0 <= (line = reader.read(b))) {
                sb.append(b, 0, line);
            }
            return sb.toString();
        }
    }
}
