package org.vedibarta.app;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.splunk.mint.Mint;

import java.io.File;
import java.io.IOException;

public class PlayerActivity extends Activity implements
		SeekBar.OnSeekBarChangeListener, View.OnClickListener {
	private TextView songCurrentDurationLabel;
	private TextView songTotalDurationLabel;
	private TextView songTitleLabel;
	private ImageButton play;
	private ImageButton btnForward;
	private ImageButton btnBackward;
	private ImageButton next;
	private ImageButton previous;

	private ServiceConnection serviceConnection;
	private BroadcastReceiver broadcastReceiver;
	Intent connectionIntent;
	int totalDuration;
	long currentDuration;

	int count;
	int numberOfTracks;
	private SeekBar songProgressBar;
	private Utilities utils;
	// Handler to update UI timer, progress bar etc,.
	private ParashotData myData;
	private boolean playing;
	private boolean fileExist;
	private boolean recreate;
	boolean running;

	private int position;
	private String path = null;
	private String trackTitle = null;
	private SharedPreferences myPref;
	private Context ctx;
	private int index;
	static NotificationManager mNotificationManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.player1);
		count = 1;
		ctx = this;
		recreate = false;
		playing = false;
		running = false;
		myPref = getPreferences(0);
		myData = new ParashotData();
		utils = new Utilities();

		connectionIntent = new Intent(this, PlayingService.class);

		path =  getIntent().getStringExtra("PATH");

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
					songTotalDurationLabel.setText(""
							+ utils.milliSecondsToTimer((long) totalDuration));
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
								+ ";" + count + ";" + currentDuration;
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
					count = 1;
					finish();
					break;
				}
			}
		};
		registerReceiver(broadcastReceiver, filter);

		play = (ImageButton) findViewById(R.id.btnPlay);
		next = (ImageButton) findViewById(R.id.btnNext);
		btnForward = (ImageButton) findViewById(R.id.btnForward);
		btnBackward = (ImageButton) findViewById(R.id.btnBackward);
		previous = (ImageButton) findViewById(R.id.btnPrevious);
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
		position = getIntent().getIntExtra("POSITION", 0);
		trackTitle = getIntent().getStringExtra("PARASHA");
		// for update the file about the last data of playing state
		index = getIntent().getIntExtra("INDEX", 0);
		// we trying to play from last point for user
		if (myPref.getInt("POSITION", 100) == position)
			recreate = true;
		fileExist = getIntent().getBooleanExtra("FILE_EXIST", false);
		numberOfTracks = myData.tracksNumber(position);

		connectionIntent.putExtra("STATE", 1);
		if (!playing && !running) {
			path =  getIntent().getStringExtra("PATH");
			if (recreate || fileExist) {
				if (recreate) {
					count = myPref.getInt("TRACK", 1);
					path = myData.getPath(position, count - 1)[1];
					connectionIntent.putExtra("CURRENT",
							myPref.getLong("CURRENT", 0));
					recreate = false;
				} else {
					count =  getIntent().getIntExtra("COUNT", 1);
					String myFile = myData.getPath(position, count - 1)[0];
					path = path + File.separator + myFile;
					connectionIntent.putExtra("CURRENT", getIntent()
							.getLongExtra("CURRENT", 0));
				}
			}
			connectionIntent.putExtra("PATH", path);
			startService(connectionIntent);
			loadClip();
		}
		if (playing && getIntent().getBooleanExtra("launch", false)) {
			path =  getIntent().getStringExtra("PATH");
			if (fileExist) {
				count =  getIntent().getIntExtra("COUNT", 1);
				String myFile = myData.getPath(position, count - 1)[0];
				path =  path + File.separator + myFile;
				connectionIntent.putExtra("CURRENT",
						getIntent().getLongExtra("CURRENT", 0));
			} else {
				count = 1;
			}
			connectionIntent.putExtra("PATH", path);
			connectionIntent.putExtra("STATE", 1);
			startService(connectionIntent);
			loadClip();
		}
		connectionIntent.putExtra("CURRENT", (long) 0);
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
			String result = getIntent().getStringExtra("PATH") + ";" + count
					+ ";" + currentDuration;
			try {
				utils.updateLine(ctx, index, true, result);
			} catch (IOException e) {
                Mint.logException(e);
			}
		} else {
			SharedPreferences.Editor editor = myPref.edit();
			editor.putLong("CURRENT", currentDuration);
			editor.putInt("TRACK", count);
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
									+ " " + trackTitle).setAutoCancel(true);
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
		running = true;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// whenever our activity gets destroyed, unbind from the service
		unbindService(this.serviceConnection);
		unregisterReceiver(broadcastReceiver);
	}

	private void loadClip() {
		new Thread(new Runnable() {
			public void run() {
				Looper.prepare();
				try {
					// set Progress bar values
					songProgressBar.setProgress(0);
					songProgressBar.setMax(100);
					runOnUiThread(new Runnable() {
						public void run() {
							songTitleLabel.setText((String) trackTitle + " "
									+ count + "/" + numberOfTracks);
						}
					});

				} catch (Throwable t) {
					goBlooey(t);

				}
			}
		}).start();
	}

	@Override
	public void onBackPressed() {
		// super.onBackPressed();

		if (!playing) {
			if (fileExist) {
				String result = getIntent().getStringExtra("PATH") + ";"
						+ count + ";" + currentDuration;
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
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromTouch) {
	}

	/**
	 * When user starts moving the progress handler
	 * */
	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		connectionIntent.putExtra("STATE", 4);
		startService(connectionIntent);
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

	}

	private void clickEvent(int add) {
		count = count + add;
		if (count < numberOfTracks && count >= 0) {
			try {
				if (fileExist)
					path = (String) getIntent().getStringExtra("PATH") + "/"
							+ myData.getPath(position, count)[0];
				else {
					path = myData.getPath(position, count)[1];
					Toast.makeText(PlayerActivity.this,
							getResources().getString(R.string.begin_playing),
							Toast.LENGTH_SHORT).show();
				}
				connectionIntent.putExtra("PATH", path);
				connectionIntent.putExtra("STATE", 1);
				startService(connectionIntent);
				loadClip();
				play.setImageResource(R.drawable.btn_play);
				count++;
			} catch (Throwable t) {
				goBlooey(t);
			}
		} else {
			count = 1;
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

	private void goBlooey(Throwable t) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Exception!").setMessage(t.toString())
				.setPositiveButton("OK", null).show();
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

}
