package miaoyipu.nolisten;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import miaoyipu.nolisten.music.MusicService;
import miaoyipu.nolisten.music.Song;
import miaoyipu.nolisten.music.SongAdapter;

public class MainActivity extends AppCompatActivity {

    private ArrayList<Song> songList;
    private ListView songView;

    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound = false;

    private boolean paused = false, playbackPaused = false;

    private boolean is_start = false;
    private boolean shuffle = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        songView = (ListView)findViewById(R.id.song_list);
        songList = new ArrayList<Song>();
        getSongList();

        // Sort songs alphabetically
        Collections.sort(songList, new Comparator<Song>() {
            @Override
            public int compare(Song o1, Song o2) {
                return o1.getTitle().compareTo(o2.getTitle());
            }
        });

        SongAdapter songAda = new SongAdapter(this, songList);
        songView.setAdapter(songAda);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    private ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicSrv = binder.getService();
            musicSrv.setList(songList);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    public void getSongList() {
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null,
                null, null, null);

        if(musicCursor != null && musicCursor.moveToFirst()) {
            //get columns
            int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int coverColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);

            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                long thisCover = musicCursor.getLong(coverColumn);

                Uri coverUri = Uri.parse("content://media/external/audio/albumart");
                Uri artUri = ContentUris.withAppendedId(coverUri, thisCover);

                Bitmap bitmap = null;

                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), artUri);
                } catch (FileNotFoundException exception) {
                    exception.printStackTrace();
                    bitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.icon);
                } catch (IOException e) {e.printStackTrace();}


                songList.add(new Song(thisId, thisTitle, thisArtist, bitmap));
            }
            while (musicCursor.moveToNext());
        }
    }

    public void songPicked(View view) {
        musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
        musicSrv.playSong();
        if (playbackPaused) {
            playbackPaused=false;
        }
    }

    public void play_music(View view) {
        if (!is_start) {
            musicSrv.setSong(0);
            musicSrv.playSong();

            playbackPaused = false;
            is_start = true;
        }else if (playbackPaused) {
            musicSrv.go();
            playbackPaused=false;
        }
    }

    public void shuffle(View view) {

        musicSrv.setShuffle();
        if (!shuffle) {
            shuffle = true;
            Toast.makeText(getApplicationContext(), "shuffle on", Toast.LENGTH_SHORT).show();
        }
        else {
            shuffle = false;
            Toast.makeText(getApplicationContext(), "shuffle off", Toast.LENGTH_SHORT).show();
        }
    }

    public void pause_music(View view) {
        if (is_start && !playbackPaused) {
            pause();
            Toast.makeText(getApplicationContext(), "music paused", Toast.LENGTH_SHORT).show();
        }
    }

    public void play_prev(View view) {
        playPrev();
    }

    public void play_next(View view) {
        playNext();
    }



    private void playNext() {
        musicSrv.playNext();
        if (playbackPaused) {
            playbackPaused=false;
        }
    }

    private void playPrev() {
        musicSrv.playPrev();
        if (playbackPaused) {
            playbackPaused=false;
        }
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_shuffle:
                musicSrv.setShuffle();
                break;
            case R.id.action_end:
                stopService(playIntent);
                musicSrv=null;
                System.exit(0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        stopService(playIntent);
        musicSrv=null;
        super.onDestroy();
    }

    public void start() {
        musicSrv.go();
        playbackPaused = false;
    }

    public void pause() {
        musicSrv.pausePlayer();
        playbackPaused=true;
    }

    public int getDuration() {
        if (musicSrv!= null && musicBound && musicSrv.isPng()) {
            return musicSrv.getDur();
        }
        else return 0;
    }

    public int getCurrentPosition() {
        if(musicSrv!=null && musicBound && musicSrv.isPng()) {
            return musicSrv.getPosn();
        }
        else return 0;
    }

    public void seekTo(int pos) {
        musicSrv.seek(pos);
    }

    public boolean isPlaying() {
        if (musicSrv!=null && musicBound) {
            return musicSrv.isPng();
        } else return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (paused) {
            paused = false;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}
