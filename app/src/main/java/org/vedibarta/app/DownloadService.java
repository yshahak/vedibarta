package org.vedibarta.app;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

//import com.splunk.mint.Mint;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadService extends IntentService {

	private final static String TAG = "'DownloadService'";

	String[] data;
	String AudioFiles = "AudioFiles";
	public static final int UPDATE_PROGRESS = 8344;

	public DownloadService() {
		super("DownloadService");

	}

	@Override
	public void onCreate() {
		super.onCreate();

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		ResultReceiver receiver = (ResultReceiver) intent
				.getParcelableExtra("receiver");
		int count;
		ParashotData par = new ParashotData();
		double memory = (getFilesDir().getFreeSpace()) / 1048576;
		int position = (int) intent.getIntExtra("POSITION", 0);
		File dir = null;
		File myFile = null;
		boolean internal = (memory > 300);
		if (internal)
			dir = new File(getFilesDir() + File.separator + AudioFiles + File.separator
					+ Integer.toString(position));
		else
			dir = new File(getExternalFilesDir(null) + File.separator + AudioFiles + File.separator
					+ Integer.toString(position));
		dir.mkdirs();
		int numberOfTracks = (int) par.getTracksNumber(position);
        Bundle resultData = new Bundle();
		resultData.putInt("tracks" ,(int) (numberOfTracks));
		resultData.putInt("i" ,(int) (0));
		receiver.send(UPDATE_PROGRESS, resultData);
		OkHttpClient.Builder builder = new OkHttpClient.Builder();
		builder.hostnameVerifier(new HostnameVerifier() {
			@Override
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		});
		OkHttpClient client = builder.build();


		for (int i = 0; i < numberOfTracks; i++) {
			String path = ParashotData.getPath(position, i)[1];
			try {
				Request request = new Request.Builder().url(path).build();
				Response response = client.newCall(request).execute();
				if (response.isSuccessful() && response.body() != null) {
					String fileName = ParashotData.getPath(position, i)[0];
					myFile = new File(dir, fileName);
					FileOutputStream fos = new FileOutputStream(myFile);
					fos.write(response.body().bytes());
					fos.close();
					resultData.putInt("i" ,(int) (i + 1));
					resultData.putString("POSITION", Integer.toString(position));
					resultData.putString("PATH", dir.getPath());
					receiver.send(UPDATE_PROGRESS, resultData);
				} else {
					Log.e(TAG, "there was an issue with the download:" + path );
				}

//				String fileName = par.getPath(position, i)[0];
//				myFile = new File(dir, fileName);
//
//				URL myUrl = new URL(path);
//				URLConnection conexion = myUrl.openConnection();
//				conexion.connect();
//
//				// this will be useful so that you can show a typical 0-100%
//				// progress bar
//				InputStream input = new BufferedInputStream(myUrl.openStream());
//				OutputStream output;
//				output = new FileOutputStream(myFile);
//				byte data[] = new byte[1024];
//				long total = 0;
//				while ((count = input.read(data)) != -1) {
//					total += count;
//					//resultData.putInt("progress" ,(int) (total * 100/ fileLength));
//	               // receiver.send(UPDATE_PROGRESS, resultData);
//					output.write(data, 0, count);
//				}

				
				
//				output.flush();
//				output.close();
//				input.close();
			} catch (IOException e) {
				e.printStackTrace();
				Log.e(TAG, "there was an issue with the download:" + path );
				break;
//                Mint.logException(e);
			}
			
		}
		

	}
}
