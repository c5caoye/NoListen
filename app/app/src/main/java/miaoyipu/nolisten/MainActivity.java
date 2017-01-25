package miaoyipu.nolisten;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import miaoyipu.nolisten.music.MusicService;
import miaoyipu.nolisten.music.Song;
import miaoyipu.nolisten.music.SongAdapter;

public class MainActivity extends Activity {
    private ArrayList<Song> songList;
    private ListView songView;

    public static MusicService musicSrv;
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

        controllerSetGesture();
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


    @Override
    protected void onResume() {
        super.onResume();
        Button play_btn = (Button) findViewById(R.id.music_play_button);

        if (musicBound) {
            if (musicSrv.isPng()) {
                playbackPaused = false;
                play_btn.setBackgroundResource(R.drawable.pause);
            } else {
                playbackPaused = true;
                play_btn.setBackgroundResource(R.drawable.play_black);
            }

            setSongTitleView();
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

    private void controllerSetGesture() {
        View controllerView = findViewById(R.id.music_controller);
        controllerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = MotionEventCompat.getActionMasked(event);

                switch(action) {
                    case (MotionEvent.ACTION_UP):
                        backToFullScreen();
                        return true;
                }
                return true;
            }
        });
    }

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
        is_start = true;
        if (playbackPaused) {
            playbackPaused=false;
        }
        setSongTitleView();

        toFullscreen();
    }

    public void toFullscreen() {
        Intent intent = new Intent(this, FullscreenActivity.class);
        startActivity(intent);
    }

    public void backToFullScreen(View view) {
        backToFullScreen();
    }

    public void backToFullScreen() {
        if (!is_start) {
            musicSrv.setSong(0);
            is_start = true;
            setSongTitleView();
        }

        toFullscreen();
    }

    public void play_pause(View view) {
        if (!is_start) {
            musicSrv.setSong(0);
            musicSrv.playSong();
            playbackPaused = false;
            is_start = true;
            setSongTitleView();
            view.setBackgroundResource(R.drawable.pause);
        } else if (playbackPaused){
            musicSrv.go();
            playbackPaused=false;
            setSongTitleView();
            view.setBackgroundResource(R.drawable.pause);
        } else if (is_start && !playbackPaused) {
            pause();
            view.setBackgroundResource(R.drawable.play_black);
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

    public void play_prev(View view) {

        playPrev();
        setSongTitleView();
        Button bt = (Button) findViewById(R.id.music_play_button);
        bt.setBackgroundResource(R.drawable.pause);
    }

    public void play_next(View view) {
        playNext();
        setSongTitleView();
        Button bt = (Button) findViewById(R.id.music_play_button);
        bt.setBackgroundResource(R.drawable.pause);
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

    public void pause() {
        musicSrv.pausePlayer();
        playbackPaused=true;
    }


    public void setSongTitleView() {
        TextView View = (TextView) findViewById(R.id.song_title_text);
        View.setText(musicSrv.getSongTitle());
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.super.onBackPressed();
                    }
                })
                .setNegativeButton("No", null)
                .show();


    }

}
