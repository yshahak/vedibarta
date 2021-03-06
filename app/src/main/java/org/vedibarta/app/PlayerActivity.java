package org.vedibarta.app;

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
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class PlayerActivity extends Activity implements
		SeekBar.OnSeekBarChangeListener, View.OnClickListener {
    private static final String EXTRA_FILE_IN_DEVICE = "FILE_EXIST";
    //public static final String EXTRA_PATH = "PATH";
   // private static final String EXTRA_TRACK = "TRACK";
    //private static final String EXTRA_CURRENT = "CURRENT";
    //private static final String EXTRA_COUNT = "COUNT";
    private static final String EXTRA_LAUNCH = "launch";
	public static final String EXTRA_PARASHA_DATA = "PARASHA_DATA";
    private TextView songCurrentDurationLabel;
	private TextView songTotalDurationLabel;
	private TextView songTitleLabel;
	private ImageButton play;

    private ServiceConnection serviceConnection;
	Intent playingIntent;
    MyApplication myApplication;
	private SeekBar songProgressBar;
	// Handler to update UI timer, progress bar etc,.
	private boolean fileExist;

	private SharedPreferences myPref;
	static NotificationManager mNotificationManager;
    static int currentParashPosition, currentTrack, numberOfTracks;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		((MyApplication)getApplication()).setPlayerActivity(this);
		setContentView(R.layout.player1);
		myPref = getPreferences(0);
        myApplication = (MyApplication)getApplication();
		playingIntent = new Intent(this, PlayingServiceNew.class);

		//parasha =  getIntent().getParcelableExtra(EXTRA_PARASHA_DATA);

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

		play = (ImageButton) findViewById(R.id.btnPlay);
        ImageButton next = (ImageButton) findViewById(R.id.btnNext);
        ImageButton btnForward = (ImageButton) findViewById(R.id.btnForward);
        ImageButton btnBackward = (ImageButton) findViewById(R.id.btnBackward);
        ImageButton previous = (ImageButton) findViewById(R.id.btnPrevious);
		songProgressBar = (SeekBar) findViewById(R.id.songProgressBar);
		songProgressBar.setProgress(0);
		songCurrentDurationLabel = (TextView) findViewById(R.id.songCurrentDurationLabel);
		songTotalDurationLabel = (TextView) findViewById(R.id.songTotalDurationLabel);
		songTitleLabel = (TextView) findViewById(R.id.songTitle);
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
        numberOfTracks = myApplication.parahsot.get(currentParashPosition).totalTracks;
		if (mNotificationManager != null)
			mNotificationManager.cancel(1);
		// for update the file about the last data of playing state
		// we trying to play from last point for user
		fileExist = getIntent().getBooleanExtra(EXTRA_FILE_IN_DEVICE, false);


		playingIntent.putExtra(PlayingServiceNew.COMMAND, PlayingServiceNew.START_PLAY);
        if (PlayingServiceNew.playing ) {
            if (getIntent().getBooleanExtra(EXTRA_LAUNCH, false)) {
                if (fileExist) {
                    //currentTrack = getIntent().getIntExtra(EXTRA_COUNT, 1);
                    //playingIntent.putExtra(EXTRA_CURRENT, getIntent().getLongExtra(EXTRA_CURRENT, 0));
                }
                //playingIntent.putExtra(EXTRA_PATH, myApplication.parahsot.get(currentParashPosition).paths.get(currentTrack - 1));
                startService(playingIntent);
                loadClip();
            }
        }
		else {
			if (fileExist) {
				//currentTrack = myPref.getInt(EXTRA_TRACK, 1);
                //playingIntent.putExtra(EXTRA_CURRENT, getIntent().getLongExtra(EXTRA_CURRENT, 0));
			}
			//playingIntent.putExtra(EXTRA_PATH, myApplication.parahsot.get(currentParashPosition).paths.get(currentTrack - 1));
            //playingIntent.putExtra(EXTRA_CURRENT, (long) 0);
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
			NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
					this)
					.setSmallIcon(R.drawable.ic_menu_play_clip)
					.setContentTitle(
							getResources().getString(R.string.app_name))
					.setContentText(
							getResources().getString(R.string.playing_parashat)
									+ " " + myApplication.parahsot.get(currentParashPosition).label).setAutoCancel(true);
			Intent resultIntent = new Intent(getApplicationContext(),
					PlayerActivity.class);
			resultIntent.setAction(Intent.ACTION_MAIN);
			resultIntent.addCategory(Intent.CATEGORY_LAUNCHER);

			PendingIntent resultPendingIntent = PendingIntent.getActivity(
					getApplicationContext(), 0, resultIntent, 0);
			mBuilder.setContentIntent(resultPendingIntent);
			mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			// mId allows you to update the notification later on.
			mNotificationManager.notify(1, mBuilder.build());
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		((MyApplication)getApplication()).setPlayerActivity(null);
		// whenever our activity gets destroyed, unbind from the service
		unbindService(this.serviceConnection);
        songProgressBar.removeCallbacks(updatePlayerTime);
	}

	private void loadClip() {
        // set Progress bar values
        songProgressBar.setProgress(0);
        songTitleLabel.setText(myApplication.parahsot.get(currentParashPosition).label + " " + (currentTrack + 1) + "/" +
                myApplication.parahsot.get(currentParashPosition).totalTracks);
	}

	@Override
	public void onBackPressed() {
		// super.onBackPressed();

		if (!PlayingServiceNew.playing) {
			if (fileExist) {

			} else {
				playingIntent.putExtra(PlayingServiceNew.COMMAND, 2);
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
		int currentPosition = Utilities.progressToTimer(seekBar.getProgress(),  PlayingServiceNew.totalDuration);
        playingIntent.putExtra("MOVE_TO", currentPosition);
        playingIntent.putExtra("ABS_VALUE", true);
        playingIntent.putExtra(PlayingServiceNew.COMMAND, PlayingServiceNew.SEEK_TO);
        startService(playingIntent);
        progressIsDragging = false;
	}

	private void clickEvent(int add) {
		currentTrack = currentTrack + add;
        if (currentTrack < 0) { // previus pressed when playing first track
            currentTrack = 0;
            playingIntent.putExtra("MOVE_TO", 0);
            playingIntent.putExtra("ABS_VALUE", true);
            playingIntent.putExtra(PlayingServiceNew.COMMAND, PlayingServiceNew.SEEK_TO);
            startService(playingIntent);
        }else if (!(currentTrack < numberOfTracks)){ // next pressed when playing last track
            currentTrack = numberOfTracks - 1;
        } else {
            if (!fileExist)
                Toast.makeText(this,getResources().getString(R.string.begin_playing), Toast.LENGTH_SHORT).show();
            playingIntent.putExtra(PlayingServiceNew.COMMAND, PlayingServiceNew.START_PLAY);
            startService(playingIntent);
            loadClip();
        }
	}


	@Override
	public void onClick(View v) {
		switch ( v.getId()) {
            case R.id.btnPlay:
                if (PlayingServiceNew.playing)
                    play.setImageResource(R.drawable.btn_play);
                else
                    play.setImageResource(R.drawable.btn_pause);
                playingIntent.putExtra(PlayingServiceNew.COMMAND, PlayingServiceNew.PLAY_PRESSED);
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
                playingIntent.putExtra(PlayingServiceNew.COMMAND, PlayingServiceNew.SEEK_TO);
                startService(playingIntent);
                break;
            case R.id.btnBackward:
                playingIntent.putExtra("MOVE_TO", -10000);
                playingIntent.putExtra("ABS_VALUE", false);
                playingIntent.putExtra(PlayingServiceNew.COMMAND, PlayingServiceNew.SEEK_TO);
                startService(playingIntent);
                break;
            }
	}

    /**
     * Update player data
     * @param chapterChanged if <b>true</b>, update the new playing chapter, else update player time
     */
    public void updatePlayerInUIThread(boolean chapterChanged) {
        runOnUiThread(updatePlayerTime);
        if (chapterChanged) {
            runOnUiThread(updatePlayerChapter);
        }
    }

    Runnable updatePlayerChapter = new Runnable() {
        @Override
        public void run() {
            play.setImageResource(R.drawable.btn_pause);
            songTitleLabel.setText(myApplication.parahsot.get(currentParashPosition).label + " " + (currentTrack + 1)
                    + "/" + myApplication.parahsot.get(currentParashPosition).totalTracks);
            songTotalDurationLabel.setText("" + Utilities.milliSecondsToTimer((long) PlayingServiceNew.totalDuration));
        }
    };

    private boolean progressIsDragging;
    Runnable updatePlayerTime = new Runnable() {
        @Override
        public void run() {
            if (!progressIsDragging) {
                songCurrentDurationLabel.setText("" + Utilities.milliSecondsToTimer( PlayingServiceNew.currentDuration));
                // Updating progress bar
                int progress = (Utilities.getProgressPercentage(PlayingServiceNew.currentDuration, (long) PlayingServiceNew.totalDuration));
                songProgressBar.setProgress(progress);
            }
            songProgressBar.postDelayed(this, 1000);
        }
    };

}
