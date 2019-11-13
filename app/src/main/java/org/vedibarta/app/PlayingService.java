package org.vedibarta.app;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

//import com.splunk.mint.Mint;

public class PlayingService extends Service implements
		MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
		MediaPlayer.OnSeekCompleteListener {
	Intent intent;
	TelephonyManager telephonyManager;
	PhoneStateListener listener;

	static int pauseplay;
	private String path = null;
	private boolean prepare;
	private boolean first;
	private boolean wasPlay;
	public static MediaPlayer mp;
	Intent broadcastIntent = new Intent();
	public final IBinder localBinder = new LocalBinder();
	private Handler mHandler;
	private final int seekBackwardTime = 10000; // 10 seconds
	AudioManager am;
	OnAudioFocusChangeListener afChangeListener;

	public IBinder onBind(Intent intent) {
		return localBinder;
	}

	@Override
	public void onCreate() {
		broadcastIntent.setAction("UPDATE");
		mHandler = new Handler();
		mp = new MediaPlayer();
		wasPlay = false;
		first = true;
		mp.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				prepare = false;
				broadcastIntent.putExtra("STATE", 3);
				sendBroadcast(broadcastIntent);
			}

		});
		mp.setOnSeekCompleteListener(new OnSeekCompleteListener() {
			public void onSeekComplete(MediaPlayer mp) {
				if (!mp.isPlaying()) {
					mp.start();
					// update timer progress again
				}
				updateProgressBar();
			}
		});
		mp.setOnPreparedListener(new OnPreparedListener() {
			public void onPrepared(MediaPlayer mp) {
				try {
					prepare = true;
					if ( intent.getLongExtra("CURRENT", 0) > 0)
						mp.seekTo((int) intent.getLongExtra("CURRENT", 0));
					else {
						mp.start();
						broadcastIntent.putExtra("STATE", 1);
						sendBroadcast(broadcastIntent);
					}
				} catch (Exception e) {
					e.printStackTrace();
//                    Mint.logException(e);
				}

			}

		});
		am = (AudioManager) getApplication().getSystemService(Context.AUDIO_SERVICE);
		afChangeListener = new OnAudioFocusChangeListener() {
			public void onAudioFocusChange(int focusChange) {
                if (mp == null)
                    return;
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                    if (mp.isPlaying())
                        mp.stop();
                    if (PlayerActivity.mNotificationManager != null) {
                        PlayerActivity.mNotificationManager.cancelAll();
                    }
                    wasPlay = false;
                    broadcastIntent.putExtra("STATE", 4);
                    sendBroadcast(broadcastIntent);
                    am.abandonAudioFocus(afChangeListener);
                } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                    if (mp != null && !mp.isPlaying() && wasPlay)
                        mp.start();
                    wasPlay = false;
                }
                else {
                    if (mp.isPlaying()) {
                        mp.pause();
                        wasPlay = true;
                    }
                }
			}
		};
		telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		// Create a new PhoneStateListener
		listener = new PhoneStateListener() {

			@Override
			public void onCallStateChanged(int state, String incomingNumber) {
				switch (state) {
				case TelephonyManager.CALL_STATE_IDLE:
					if (mp != null && wasPlay)
						mp.start();
					wasPlay = false;
					break;
				case TelephonyManager.CALL_STATE_OFFHOOK:
				case TelephonyManager.CALL_STATE_RINGING:
					if (mp.isPlaying()) {
						mp.pause();
						wasPlay = true;
					}
					break;
				}
			}
		};
		// Register the listener wit the telephony manager
		telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);

	}

	@Override
	public int onStartCommand(Intent i, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		intent = i;
		if (intent != null) {
			switch (intent.getIntExtra("STATE", 0)) {
			case 1:
				try {
					if (first) {
						am.requestAudioFocus(afChangeListener,
								AudioManager.STREAM_MUSIC,
								AudioManager.AUDIOFOCUS_GAIN);
					}
					mHandler.removeCallbacks(mUpdateTimeTask);
					prepare = false;
					path = intent.getStringExtra("PATH");
					if (mp.isPlaying())
						mp.stop();
					mp.reset();
					mp.setDataSource(path);
					mp.prepareAsync();
					updateProgressBar();

				} catch (Exception e) {
					e.printStackTrace();
//					Mint.logException(e);
				}
				break;
			case 2:
				if (mp.isPlaying())
					mp.pause();
				break;
			case 3:
				if (!mp.isPlaying())
					mp.start();
				break;
			case 4:
				mHandler.removeCallbacks(mUpdateTimeTask);
				break;
			case 5:
				mp.seekTo((int) intent.getIntExtra("SEEK", 0));
				break;
			case 6:
				mHandler.removeCallbacks(mUpdateTimeTask);
				if (mp != null && mp.isPlaying())
					mp.stop();
				mp.release();
				stopSelf();
				break;
			case 7:
				try {
					if (mp.isPlaying()) {
						// get current song position
						int currentPosition = mp.getCurrentPosition();
						// check if seekBackward time is greater than 0 sec
						if (currentPosition - seekBackwardTime >= 0) {
							// forward song
							mp.seekTo(currentPosition - seekBackwardTime);
						} else {
							// backward to starting position
							mp.seekTo(0);
						}
					}
				} catch (IllegalStateException e) {
					e.printStackTrace();
//                    Mint.logException(e);
				}
				break;
			case 8:
				try {
					if (mp.isPlaying()) {
						// get current song position
						int currentPosition = mp.getCurrentPosition();
						// check if seekBackward time is greater than 0 sec
						if (currentPosition + seekBackwardTime <= mp
								.getDuration()) {
							// forward song
							mp.seekTo(currentPosition + seekBackwardTime);
						}
					}
				} catch (IllegalStateException e) {
					e.printStackTrace();
//                    Mint.logException(e);
				}
				break;
			}
		}
		return START_NOT_STICKY;
	}

	public class LocalBinder extends Binder {
		PlayingService getService() {
			return PlayingService.this;
		}
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		// TODO Auto-generated method stub

	}

	public void updateProgressBar() {
		// First time we need long delay so the file always download otherwise
		// we get errors
		first = true;
		mHandler.postDelayed(mUpdateTimeTask, 800);
	}

	private Runnable mUpdateTimeTask = new Runnable() {
		public void run() {
			if (mp != null && prepare) {
				long currentDuration = mp.getCurrentPosition();
				broadcastIntent.putExtra("STATE", 2);
				if (first) {
					broadcastIntent.putExtra("TOTAL", (int) mp.getDuration());
					broadcastIntent.putExtra("STATE", 1);
					first = false;
				}
				broadcastIntent.putExtra("CURRENT", currentDuration);
				sendBroadcast(broadcastIntent);

			}
			// Running this thread after 100 milliseconds
			mHandler.postDelayed(this, 100);

		}
	};

	public static void pauseSong() {
		if (mp.isPlaying()) {
			mp.pause();
			// mNotificationManager.cancel(4);
			pauseplay = 0;
		}
	}

	@Override
	public void onSeekComplete(MediaPlayer mp) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		// TODO Auto-generated method stub

	}

}
