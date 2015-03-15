package org.vedibarta.app;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBar.TabListener;
import android.support.v7.app.ActionBarActivity;
import android.widget.Toast;

import com.splunk.mint.Mint;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;


public class VedibartaActivity extends ActionBarActivity {
	static ActionBar actionBar;
	public ProgressDialog mProgress;
	Utilities util;
	Context ctx;
	int size = 0;
	boolean hide;
	boolean downloading;
	public static boolean firstEntry;
	String parasha;
	ArrayList<String> names;
	static ArrayList<String> logList = new ArrayList<String>();

	NotificationManager mNotifyManager;
	Builder mBuilder;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        Mint.initAndStartSession(this, "0ea8e3c0");
		firstEntry = true;
		super.onCreate(savedInstanceState);
		util = new Utilities();
		parasha = null;
		hide = false;
		downloading = false;
		names = new ArrayList<String>();
		ctx = this;

		actionBar = getSupportActionBar();
		// setup action bar for tabs
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.setDisplayShowTitleEnabled(false);

		Tab tab1 = actionBar
				.newTab()
				.setText(R.string.parashot)
				.setTabListener(
						new MyTabListener<List_View1>(this, "parashot",
								List_View1.class));
		actionBar.addTab(tab1);
		Tab tab2 = actionBar
				.newTab()
				.setText(R.string.downloads)
				.setTabListener(
						new MyTabListener<List_View2>(this, "horadot",
								List_View2.class));
		actionBar.addTab(tab2);
		Tab tab3 = actionBar
				.newTab()
				.setText(R.string.feedback)
				.setTabListener(
						new MyTabListener<Feedback2>(this, "feedback",
								Feedback2.class));
		actionBar.addTab(tab3);

	}

	@Override
	public void onResume() {
		super.onResume();

		// Updating HTML files of the app
		if (util.isNetworkAvailable(this)) {
			Thread myThread = new Thread(new Runnable() {
				@Override
				public void run() {
					downloadHtmlFiles();
				}
			});
			myThread.start();
		}


	}

	@Override
	public void onRestart() {
		super.onRestart();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		logList.clear();
	}

	@Override
	public void onBackPressed() {
		moveTaskToBack(true);

	}


	public ArrayList<String> getList() {
		return logList;
	}

	public void updateDownload(String item, int pstn, long memory) {

		mProgress = new ProgressDialog(this);
		mProgress.setCancelable(true);

		mProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mProgress.setProgress(0);
		if (!downloading) {
			downloading = true;
			Toast.makeText(this, R.string.beginDownloading, Toast.LENGTH_LONG)
					.show();
			mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			mBuilder = new NotificationCompat.Builder(this);
			mBuilder.setContentTitle(
					getResources().getString(R.string.app_name))
					.setContentText(
							getResources().getString(
									R.string.theParashaIsDownloading)
									+ "  " + item)
					.setSmallIcon(R.drawable.stat_sys_download_anim1)
					.setContentIntent(
							PendingIntent.getActivity(this, 0, new Intent(), 0));
		} else {
			Toast.makeText(this,
					getResources().getString(R.string.joinToDownloadList),
					Toast.LENGTH_LONG).show();
		}
		if (parasha == null)
			parasha = item;
		else
			names.add(item);
		size = names.size();
		Intent intent = new Intent(this, DownloadService.class);
		intent.putExtra("PARASHA", item);
		intent.putExtra("POSITION", pstn);
		intent.putExtra("MEMORY", memory);
		intent.putExtra("receiver", new DownloadReceiver(new Handler()));
		startService(intent);
	}

	private class DownloadReceiver extends ResultReceiver {
		public DownloadReceiver(Handler handler) {
			super(handler);
		}

		@Override
		protected void onReceiveResult(int resultCode, Bundle resultData) {
			super.onReceiveResult(resultCode, resultData);
			if (resultCode == DownloadService.UPDATE_PROGRESS) {

				int tracks = resultData.getInt("tracks");
				int i = resultData.getInt("i");
				if (parasha == null) {
					parasha = names.get(0);
					names.remove(0);
					size = names.size();
				}
				mBuilder.setProgress(tracks, i, false).setContentText(
						(getResources().getString(
								R.string.theParashaIsDownloading)
								+ " " + parasha));
				mNotifyManager.notify(0, mBuilder.build());

				mProgress.setMessage(getResources().getString(
						R.string.DownloadingPart)
						+ " "
						+ (i + 1)
						+ "/"
						+ tracks
						+ " "
						+ getResources().getString(R.string.from)
						+ " "
						+ "'"
						+ parasha + "'");
				mProgress.setButton(DialogInterface.BUTTON_NEGATIVE,
						getResources().getString(R.string.hideWindow),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								hide = true;
								dialog.dismiss();
							}
						});
				mProgress.setMax(tracks);
				if (!hide)
					mProgress.show();
				mProgress.setProgress(i);
				// This is the end of the download
				if (i == tracks) {
					mProgress.dismiss();
					if (hide) {
						Toast.makeText(ctx, R.string.download_complete,
								Toast.LENGTH_LONG).show();
						if (size == 0)
							hide = false;
					}
					mBuilder.setContentText(
							getResources()
									.getString(R.string.download_complete))
							.setProgress(0, 0, false).setAutoCancel(true);
					mNotifyManager.notify(0, mBuilder.build());
					downloading = false;
					util.writeToFile(resultData.getString("POSITION"), true,
							ctx);
					util.writeToFile((String) resultData.getString("PATH")
							+ ";" + "1" + ";" + "0", false, ctx);
					VedibartaActivity.logList.add("mainActivity");
					VedibartaActivity.logList.add(parasha + " "
                            + " נוספה לקובץ טקסט");
					parasha = null;
					if (size == 0) {
						if (mNotifyManager != null)
							mNotifyManager.cancel(0);
					}

				}
			}
		}
	}

	protected void downloadHtmlFiles() {
		try {
			int count;
			File dir = new File(getFilesDir() + File.separator + "HtmlFiles");
			dir.mkdirs();
			String fileName = "splash.html";
			File myFile = new File(dir, fileName);
			URL myUrl = new URL(
					"http://www.vedibarta.org/androidApp/splash.html");
			InputStream input = new BufferedInputStream(myUrl.openStream());
			OutputStream output;
			output = new FileOutputStream(myFile);
			byte data[] = new byte[1024];
			while ((count = input.read(data)) != -1) {
				output.write(data, 0, count);
			}
			output.flush();
			output.close();
			input.close();
			if (myFile.length() > 100) {
				VedibartaActivity.logList.add("main");
				VedibartaActivity.logList.add("�����  " + myFile.getPath());
			}

		} catch (IOException e) {
            Mint.logException(e);
		}
	}

	public static class MyTabListener<T extends Fragment> implements
			TabListener {
		private Fragment mFragment;
		private final Activity mActivity;
		private final String mTag;
		private final Class<T> mClass;

		public MyTabListener(Activity activity, String tag, Class<T> clz) {
			mActivity = activity;
			mTag = tag;
			mClass = clz;
		}

		/* The following are each of the ActionBar.TabListener callbacks */

		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			// Check if the fragment is already initialized
			if (mFragment == null) {
				// If not, instantiate and add it to the activity
				mFragment = Fragment.instantiate(mActivity, mClass.getName());
				ft.add(android.R.id.content, mFragment, mTag);
			} else {
				// If it exists, simply attach it in order to show it
				ft.attach(mFragment);
			}
		}

		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			if (mFragment != null) {
				// Detach the fragment, because another one is being attached
				ft.detach(mFragment);
			}
		}

		public void onTabReselected(Tab tab, FragmentTransaction ft) {
			// User selected the already selected tab. Usually do nothing.
		}

	}

}
