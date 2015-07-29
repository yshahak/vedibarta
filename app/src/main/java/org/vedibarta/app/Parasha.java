package org.vedibarta.app;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * Created by yshahak on 20/07/2015.
 */
public class Parasha implements Parcelable{
    String label;
    int totalTracks;
    int lastPlayedTrack;
    int lastPlayedPosition;
    ArrayList<String> paths;
    boolean downloaded;


    /**
     *
     * Constructor to use when re-constructing object
     * from a parcel
     *
     * @param in a parcel from which to read this object
     */
    public Parasha(Parcel in) {
        readFromParcel(in);
    }


    public Parasha(String label, int totalTracks, ArrayList<String> paths) {
        this.label = label;
        this.totalTracks = totalTracks;
        this.paths = paths;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int i) {
        dest.writeString(label);
        dest.writeStringList(paths);
        dest.writeInt(totalTracks);
        dest.writeInt(lastPlayedPosition);
        dest.writeInt(lastPlayedTrack);
        dest.writeByte((byte) (downloaded ? 1 : 0));     //if myBoolean == true, byte == 1
    }

    /**
     *
     * Called from the constructor to create this
     * object from a parcel.
     *
     * @param in parcel from which to re-create object
     */
    private void readFromParcel(Parcel in) {

        // We just need to read back each
        // field in the order that it was
        // written to the parcel
        label = in.readString();
        paths = new ArrayList<String>();
        in.readStringList(paths);
        totalTracks = in.readInt();
        lastPlayedPosition = in.readInt();
        lastPlayedTrack = in.readInt();
        downloaded = in.readByte() != 0;     //myBoolean == true if byte != 0
    }

    /**
     *
     * This field is needed for Android to be able to
     * create new objects, individually or as arrays.
     *
     * This also means that you can use use the default
     * constructor to create the object and use another
     * method to hyrdate it as necessary.
     *
     * I just find it easier to use the constructor.
     * It makes sense for the way my brain thinks ;-)
     *
     */
    public static final Parcelable.Creator CREATOR =
            new Parcelable.Creator() {
                public Parasha createFromParcel(Parcel in) {
                    return new Parasha(in);
                }

                public Parasha[] newArray(int size) {
                    return new Parasha[size];
                }
            };

}
