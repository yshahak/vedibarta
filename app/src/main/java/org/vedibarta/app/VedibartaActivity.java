package org.vedibarta.app;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBar.TabListener;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;

import static org.vedibarta.app.DownloadService.EXTRA_INDEX;
import static org.vedibarta.app.DownloadService.EXTRA_PAR_TITLE;
import static org.vedibarta.app.DownloadService.EXTRA_POSITION;
import static org.vedibarta.app.DownloadService.EXTRA_RECEIVER;
import static org.vedibarta.app.DownloadService.EXTRA_TOTAL_TRACKS;
import static org.vedibarta.app.DownloadService.RESULT_CODE_FINISH;
import static org.vedibarta.app.DownloadService.RESULT_CODE_PROGRESS;
import static org.vedibarta.app.DownloadService.RESULT_CODE_START_DOWNLOAD;
import static org.vedibarta.app.NotificationHelper.CHANNEL_ID;
import static org.vedibarta.app.PlayerActivity.EXTRA_LAUNCH;


public class VedibartaActivity extends AppCompatActivity implements OnStartPlayClicked {

    NotificationManagerCompat mNotifyManager;
    NotificationCompat.Builder notificationBuilder;
    private ProgressDialog mProgress;
    private boolean hideProgressWindow;
    private ArrayList<String> names = new ArrayList<>();
    private WeakReference<ResultReceiver> downloadReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        // setup action bar for tabs
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayShowTitleEnabled(false);

        Tab tab1 = actionBar
                .newTab()
                .setText(R.string.parashot)
                .setTabListener(
                        new MyTabListener<>(this, "parashot",
                                FragmentParashot.class));
        actionBar.addTab(tab1);
        Tab tab2 = actionBar
                .newTab()
                .setText(R.string.downloads)
                .setTabListener(
                        new MyTabListener<>(this, "horadot",
                                FragmentHoradot.class));
        actionBar.addTab(tab2);
        Tab tab3 = actionBar
                .newTab()
                .setText(R.string.feedback)
                .setTabListener(
                        new MyTabListener<>(this, "feedback",
                                Fragmentfeedback.class));
        actionBar.addTab(tab3);
        if (Utilities.isNetworkAvailable(this)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    downloadHtmlFiles();
                }
            }).start();
        }
        mNotifyManager = NotificationManagerCompat.from(getApplicationContext());
        final PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), 0);
        notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentIntent(pendingIntent);
        mProgress = new ProgressDialog(this);
        mProgress.setCancelable(true);
        mProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgress.setButton(DialogInterface.BUTTON_NEGATIVE,
                getResources().getString(R.string.hideWindow),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        hideProgressWindow = true;
                        mProgress.setProgress(0);
                        dialog.dismiss();
                    }
                });
        ResultReceiver resultReceiver = new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                onNewDownloadUpdate(resultCode, resultData);
            }
        };
        downloadReceiver = new WeakReference<>(resultReceiver);
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);

    }

    public void startDownload(String item, int position) {
        if (names.contains(item)) {
            return;
        }
        if (names.size() == 0) {
            Toast.makeText(this, R.string.beginDownloading, Toast.LENGTH_LONG)
                    .show();
        } else {
            Toast.makeText(this,
                    getResources().getString(R.string.joinToDownloadList),
                    Toast.LENGTH_LONG).show();
        }
        names.add(item);
        Intent intent = new Intent(this, DownloadService.class);
        intent.putExtra(EXTRA_PAR_TITLE, item);
        intent.putExtra(EXTRA_POSITION, position);
        intent.putExtra(EXTRA_RECEIVER, downloadReceiver.get());
        startService(intent);
    }

    @Override
    public void onStartPlayClicked(int parPosition) {
        ((MyApplication) getApplication()).setCurrentParashaPosition(parPosition);
        Toast.makeText(this, getResources().getString(R.string.begin_playing), Toast.LENGTH_SHORT).show();
        final Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(EXTRA_LAUNCH, true);
        startActivity(intent);
    }

    private void onNewDownloadUpdate(int resultCode, Bundle resultData) {
        if (isFinishing()) {
            return;
        }
        String parasha = resultData.getString(EXTRA_PAR_TITLE);
        switch (resultCode) {
            case RESULT_CODE_START_DOWNLOAD:
                break;
            case RESULT_CODE_PROGRESS:
                int numberOfTracks = resultData.getInt(EXTRA_TOTAL_TRACKS);
                int i = resultData.getInt(EXTRA_INDEX);
                notificationBuilder.setProgress(numberOfTracks, i + 1, false)
                        .setContentText(
                        (getResources().getString(
                                R.string.theParashaIsDownloading)
                                + " " + parasha))
                        .setSmallIcon(R.drawable.stat_sys_download_anim1);
                mNotifyManager.notify(0, notificationBuilder.build());
                if(!hideProgressWindow){
                    mProgress.setMessage(getResources().getString(
                            R.string.DownloadingPart)
                            + " "
                            + (i + 1)
                            + "/"
                            + numberOfTracks
                            + " "
                            + getResources().getString(R.string.from)
                            + " "
                            + "'"
                            + parasha + "'");
                    mProgress.setMax(numberOfTracks);
                    mProgress.setProgress(i);
                    mProgress.show();
                }

                break;
            case RESULT_CODE_FINISH:
                Toast.makeText(getApplicationContext(), R.string.download_complete, Toast.LENGTH_LONG).show();
                notificationBuilder.setContentText(
                        getResources().getString(R.string.download_complete))
                        .setProgress(0, 0, false)
                        .setAutoCancel(true);
                mNotifyManager.notify(0, notificationBuilder.build());
                names.remove(parasha);
                if (names.size() == 0) {
                    if (mNotifyManager != null)
                        mNotifyManager.cancel(0);
                }
//                if (hideProgressWindow){
                    mProgress.setProgress(0);
                    mProgress.dismiss();
//                }
                break;
        }
    }


    //	@AfterPermissionGranted(RC_CAMERA_AND_LOCATION)
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
            //hot fix for the issue with downloading nginx response instead of real response
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("upgraded", true).apply();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class MyTabListener<T extends Fragment> implements TabListener {
        private Fragment mFragment;
        private final Activity mActivity;
        private final String mTag;
        private final Class<T> mClass;

        MyTabListener(Activity activity, String tag, Class<T> clz) {
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
