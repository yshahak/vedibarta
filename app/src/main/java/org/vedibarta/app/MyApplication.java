package org.vedibarta.app;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;


/**
 * Created by yshahak on 07/01/2015.
 */
public class MyApplication extends Application {
    private static final String EXTRA_SAVED_DATA = "savedData";
    //Tracker tracker;

    private final String TAG = getClass().getSimpleName();

    SharedPreferences pref;

    PlayerActivity playerActivity;
    SplashActivity splashActivity;
    DownloadService downloadService;
    PlayingServiceNew playingService;
    final String MINT_TAG = "";
    ArrayList<Parasha> parahsot;


    private int currentParashaPosition;



    @Override
    public void onCreate(){
        super.onCreate();
        String data = PreferenceManager.getDefaultSharedPreferences(this).getString(EXTRA_SAVED_DATA, "null");
        if ("null".equals(data)){
            new Thread(setData).start();
        }else {

        }
        //Mint.initAndStartSession(this, MINT_TAG);
    }

    public void setCurrentParashaPosition(int currentParashaPosition) {
        this.currentParashaPosition = currentParashaPosition;
    }

    public int getCurrentParashaPosition() {
        return currentParashaPosition;
    }

    public void setDownloadService(DownloadService service){
        downloadService = service;
    }

    public DownloadService getDownloadService(){
        return downloadService;
    }

    public void setPlayingService(PlayingServiceNew service){
        playingService = service;
    }

    public PlayingServiceNew getPlayingService(){
        return playingService;
    }

    public void setPlayerActivity(PlayerActivity activity){
        playerActivity = activity;
    }

    public PlayerActivity getPlayerActivity(){
        return playerActivity;
    }
    public void setSplashActivity(SplashActivity activity){
        splashActivity = activity;
    }

    public SplashActivity getSplashActivity(){
        return splashActivity;
    }


    Runnable setData = new Runnable() {
        @Override
        public void run() {
            parahsot = new ArrayList<Parasha>();
            int size = ParashotData.parashot.length;
            for (int i = 0 ; i < size; i++){
                String name = ParashotData.getParashaHeb(i);
                int numberTracks = ParashotData.getTracksNumber(i);
                ArrayList<String> arrayList = new ArrayList<String>();
                for (int j = 0; j < numberTracks; j++){
                    arrayList.add(ParashotData.getPath(i, j)[1]);
                }
                parahsot.add(new Parasha(name, numberTracks, arrayList));
            }
        }
    };

   /* public synchronized Tracker getTracker() {
        if (tracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            tracker =  analytics.newTracker(R.xml.app_tracker);
        }
        return tracker;
    }*/

}