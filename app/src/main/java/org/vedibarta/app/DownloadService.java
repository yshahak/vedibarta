package org.vedibarta.app;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

//import com.splunk.mint.Mint;

public class DownloadService extends IntentService {

    private final static String TAG = "'DownloadService'";
    public static final int RESULT_CODE_START_DOWNLOAD = 3000;
    public static final int RESULT_CODE_PROGRESS = 8344;
    public static final int RESULT_CODE_FINISH = 9000;
    public static final int RESULT_CODE_ERROR = -10;

    public static final String EXTRA_PAR_TITLE = "PARASHA";
    public static final String EXTRA_POSITION = "POSITION";
    public static final String EXTRA_RECEIVER = "receiver";
    public static final String EXTRA_INDEX = "i";
    public static final String EXTRA_TOTAL_TRACKS = "tracks";

    public DownloadService() {
        super("DownloadService");

    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String title = intent.getStringExtra(EXTRA_PAR_TITLE);
        ResultReceiver receiver = intent.getParcelableExtra(EXTRA_RECEIVER);
        int parPositionInArray = intent.getIntExtra(EXTRA_POSITION, 0);
        double memory = (getFilesDir().getFreeSpace()) / 1048576;
        File dir;
        boolean internal = (memory > 300);
        String audioFiles = "AudioFiles";
        if (internal)
            dir = new File(getFilesDir() + File.separator + audioFiles);
        else
            dir = new File(getExternalFilesDir(null) + File.separator + audioFiles);
        dir.mkdirs();
        int numberOfTracks = ParashotData.getTracksNumber(parPositionInArray);
        Bundle resultData = new Bundle();
        int downloadCounter = 0;
        resultData.putInt(EXTRA_TOTAL_TRACKS, numberOfTracks);
        resultData.putInt(EXTRA_INDEX, downloadCounter);
        resultData.putString(EXTRA_PAR_TITLE, title);
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.hostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });
        OkHttpClient client = builder.build();
        if (receiver != null) {
            receiver.send(RESULT_CODE_START_DOWNLOAD, resultData);
        }

        for (int i = 0; i < numberOfTracks; i++) {
            resultData.putInt(EXTRA_INDEX, downloadCounter);
            if (receiver != null) {
                receiver.send(RESULT_CODE_PROGRESS, resultData);
            }
            String path = ParashotData.getPath(parPositionInArray, i)[1];
            try {
                Request request = new Request.Builder().url(path).build();
                Response response = client.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
//                    String fileName = ParashotData.getPath(parPositionInArray, i)[0];
                    String fileName = ParashotData.getRelativePath(parPositionInArray, i);
                    File myFile = new File(dir, fileName);
                    if (!myFile.getParentFile().exists()){
                        myFile.getParentFile().mkdirs();
                    }
                    FileOutputStream fos = new FileOutputStream(myFile);
                    fos.write(response.body().bytes());
                    fos.close();
                    downloadCounter++;
                } else {
                    Log.e(TAG, "there was an issue with the downloadBtn:" + path);
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "there was an issue with the downloadBtn:" + path);
                break;
//                Mint.logException(e);
            }

        }
        if (downloadCounter == numberOfTracks) {
            //finished successfully!
            Utilities.writeToFile(Integer.toString(parPositionInArray), true, getApplication());
            Utilities.writeToFile(dir.getPath() + ";" + "1" + ";" + "0", false, getApplication());
            if (receiver != null) {
                receiver.send(RESULT_CODE_FINISH, resultData);
            }
        } else {
            if (receiver != null) {
                resultData.putString("error", "there was an issue with the download");
                receiver.send(RESULT_CODE_ERROR, resultData);
            }
        }


    }
}
