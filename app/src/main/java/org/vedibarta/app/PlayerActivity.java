package org.vedibarta.app;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import com.splunk.mint.Mint;

import java.io.IOException;

public class PlayerActivity extends Activity implements
		SeekBar.OnSeekBarChangeListener, View.OnClickListener {
    private static final String EXTRA_PARASHA_NAME = "PARASHA";
    private static final String EXTRA_POSITION = "POSITION";
    private static final String EXTRA_FILE_IN_DEVICE = "FILE_EXIST";
    private static final String EXTRA_PATH = "PATH";
    private static final String EXTRA_STATE = "STATE";
    private static final String EXTRA_TRACK = "TRACK";
    private static final String EXTRA_CURRENT = "CURRENT";
    private static final String EXTRA_COUNT = "COUNT";
    private static final String EXTRA_LAUNCH = "launch";
    private static final String EXTRA_INDEX = "INDEX";
	public  static final String EXTRA_PARASHA_DATA = "PARASHA_DATA";
    private TextView songCurrentDurationLabel;
	private TextView songTotalDurationLabel;
	private TextView songTitleLabel;
	private ImageButton play;

    private ServiceConnection serviceConnection;
	private BroadcastReceiver broadcastReceiver;
	Intent connectionIntent;
	int totalDuration;
	long currentDuration;

	int currentTrack;
	private SeekBar songProgressBar;
	private Utilities utils;
	// Handler to update UI timer, progress bar etc,.
	private ParashotData myData;
	private boolean playing;
	private boolean fileExist;
	private boolean recreate;

	private int position;
	//private String path = null;
	private SharedPreferences myPref;
	private Context ctx;
	private int index;
	static NotificationManager mNotificationManager;
	Parasha parasha;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.player1);
		currentTrack = 1;
		ctx = this;
		myPref = getPreferences(0);
		myData = new ParashotData();
		utils = new Utilities();

		connectionIntent = new Intent(this, PlayingServiceNew.class);

		parasha =  getIntent().getParcelableExtra(EXTRA_PARASHA_DATA);

		// bind to our service by first creating a new connectionIntent
		serviceConnection = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName arg0, IBinder binder) {
			}

			@Override
			public void onServiceDisconnected(ComponentName arg0) {
			}

		};

		// Supply the Intent & ServiceConnection that will use for the binding
		bindService(connectionIntent, serviceConnection, Context.BIND_AUTO_CREATE);

		// Set up broadcast receiver for doing changes in UI
		IntentFilter filter = new IntentFilter();
		filter.addAction("UPDATE");
		broadcastReceiver = new BroadcastReceiver() {
			@Override
			public synchronized void onReceive(Context context, Intent i) {
				switch ( i.getIntExtra("STATE", 0)) {
				case 1:
					playing = true;
					play.setImageResource(R.drawable.btn_pause);
					break;
				case 2:
					totalDuration =  i.getIntExtra("TOTAL", 0);
					currentDuration =  i.getLongExtra("CURRENT", 0);
					songTotalDurationLabel.setText(""+ utils.milliSecondsToTimer((long) totalDuration));
					// Displaying time completed playing
					songCurrentDurationLabel
							.setText(""
									+ utils.milliSecondsToTimer( currentDuration));

					// Updating progress bar
					int progress = (utils.getProgressPercentage(
							 currentDuration, (long) totalDuration));
					songProgressBar.setProgress(progress);
					break;
				case 3:
					clickEvent(0);
					break;
				case 4:
					// this when audio focus had lost
					connectionIntent.putExtra("STATE", 6);
					startService(connectionIntent);
					playing = false;
					if (fileExist) {
						String result = getIntent().getStringExtra("PATH")
								+ ";" + currentTrack + ";" + currentDuration;
						try {
							utils.updateLine(ctx, index, true, result);
						} catch (IOException e) {
                            Mint.logException(e);
						}
					}
					Intent backIntent = new Intent(ctx, VedibartaActivity.class);
					backIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
					backIntent.putExtra("playing", false);
					startActivity(backIntent);
					currentTrack = 1;
					finish();
					break;
				}
			}
		};
		registerReceiver(broadcastReceiver, filter);

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
		if (mNotificationManager != null)
			mNotificationManager.cancel(1);
		// for update the file about the last data of playing state
		// we trying to play from last point for user
		if (myPref.getInt(EXTRA_POSITION, 100) == position)
			recreate = true;
		fileExist = getIntent().getBooleanExtra(EXTRA_FILE_IN_DEVICE, false);


		connectionIntent.putExtra(EXTRA_STATE, 1);
        if (playing ) {
            if (getIntent().getBooleanExtra(EXTRA_LAUNCH, false)) {
                if (fileExist) {
                    currentTrack = getIntent().getIntExtra(EXTRA_COUNT, 1);
                    connectionIntent.putExtra(EXTRA_CURRENT, getIntent().getLongExtra(EXTRA_CURRENT, 0));
                } else {
                    currentTrack = 1;
                }
                connectionIntent.putExtra(EXTRA_PATH, parasha.paths.get(currentTrack - 1));
                connectionIntent.putExtra(EXTRA_STATE, 1);
                startService(connectionIntent);
                loadClip();
            }
        }
		else {
			if (recreate || fileExist) {
				currentTrack = myPref.getInt(EXTRA_TRACK, 1);
				if (recreate) {
					connectionIntent.putExtra(EXTRA_CURRENT, myPref.getLong(EXTRA_CURRENT, 0));
					recreate = false;
				} else {
					connectionIntent.putExtra(EXTRA_CURRENT, getIntent().getLongExtra(EXTRA_CURRENT, 0));
				}
			}
			connectionIntent.putExtra(EXTRA_PATH, parasha.paths.get(currentTrack - 1));
            connectionIntent.putExtra(EXTRA_CURRENT, (long) 0);
            startService(connectionIntent);
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
			String result = getIntent().getStringExtra("PATH") + ";" + currentTrack
					+ ";" + currentDuration;
			try {
				utils.updateLine(ctx, index, true, result);
			} catch (IOException e) {
                Mint.logException(e);
			}
		} else {
			SharedPreferences.Editor editor = myPref.edit();
			editor.putLong("CURRENT", currentDuration);
			editor.putInt("TRACK", currentTrack);
			editor.putInt("POSITION", position);
			// Commit the edits!
			editor.commit();
		}

		// add notification if playing
		if (playing) {
			NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
					ctx)
					.setSmallIcon(R.drawable.ic_menu_play_clip)
					.setContentTitle(
							getResources().getString(R.string.app_name))
					.setContentText(
							getResources().getString(R.string.playing_parashat)
									+ " " + parasha.label).setAutoCancel(true);
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
		// whenever our activity gets destroyed, unbind from the service
		unbindService(this.serviceConnection);
		unregisterReceiver(broadcastReceiver);
	}

	private void loadClip() {
        // set Progress bar values
        songProgressBar.setProgress(0);
        songTitleLabel.setText(parasha.label + " " + currentTrack + "/" + parasha.totalTracks);
	}

	@Override
	public void onBackPressed() {
		// super.onBackPressed();

		if (!playing) {
			if (fileExist) {
				String result = getIntent().getStringExtra("PATH") + ";"
						+ currentTrack + ";" + currentDuration;
				try {
					utils.updateLine(ctx, index, true, result);
				} catch (IOException e) {
                    Mint.logException(e);
				}
			} else {
				connectionIntent.putExtra("STATE", 2);
				startService(connectionIntent);
			}
			finish();
		}

		Intent backIntent = new Intent(this, VedibartaActivity.class);
		backIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		backIntent.putExtra("playing", playing);
		startActivity(backIntent);
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
	}

	/**
	 * When user starts moving the progress handler
	 * */
	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		connectionIntent.putExtra("STATE", 4);
		startService(connectionIntent);
        progressIsDragging = true;
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		connectionIntent.putExtra("STATE", 5);
		int currentPosition = (int) utils.progressToTimer(
				(int) seekBar.getProgress(), (int) totalDuration);
		connectionIntent.putExtra("SEEK", currentPosition);
		startService(connectionIntent);
		play.setImageResource(R.drawable.btn_pause);
		playing = true;
        progressIsDragging = false;
	}

	private void clickEvent(int add) {
		currentTrack = currentTrack + add;
		if (currentTrack < parasha.totalTracks && currentTrack >= 0) {
			try {
				if (!fileExist)
					Toast.makeText(this,getResources().getString(R.string.begin_playing),	Toast.LENGTH_SHORT).show();
				connectionIntent.putExtra("PATH", parasha.paths.get(currentTrack));
				connectionIntent.putExtra("STATE", 1);
				startService(connectionIntent);
				loadClip();
				play.setImageResource(R.drawable.btn_play);
				currentTrack++;
			} catch (Throwable t) {
			}
		} else {
			currentTrack = 1;
			connectionIntent.putExtra("STATE", 6);
			startService(connectionIntent);
			runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(PlayerActivity.this,
							getResources().getString(R.string.return_main),
							Toast.LENGTH_SHORT).show();
				}
			});
			playing = false;
			finish();
		}

	}


	@Override
	public void onClick(View v) {
		switch ((int) v.getId()) {
		case R.id.btnPlay:
			// check for already playing
			if (playing) {
				connectionIntent.putExtra("STATE", 2);
				startService(connectionIntent);
				play.setImageResource(R.drawable.btn_play);
				playing = false;
			} else {
				connectionIntent.putExtra("STATE", 3);
				startService(connectionIntent);
				play.setImageResource(R.drawable.btn_pause);
				playing = true;
			}
			break;
		case R.id.btnNext:
			clickEvent(0);
			break;
		case R.id.btnPrevious:
			clickEvent(-2);
			break;
		case R.id.btnForward:
			connectionIntent.putExtra("STATE", 8);
			startService(connectionIntent);
			break;
		case R.id.btnBackward:
			connectionIntent.putExtra("STATE", 7);
			startService(connectionIntent);
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
            songTitleLabel.setText(parasha.label + " "+ currentTrack + "/" + parasha.totalTracks);
            songTotalDurationLabel.setText("" + utils.milliSecondsToTimer((long) totalDuration));
        }
    };

    private boolean progressIsDragging;
    Runnable updatePlayerTime = new Runnable() {
        @Override
        public void run() {
            if (!progressIsDragging) {
                songCurrentDurationLabel.setText("" + utils.milliSecondsToTimer( currentDuration));
                // Updating progress bar
                int progress = (utils.getProgressPercentage(currentDuration, (long) totalDuration));
                songProgressBar.setProgress(progress);
            }
        }
    };

}
