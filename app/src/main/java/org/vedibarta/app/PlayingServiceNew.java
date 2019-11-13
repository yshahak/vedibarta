package org.vedibarta.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.RemoteViews;

//import com.splunk.mint.Mint;

public class PlayingServiceNew extends Service implements MediaPlayer.OnPreparedListener, OnCompletionListener, OnAudioFocusChangeListener {
    Context ctx;
    MyApplication myApplication;
    public final static int START_PLAY = 0, PLAY_PRESSED = 1, SEEK_TO = 5, ACTIVITY_STOP = 8
            , ACTIVTY_RESUME = 9, ACTIVIY_DESTROY = 10, END_PLAY = 11, NOTIFICATION_STOP = 12;
    public static int currentDuration, totalDuration;

    public final static String COMMAND = "command";
    public final static String LAST_PLAY_TRACK = "last_play_chapter";
    public static boolean playing;
    private boolean wasPlay;
    public MediaPlayer mp;
    public Handler mHandler;
    private RemoteViews contentView;
    public NotificationManager manager;
    TelephonyManager telephonyManager;
    PhoneStateListener listener;
    AudioManager am;
    SharedPreferences pref;
    private Notification notification;

    @Override
    public void onCreate() {
        ctx = this;
        myApplication = (MyApplication) getApplication();
        myApplication.setPlayingService(this);
        mHandler = new Handler();
        manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        pref = PreferenceManager.getDefaultSharedPreferences(myApplication);
        am = (AudioManager) getApplication().getSystemService(Context.AUDIO_SERVICE);


        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        // Create a new PhoneStateListener
        listener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    case TelephonyManager.CALL_STATE_IDLE:
                        if (wasPlay)
                            resumePlay();
                        wasPlay = false;
                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (mp.isPlaying()) {
                            wasPlay = true;
                            pausePlay();
                        }
                        break;
                }
            }
        };
        // Register the listener with the telephony manager
        telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
        mp = initializeMP();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (intent != null) {
            int command = intent.getIntExtra(COMMAND, -1);
            switch (command) {
                case START_PLAY:
                    makePlaying(myApplication.parahsot.get(PlayerActivity.currentParashPosition).paths.get(PlayerActivity.currentTrack));
                    break;
                case PLAY_PRESSED:
                    if (mp.isPlaying())
                        pausePlay();
                    else {
                        resumePlay();
                        updateCurrentTime();
                    }
                    break;
                case SEEK_TO:
                    int moveTo = intent.getIntExtra("MOVE_TO", 0);
                    boolean absValue = intent.getBooleanExtra("ABS_VALUE", false);
                    seekTo(moveTo, absValue);
                    break;
                case END_PLAY:
                    pausePlay();
                    endPlay();
                    break;
                case ACTIVITY_STOP:
                    mHandler.removeCallbacks(mUpdateTimeTask);
                    break;
                case ACTIVTY_RESUME:
                    if (mp.isPlaying()) {
                        updateCurrentTime();
                    }
                    break;
                case ACTIVIY_DESTROY:
                    if (mp != null && !mp.isPlaying())
                        mHandler.postDelayed(checkServiceInactiveInterval, 1000 * 60 * 30);
                    break;
                case NOTIFICATION_STOP:

                    break;
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mp != null) {
            mp.release();
            mp = null;
        }
        try {
            myApplication.setPlayingService(null);
            mHandler.removeCallbacksAndMessages(null);
        } catch (NullPointerException e) {
            //Mint.logException(e);
        }
        manager.cancelAll();
    }

    private MediaPlayer initializeMP() {
        MediaPlayer mp = new MediaPlayer();
        mp.setOnCompletionListener(this);
        mp.setOnPreparedListener(this);
        mp.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        return mp;
    }

    private void endPlay() {
        if (mp.isPlaying()) {
            mp.stop();
        }
        stopForeground(true);
        mHandler.removeCallbacks(mUpdateTimeTask);
        playing = false;
        saveCurrentPlayingData();
        manager.cancelAll();
        mHandler.postDelayed(checkServiceInactiveInterval, 1000 * 60 * 10);
    }

    public void makePlaying(String path) {
        try {
            mHandler.removeCallbacks(mUpdateTimeTask);
            if (PlayerActivity.currentTrack <= PlayerActivity.numberOfTracks) {
                int result = am
                        .requestAudioFocus(this,
                                AudioManager.STREAM_MUSIC,
                                AudioManager.AUDIOFOCUS_GAIN);
                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    if (mp == null) {
                        mp = initializeMP();
                    }
                    mp.reset();
                    mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mp.setDataSource(path);
                    mp.prepareAsync();
                } else {
                    endPlay();
                }
            }
        } catch (Exception e) {
//            Mint.logException(e);
            e.printStackTrace();
        }
    }

    private void resumePlay() {
        if (!mp.isPlaying()) {
            mp.start();
            playing = true;
        }
        updateNotification();
        mHandler.removeCallbacks(checkServiceInactiveInterval);

    }

    private void pausePlay() {
        playing = false;
        if (mp.isPlaying()) {
            mp.pause();
        }
        updateNotification();
        saveCurrentPlayingData();
        mHandler.postDelayed(checkServiceInactiveInterval, 1000 * 60 * 10);

    }

    private void saveCurrentPlayingData() {
        Parasha parasha = myApplication.parahsot.get(PlayerActivity.currentParashPosition);
        parasha.lastPlayedTrack = PlayerActivity.currentTrack;
        parasha.lastPlayedPosition = currentDuration;
    }

    private void seekTo(int moveTo, boolean absValue) {
        int nextPosition;
        if (absValue)
            nextPosition = moveTo;
        else {
            int currentPosition = mp.getCurrentPosition();
            nextPosition = currentPosition + (moveTo);
        }
        // check if seekBackward time is greater than 0 sec
        if (nextPosition < 0) {
            mp.seekTo(0);
        } else if (nextPosition > totalDuration) {
            /*currentChapter++;
            makePlaying();*/
            //TODO fix this logic
        } else
            mp.seekTo(nextPosition);
    }


    public void updateCurrentTime() {
        mHandler.postDelayed(mUpdateTimeTask, 100);//little delay so mp.isPlaying() will update
    }

    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            if (mp.isPlaying()) {
                currentDuration = mp.getCurrentPosition();
                /*PlayerActivity activity = myApplication.getPlayerActivity();
                if (activity != null) {
                    activity.updatePlayerInUIThread(false);
                }*/
                mHandler.postDelayed(this, 1000);
            } else {
                mHandler.removeCallbacks(mUpdateTimeTask);
            }
        }
    };




    @Override
    public void onCompletion(MediaPlayer mp) {
        if (PlayerActivity.currentTrack == PlayerActivity.numberOfTracks - 1)
            endPlay();
        else {
            PlayerActivity.currentTrack++;
            makePlaying(myApplication.parahsot.get(PlayerActivity.currentParashPosition).paths.get(PlayerActivity.currentTrack));
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        totalDuration = mp.getDuration();
        mp.start();
        playing = true;
        updateNotification();
        PlayerActivity activity = myApplication.getPlayerActivity();
        if (activity != null) {
            updateCurrentTime();
            activity.updatePlayerInUIThread(true);
        }
    }

    public void onAudioFocusChange(int focusChange) {
        if (mp == null)
            return;
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            am.abandonAudioFocus(this);
            wasPlay = false;
            endPlay();
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            if (wasPlay)
                resumePlay();
            wasPlay = false;
        }
        else {
            if (mp.isPlaying()) {
                wasPlay = true;
                pausePlay();
            }
        }
    }

    Runnable checkServiceInactiveInterval = new Runnable() {
        @Override
        public void run() {
            if (mp == null || !mp.isPlaying())
                stopSelf();
        }
    };

    public void updateNotification() {
        /*if (contentView == null) {
            contentView = new RemoteViews(getPackageName(), R.layout.notification_control);
            setListeners(contentView);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(ctx)
                    .setContent(contentView)
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .setPriority(Notification.PRIORITY_HIGH);
            notification = mBuilder.build();
        }
        contentView.setTextViewText(R.id.text, "תהלים " + chapters[PlayingService.currentChapter - 1] + "'");
        if (playing)
            contentView.setImageViewResource(R.id.pause, android.R.drawable.ic_media_pause);
        else
            contentView.setImageViewResource(R.id.pause, android.R.drawable.ic_media_play);
        startForeground(1, notification);*/

        //manager.notify(0, notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}


