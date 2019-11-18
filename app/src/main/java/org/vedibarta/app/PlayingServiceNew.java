package org.vedibarta.app;

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
import android.util.Log;


public class PlayingServiceNew extends Service implements MediaPlayer.OnPreparedListener, OnCompletionListener, OnAudioFocusChangeListener {

    private static final String TAG = "PlayingServiceNew";
    @SuppressWarnings("FieldCanBeLocal")
    private String BASE_AUDIO_PATH = "http://www.vedibarta.org/Rashi_Tora_MP3/";

    public final static int START_PLAY = 0;
    public final static int PLAY_PRESSED = 1;
    public final static int NEXT_PRESSED = 2;
    public final static int PREVIOUS_PRESSED = 3;
    public final static int SEEK_TO = 5;
    public final static int ACTIVITY_STOP = 8;
    public final static int ACTIVTY_RESUME = 9;
    public final static int ACTIVIY_DESTROY = 10;
    public final static int END_PLAY = 11;
    public final static int NOTIFICATION_STOP = 12;

    public final static String EXTRA_COMMAND = "extra_command";
    public final static String EXTRA_PAR_POSITION = "extra_par_position";
    public final static String EXTRA_CURRENT_TRACK = "extra_current_track";
    public final static String EXTRA_TOTAL_TRACKS = "extra_total_tracks";
    public final static String LAST_SESSION = "last_session";

//    public static boolean playing;
//    public static int currentDuration, totalDuration;
//    private static int currentParashPosition, currentTrack, totalTracks;
    private MyApplication myApplication;
    private SharedPreferences prefs;
    private boolean wasPlay;

    private MediaPlayer mp;
    private Handler mHandler;
    private NotificationManager manager;
    private AudioManager am;

    @Override
    public void onCreate() {
        myApplication = (MyApplication) getApplication();
        mHandler = new Handler();
        manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        am = (AudioManager) getApplication().getSystemService(Context.AUDIO_SERVICE);
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        // Create a new PhoneStateListener
        PhoneStateListener listener = new PhoneStateListener() {
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
            int command = intent.getIntExtra(EXTRA_COMMAND, -1);
            switch (command) {
                case START_PLAY:
                    int currentParashPosition = intent.getIntExtra(EXTRA_PAR_POSITION, -1);
                    int currentTrack = intent.getIntExtra(EXTRA_CURRENT_TRACK, -1);
                    int totalTracks = intent.getIntExtra(EXTRA_TOTAL_TRACKS, -1);
                    myApplication.setPlayingSession(new PlayingSession(currentParashPosition, currentTrack, totalTracks));
                    makePlaying();
                    break;
                case PLAY_PRESSED:
                    if (mp.isPlaying())
                        pausePlay();
                    else {
                        resumePlay();
                    }
                    break;
                case NEXT_PRESSED:
                    nextTrack();
                    break;
                case PREVIOUS_PRESSED:
                    previewsTrack();
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
//                    mHandler.removeCallbacks(mUpdateTimeTask);
                    break;
                case ACTIVTY_RESUME:
//                    if (mp.isPlaying()) {
//                        updateCurrentTime();
//                    }
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
//        stopForeground(true);
        myApplication.getPlayingSession().isPlaying = false;
        manager.cancelAll();
        mHandler.removeCallbacks(mUpdateTimeTask);
        mHandler.postDelayed(checkServiceInactiveInterval, 1000 * 60 * 10);
    }

    public void makePlaying() {
        try {
            mHandler.removeCallbacks(mUpdateTimeTask);
            if (myApplication.getPlayingSession().currentTrack <= myApplication.getPlayingSession().totalTracks) {
                int result = am
                        .requestAudioFocus(this,
                                AudioManager.STREAM_MUSIC,
                                AudioManager.AUDIOFOCUS_GAIN);
                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    String relativePath = ParashotData.getRelativePath(myApplication.getPlayingSession().currentParashPosition, myApplication.getPlayingSession().currentTrack);
                    String path = Utilities.isLocalFileExists(getApplicationContext(), relativePath);
                    if (path == null) {
                        path = BASE_AUDIO_PATH + relativePath;
                    }
                    Log.d(TAG, "playing track:" + path);
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
        }
        myApplication.getPlayingSession().isPlaying = true;
        mHandler.removeCallbacks(checkServiceInactiveInterval);
        updateCurrentTime();
    }

    private void pausePlay() {
        myApplication.getPlayingSession().isPlaying = false;
        if (mp.isPlaying()) {
            mp.pause();
        }
        saveCurrentSessionState();
        mHandler.postDelayed(checkServiceInactiveInterval, 1000 * 60 * 10);
        mHandler.removeCallbacks(mUpdateTimeTask);
    }

    private void nextTrack(){
        if (myApplication.getPlayingSession().currentTrack == myApplication.getPlayingSession().totalTracks - 1) {
            clearCurrentSessionState();
            endPlay();
        } else {
            myApplication.getPlayingSession().currentTrack++;
            makePlaying();
        }
    }

    private void previewsTrack(){
        if (myApplication.getPlayingSession().currentTrack > 0) {
            myApplication.getPlayingSession().currentTrack--;
            makePlaying();
        }
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
        } else if (nextPosition > myApplication.getPlayingSession().totalDuration) {
            nextTrack();
        } else
            mp.seekTo(nextPosition);
    }

    private void saveCurrentSessionState() {
        final PlayingSession playingSession = myApplication.getPlayingSession();

        prefs.edit()
                .putString(LAST_SESSION,
                        "" + playingSession.currentParashPosition + "|" +
                                playingSession.currentTrack + "|" +
                                playingSession.currentDuration + "|" +
                                System.currentTimeMillis())
                .apply();
    }

    private void clearCurrentSessionState() {
        prefs.edit()
                .remove(LAST_SESSION)
                .apply();
    }


    public void updateCurrentTime() {
        mHandler.removeCallbacks(mUpdateTimeTask);
        mHandler.post(mUpdateTimeTask);
    }

    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            if (mp.isPlaying()) {
                myApplication.getPlayingSession().currentDuration = mp.getCurrentPosition();
            }
            mHandler.postDelayed(this, 1000);
        }
    };


    @Override
    public void onCompletion(MediaPlayer mp) {
        nextTrack();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        myApplication.getPlayingSession().totalDuration = mp.getDuration();
        myApplication.getPlayingSession().isPlaying = true;
        mp.start();
        updateCurrentTime();
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
        } else {
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


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}


