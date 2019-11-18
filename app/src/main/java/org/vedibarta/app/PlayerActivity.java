package org.vedibarta.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

public class PlayerActivity extends Activity implements SeekBar.OnSeekBarChangeListener, View.OnClickListener {

    public static final String EXTRA_LAUNCH = "launch";
    private TextView songCurrentDurationLabel;
    private TextView songTotalDurationLabel;
    private TextView songTitleLabel;
    private ImageButton play;

    private ServiceConnection serviceConnection;
    private Intent playingIntent;
    private MyApplication myApplication;
    private SeekBar songProgressBar;
    private boolean progressIsDragging;

    static NotificationManager mNotificationManager;
//    private int currentParashPosition, currentTrack, numberOfTracks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player1);
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
        int currentParashPosition = myApplication.getCurrentParashaPosition();
        int numberOfTracks = myApplication.getParahsot().get(currentParashPosition).totalTracks;
        if (mNotificationManager != null)
            mNotificationManager.cancel(1);
        // for update the file about the last data of playing state
        // we trying to play from last point for user
        playingIntent.putExtra(PlayingServiceNew.EXTRA_COMMAND, PlayingServiceNew.START_PLAY);
        playingIntent.putExtra(PlayingServiceNew.EXTRA_PAR_POSITION, currentParashPosition);
        playingIntent.putExtra(PlayingServiceNew.EXTRA_CURRENT_TRACK, getIntent().getIntExtra(PlayingServiceNew.EXTRA_CURRENT_TRACK, 0));
        playingIntent.putExtra(PlayingServiceNew.EXTRA_TOTAL_TRACKS, numberOfTracks);
        if (isPlaying()) {
            if (getIntent().getBooleanExtra(EXTRA_LAUNCH, false)) {
                startService(playingIntent);
//                loadClip();
            }
        } else {
            startService(playingIntent);
//            loadClip();
        }
        setIntent(getIntent().putExtra(EXTRA_LAUNCH, false));
        runOnUiThread(updatePlayerState);

    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.getBooleanExtra(EXTRA_LAUNCH, false))
            setIntent(intent);
    }

    public void onStop() {
        super.onStop();
        play.removeCallbacks(updatePlayerState);
        // add notification if playing
        if (isPlaying()) {
            Intent resultIntent = new Intent(getApplicationContext(),
                    PlayerActivity.class);
            resultIntent.setAction(Intent.ACTION_MAIN);
            resultIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            PendingIntent resultPendingIntent = PendingIntent.getActivity(
                    getApplicationContext(), 0, resultIntent, 0);
            NotificationHelper.postNotification(getApplicationContext(), 1,
                    R.drawable.ic_menu_play_clip,
                    getResources().getString(R.string.app_name),
                    getResources().getString(R.string.playing_parashat) + " " + myApplication.getParahsot().get(myApplication.getPlayingSession().currentParashPosition).label,
                    resultPendingIntent,
                    true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // whenever our activity gets destroyed, unbind from the service
        unbindService(this.serviceConnection);
//        songProgressBar.removeCallbacks(updatePlayerTime);
    }

//    @SuppressLint("SetTextI18n")
//    private void loadClip() {
//        // set Progress bar values
//        songProgressBar.setProgress(0);
//        songTitleLabel.setText(myApplication.getParahsot().get(myApplication.getPlayingSession().currentParashPosition).label + " " + (myApplication.getPlayingSession().currentTrack + 1) + "/" +
//                myApplication.getParahsot().get(myApplication.getPlayingSession().currentParashPosition).totalTracks);
//    }

    @Override
    public void onBackPressed() {
        // super.onBackPressed();

        if (!isPlaying()) {
//            playingIntent.putExtra(PlayingServiceNew.EXTRA_COMMAND, 2);
//            startService(playingIntent);
            finish();
        }

        Intent backIntent = new Intent(this, VedibartaActivity.class);
        backIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        backIntent.putExtra("playing", isPlaying());
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
        int currentPosition = Utilities.progressToTimer(seekBar.getProgress(), myApplication.getPlayingSession().totalDuration);
        playingIntent.putExtra("MOVE_TO", currentPosition);
        playingIntent.putExtra("ABS_VALUE", true);
        playingIntent.putExtra(PlayingServiceNew.EXTRA_COMMAND, PlayingServiceNew.SEEK_TO);
        startService(playingIntent);
        progressIsDragging = false;
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnPlay:
                if (isPlaying())
                    play.setImageResource(R.drawable.btn_play);
                else
                    play.setImageResource(R.drawable.btn_pause);
                playingIntent.putExtra(PlayingServiceNew.EXTRA_COMMAND, PlayingServiceNew.PLAY_PRESSED);
                break;
            case R.id.btnNext:
                playingIntent.putExtra(PlayingServiceNew.EXTRA_COMMAND, PlayingServiceNew.NEXT_PRESSED);
                break;
            case R.id.btnPrevious:
                playingIntent.putExtra(PlayingServiceNew.EXTRA_COMMAND, PlayingServiceNew.PREVIOUS_PRESSED);
                break;
            case R.id.btnForward:
                playingIntent.putExtra("MOVE_TO", 10000);
                playingIntent.putExtra("ABS_VALUE", false);
                playingIntent.putExtra(PlayingServiceNew.EXTRA_COMMAND, PlayingServiceNew.SEEK_TO);
                break;
            case R.id.btnBackward:
                playingIntent.putExtra("MOVE_TO", -10000);
                playingIntent.putExtra("ABS_VALUE", false);
                playingIntent.putExtra(PlayingServiceNew.EXTRA_COMMAND, PlayingServiceNew.SEEK_TO);
                break;
        }
        startService(playingIntent);
    }

    Runnable updatePlayerState = new Runnable() {
        @SuppressLint("SetTextI18n")
        @Override
        public void run() {
            PlayingSession playingSession = myApplication.getPlayingSession();
            if (playingSession != null) {
                play.setImageResource(playingSession.isPlaying ? R.drawable.btn_pause : R.drawable.btn_play);
                songTitleLabel.setText(myApplication.getParahsot().get(myApplication.getPlayingSession().currentParashPosition).label + " " + (myApplication.getPlayingSession().currentTrack + 1)
                        + "/" + myApplication.getParahsot().get(myApplication.getPlayingSession().currentParashPosition).totalTracks);
                songTotalDurationLabel.setText("" + Utilities.milliSecondsToTimer((long) myApplication.getPlayingSession().totalDuration));
                if (!progressIsDragging) {
                    songCurrentDurationLabel.setText("" + Utilities.milliSecondsToTimer(myApplication.getPlayingSession().currentDuration));
                    // Updating progress bar
                    int progress = (Utilities.getProgressPercentage(myApplication.getPlayingSession().currentDuration, (long) myApplication.getPlayingSession().totalDuration));
                    songProgressBar.setProgress(progress);
                }
            }
            play.postDelayed(this, 1000);

        }
    };

    private boolean isPlaying() {
        final PlayingSession playingSession = myApplication.getPlayingSession();
        return playingSession != null && playingSession.isPlaying;
    }

//
//    /**
//     * Update player data
//     *
//     * @param chapterChanged if <b>true</b>, update the new playing chapter, else update player time
//     */
//    public void updatePlayerInUIThread(boolean chapterChanged) {
//        runOnUiThread(updatePlayerTime);
//        if (chapterChanged) {
//            runOnUiThread(updatePlayerChapter);
//        }
//    }
//
//    Runnable updatePlayerChapter = new Runnable() {
//        @SuppressLint("SetTextI18n")
//        @Override
//        public void run() {
//            play.setImageResource(R.drawable.btn_pause);
//            songTitleLabel.setText(myApplication.getParahsot().get(myApplication.getPlayingSession().currentParashPosition).label + " " + (myApplication.getPlayingSession().currentTrack + 1)
//                    + "/" + myApplication.getParahsot().get(myApplication.getPlayingSession().currentParashPosition).totalTracks);
//            songTotalDurationLabel.setText("" + Utilities.milliSecondsToTimer((long) myApplication.getPlayingSession().totalDuration));
//        }
//    };
//
//    Runnable updatePlayerTime = new Runnable() {
//        @SuppressLint("SetTextI18n")
//        @Override
//        public void run() {
//            if (!progressIsDragging) {
//                songCurrentDurationLabel.setText("" + Utilities.milliSecondsToTimer(myApplication.getPlayingSession().currentDuration));
//                // Updating progress bar
//                int progress = (Utilities.getProgressPercentage(myApplication.getPlayingSession().currentDuration, (long) myApplication.getPlayingSession().totalDuration));
//                songProgressBar.setProgress(progress);
//            }
//            songProgressBar.postDelayed(this, 1000);
//        }
//    };

}
