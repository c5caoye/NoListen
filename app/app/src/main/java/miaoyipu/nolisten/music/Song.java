package miaoyipu.nolisten.music;

import android.graphics.Bitmap;

/**
 * Created by cy804 on 2017-01-13.
 */

public class Song {
    private long id;
    private String title;
    private String artist;
    //private String cover;
    private Bitmap cover;

    public Song(long songID, String songTitle, String songArtist, Bitmap songCover) {
        id = songID;
        title=songTitle;
        artist=songArtist;
        cover= songCover;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public Bitmap getCover() {
        return cover;
    }
}
