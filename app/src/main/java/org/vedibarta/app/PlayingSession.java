package org.vedibarta.app;

class PlayingSession {

    boolean isPlaying;
    int currentParashPosition, currentTrack, totalTracks, currentDuration, totalDuration;

    PlayingSession(int currentParashPosition, int currentTrack, int totalTracks) {

        this.currentParashPosition = currentParashPosition;
        this.currentTrack = currentTrack;
        this.totalTracks = totalTracks;
    }

}
