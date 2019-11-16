package org.vedibarta.app;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;


/**
 * Created by yshahak on 07/01/2015.
 */
public class MyApplication extends Application {
    //Tracker tracker;

    private final String TAG = getClass().getSimpleName();

    PlayerActivity playerActivity;
    PlayingServiceNew playingService;
    private ArrayList<Parasha> parahsot;


    private int currentParashaPosition;



    @Override
    public void onCreate(){
        super.onCreate();
        new Thread(setData).start();
        NotificationHelper.createNotificationChannel(this);
        //Mint.initAndStartSession(this, MINT_TAG);
    }

    public void setCurrentParashaPosition(int currentParashaPosition) {
        this.currentParashaPosition = currentParashaPosition;
    }

    public int getCurrentParashaPosition() {
        return currentParashaPosition;
    }

    public void setPlayingService(PlayingServiceNew service){
        playingService = service;
    }

    public void setPlayerActivity(PlayerActivity activity){
        playerActivity = activity;
    }

    public PlayerActivity getPlayerActivity(){
        return playerActivity;
    }


    public ArrayList<Parasha> getParahsot() {
        return parahsot;
    }

    Runnable setData = new Runnable() {
        @Override
        public void run() {
            parahsot = new ArrayList<>();
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

}