package miaoyipu.nolisten.music;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import miaoyipu.nolisten.R;

/**
 * Map the songs to the list view
 */

public class SongAdapter extends BaseAdapter {

    private ArrayList<Song> songs;
    private LayoutInflater songInf;

    public SongAdapter(Context c, ArrayList<Song> theSongs) {
        songs = theSongs;
        songInf = LayoutInflater.from(c);
    }

    @Override
    public int getCount() {
        return songs.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //map to song layout;
        LinearLayout songLay = (LinearLayout)songInf.inflate(R.layout.song, parent, false);

        ImageView coverView = (ImageView)songLay.findViewById(R.id.song_cover);
        TextView songView = (TextView)songLay.findViewById(R.id.song_title);
        TextView artistView = (TextView)songLay.findViewById(R.id.song_artist);

        Song currSong = songs.get(position);

        Bitmap bm = currSong.getCover();
        bm = Bitmap.createScaledBitmap(bm, 300, 300, false);
        coverView.setImageBitmap(bm);
        songView.setText(currSong.getTitle());
        artistView.setText(currSong.getArtist());

        //set position as tag
        songLay.setTag(position);

        return songLay;
    }
}
