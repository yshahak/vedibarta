package org.vedibarta.app;

import java.util.ArrayList;

/**
 * Created by yshahak on 20/07/2015.
 */
public class Parasha {
    String label;
    int totalTracks;
    int lastPlayedTrack;
    int lastPlayedPosition;
    ArrayList<String> paths;

    public Parasha(String label, int totalTracks, ArrayList<String> paths) {
        this.label = label;
        this.totalTracks = totalTracks;
        this.paths = paths;
    }
}
