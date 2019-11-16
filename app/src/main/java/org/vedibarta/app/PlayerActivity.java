package org.vedibarta.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class PlayerActivity extends Activity implements SeekBar.OnSeekBarChangeListener, View.OnClickListener {
    public static final String EXTRA_FILE_IN_DEVICE = "FILE_EXIST";
    public static final String EXTRA_LAUNCH = "launch";
    private TextView songCurrentDurationLabel;
    private TextView songTotalDurationLabel;
    private TextView songTitleLabel;
    private ImageButton play;

    private ServiceConnection serviceConnection;
    private Intent playingIntent;
    private MyApplication myApplication;
    private SeekBar songProgressBar;
    // Handler to update UI timer, progress bar etc,.
    private boolean fileExist;

    private SharedPreferences myPref;
    static NotificationManager mNotificationManager;
    private int currentParashPosition, currentTrack, numberOfTracks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((MyApplication) getApplication()).setPlayerActivity(this);
        setContentView(R.layout.player1);
        myPref = getPreferences(0);
        myApplication = (MyApplication) getApplication();
        playingIntent = new Intent(this, PlayingServiceNew.class);

        // bind to our service by first creating a new playingIntent
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName arg0, IBinder binder) {
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
            }

        };

        // Supply the Intent & ServiceConnection that will use for the binding
        bindService(playingIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        play = findViewById(R.id.btnPlay);
        ImageButton next = findViewById(R.id.btnNext);
        ImageButton btnForward = findViewById(R.id.btnForward);
        ImageButton btnBackward = findViewById(R.id.btnBackward);
        ImageButton previous = findViewById(R.id.btnPrevious);
        songProgressBar = findViewById(R.id.songProgressBar);
        songProgressBar.setProgress(0);
        songCurrentDurationLabel = findViewById(R.id.songCurrentDurationLabel);
        songTotalDurationLabel = findViewById(R.id.songTotalDurationLabel);
        songTitleLabel = findViewById(R.id.songTitle);
        // Listeners
        songProgressBar.setOnSeekBarChangeListener(this); // Important

        play.setOnClickListener(this);
        next.setOnClickListener(this);
        previous.setOnClickListener(this);
        btnForward.setOnClickListener(this);
        btnBackward.setOnClickListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        currentParashPosition = myApplication.getCurrentParashaPosition();
        numberOfTracks = myApplication.getParahsot().get(currentParashPosition).totalTracks;
        if (mNotificationManager != null)
            mNotificationManager.cancel(1);
        // for update the file about the last data of playing state
        // we trying to play from last point for user
        fileExist = getIntent().getBooleanExtra(EXTRA_FILE_IN_DEVICE, false);
        playingIntent.putExtra(PlayingServiceNew.EXTRA_COMMAND, PlayingServiceNew.START_PLAY);
        playingIntent.putExtra(PlayingServiceNew.EXTRA_PAR_POSITION, currentParashPosition);
        playingIntent.putExtra(PlayingServiceNew.EXTRA_CURRENT_TRACK, 0);
        playingIntent.putExtra(PlayingServiceNew.EXTRA_TOTAL_TRACKS, numberOfTracks);
        if (PlayingServiceNew.playing) {
            if (getIntent().getBooleanExtra(EXTRA_LAUNCH, false)) {
                if (fileExist) {
                }
                startService(playingIntent);
                loadClip();
            }
        } else {
            startService(playingIntent);
            loadClip();
        }
        setIntent(getIntent().putExtra("launch", false));

    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.getBooleanExtra("launch", false))
            setIntent(intent);
    }

    public void onStop() {
        super.onStop();
        if (fileExist) {

        } else {
            SharedPreferences.Editor editor = myPref.edit();
            editor.putLong("CURRENT", PlayingServiceNew.currentDuration);
            editor.putInt("TRACK", currentTrack);
            // Commit the edits!
            editor.commit();
        }

        // add notification if playing
        if (PlayingServiceNew.playing) {
            Intent resultIntent = new Intent(getApplicationContext(),
                    PlayerActivity.class);
            resultIntent.setAction(Intent.ACTION_MAIN);
            resultIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            PendingIntent resultPendingIntent = PendingIntent.getActivity(
                    getApplicationContext(), 0, resultIntent, 0);
            NotificationHelper.postNotification(getApplicationContext(), 1,
                    R.drawable.ic_menu_play_clip,
                    getResources().getString(R.string.app_name),
                    getResources().getString(R.string.playing_parashat) + " " + myApplication.getParahsot().get(currentParashPosition).label,
                    resultPendingIntent,
                    true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ((MyApplication) getApplication()).setPlayerActivity(null);
        // whenever our activity gets destroyed, unbind from the service
        unbindService(this.serviceConnection);
        songProgressBar.removeCallbacks(updatePlayerTime);
    }

    @SuppressLint("SetTextI18n")
    private void loadClip() {
        // set Progress bar values
        songProgressBar.setProgress(0);
        songTitleLabel.setText(myApplication.getParahsot().get(currentParashPosition).label + " " + (currentTrack + 1) + "/" +
                myApplication.getParahsot().get(currentParashPosition).totalTracks);
    }

    @Override
    public void onBackPressed() {
        // super.onBackPressed();

        if (!PlayingServiceNew.playing) {
            if (fileExist) {

            } else {
                playingIntent.putExtra(PlayingServiceNew.EXTRA_COMMAND, 2);
                startService(playingIntent);
            }
            finish();
        }

        Intent backIntent = new Intent(this, VedibartaActivity.class);
        backIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        backIntent.putExtra("playing", PlayingServiceNew.playing);
        startActivity(backIntent);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        progressIsDragging = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        int currentPosition = Utilities.progressToTimer(seekBar.getProgress(), PlayingServiceNew.totalDuration);
        playingIntent.putExtra("MOVE_TO", currentPosition);
        playingIntent.putExtra("ABS_VALUE", true);
        playingIntent.putExtra(PlayingServiceNew.EXTRA_COMMAND, PlayingServiceNew.SEEK_TO);
        startService(playingIntent);
        progressIsDragging = false;
    }

    private void clickEvent(int add) {
        currentTrack = currentTrack + add;
        if (currentTrack < 0) { // previus pressed when playing first track
            currentTrack = 0;
            playingIntent.putExtra("MOVE_TO", 0);
            playingIntent.putExtra("ABS_VALUE", true);
            playingIntent.putExtra(PlayingServiceNew.EXTRA_COMMAND, PlayingServiceNew.SEEK_TO);
            startService(playingIntent);
        } else if (!(currentTrack < numberOfTracks)) { // next pressed when playing last track
            currentTrack = numberOfTracks - 1;
        } else {
            if (!fileExist)
                Toast.makeText(this, getResources().getString(R.string.begin_playing), Toast.LENGTH_SHORT).show();
            playingIntent.putExtra(PlayingServiceNew.EXTRA_COMMAND, PlayingServiceNew.START_PLAY);
            playingIntent.putExtra(PlayingServiceNew.EXTRA_CURRENT_TRACK, currentTrack);
            startService(playingIntent);
            loadClip();
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnPlay:
                if (PlayingServiceNew.playing)
                    play.setImageResource(R.drawable.btn_play);
                else
                    play.setImageResource(R.drawable.btn_pause);
                playingIntent.putExtra(PlayingServiceNew.EXTRA_COMMAND, PlayingServiceNew.PLAY_PRESSED);
                startService(playingIntent);
                break;
            case R.id.btnNext:
                clickEvent(1);
                break;
            case R.id.btnPrevious:
                clickEvent(-1);
                break;
            case R.id.btnForward:
                playingIntent.putExtra("MOVE_TO", 10000);
                playingIntent.putExtra("ABS_VALUE", false);
                playingIntent.putExtra(PlayingServiceNew.EXTRA_COMMAND, PlayingServiceNew.SEEK_TO);
                startService(playingIntent);
                break;
            case R.id.btnBackward:
                playingIntent.putExtra("MOVE_TO", -10000);
                playingIntent.putExtra("ABS_VALUE", false);
                playingIntent.putExtra(PlayingServiceNew.EXTRA_COMMAND, PlayingServiceNew.SEEK_TO);
                startService(playingIntent);
                break;
        }
    }

    /**
     * Update player data
     *
     * @param chapterChanged if <b>true</b>, update the new playing chapter, else update player time
     */
    public void updatePlayerInUIThread(boolean chapterChanged) {
        runOnUiThread(updatePlayerTime);
        if (chapterChanged) {
            runOnUiThread(updatePlayerChapter);
        }
    }

    Runnable updatePlayerChapter = new Runnable() {
        @SuppressLint("SetTextI18n")
        @Override
        public void run() {
            play.setImageResource(R.drawable.btn_pause);
            songTitleLabel.setText(myApplication.getParahsot().get(currentParashPosition).label + " " + (currentTrack + 1)
                    + "/" + myApplication.getParahsot().get(currentParashPosition).totalTracks);
            songTotalDurationLabel.setText("" + Utilities.milliSecondsToTimer((long) PlayingServiceNew.totalDuration));
        }
    };

    private boolean progressIsDragging;
    Runnable updatePlayerTime = new Runnable() {
        @SuppressLint("SetTextI18n")
        @Override
        public void run() {
            if (!progressIsDragging) {
                songCurrentDurationLabel.setText("" + Utilities.milliSecondsToTimer(PlayingServiceNew.currentDuration));
                // Updating progress bar
                int progress = (Utilities.getProgressPercentage(PlayingServiceNew.currentDuration, (long) PlayingServiceNew.totalDuration));
                songProgressBar.setProgress(progress);
            }
            songProgressBar.postDelayed(this, 1000);
        }
    };

}
