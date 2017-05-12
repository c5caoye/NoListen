package miaoyipu.nolisten;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
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

public class Main2Activity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static MusicService musicSrv;

    private ArrayList<Song> songList;
    private ListView songView;
    private Intent playIntent;
    private boolean musicBound = false;
    private boolean paused = false;
    private boolean is_start = false;
    private boolean shuffle = false;
    private static final int PERMISSON_REQUEST_READ_STORAGE = 11;
    private ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicSrv = binder.getService();
            musicSrv.setList(songList);
            musicBound = true;

            musicSrv.getMediaPlayer().setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.reset();
                    musicSrv.playNext();
                    setSongTitleView();
                }
             });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {musicBound = false;}
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        int readStorageCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (readStorageCheck == PackageManager.PERMISSION_DENIED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSON_REQUEST_READ_STORAGE);

        } else {
            setSongAdapter();
        }


        // Define float button action
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shuffle(view);
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        setVolumeControlStream(AudioManager.STREAM_MUSIC); // This connects volume control to STREAM_MUSIC whenever the target activity or fragment is visible.
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!musicBound) {
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
                play_btn.setBackgroundResource(R.drawable.pause);
            } else {
                play_btn.setBackgroundResource(R.drawable.play_black);
            }

            setSongTitleView();

            musicSrv.getMediaPlayer().setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.reset();
                    musicSrv.playNext();
                    setSongTitleView();
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(playIntent);
        unbindService(musicConnection);
        musicSrv = null;
        musicBound = false;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            new AlertDialog.Builder(this)
                    .setMessage("Are you sure you want to exit?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Main2Activity.super.onBackPressed();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSON_REQUEST_READ_STORAGE: {
                // If request is cancelled, thre result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setSongAdapter();
                } else {
                    Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main2, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void setSongAdapter() {
        songList = new ArrayList<Song>();
        getSongList();

        // Sort songs alphabetically
        Collections.sort(songList, new Comparator<Song>() {
            @Override
            public int compare(Song o1, Song o2) {
                return o1.getTitle().compareTo(o2.getTitle());
            }
        });

        SongAdapter songAdapter = new SongAdapter(this, songList);
        songView = (ListView)findViewById(R.id.song_list);
        songView.setAdapter(songAdapter);
    }


    private void getSongList() {
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
        if (paused) {
            paused=false;
        }
        setSongTitleView();
    }

    public void backToFullScreen(View view) {
        backToFullScreen();
    }

    public void backToFullScreen() {
        if (songList.size() > 0) {
            if (!is_start) {
                musicSrv.setSong(0);
                is_start = true;
                setSongTitleView();
            }

            Intent intent = new Intent(this, FullscreenActivity.class);
            startActivity(intent);
        } else {
            Toast.makeText(getApplicationContext(), "No song selected", Toast.LENGTH_SHORT).show();
        }
    }


    public void play_pause(View view) {
        if (songList.size() == 0) {
            Toast.makeText(getApplicationContext(), "No song selected", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!is_start) {
            musicSrv.setSong(0);
            musicSrv.playSong();
            paused = false;
            is_start = true;
            setSongTitleView();
            view.setBackgroundResource(R.drawable.pause);
        } else if (paused){
            musicSrv.go();
            paused=false;
            setSongTitleView();
            view.setBackgroundResource(R.drawable.pause);
        } else if (is_start && !paused) {
            musicSrv.pausePlayer();
            paused=true;
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
        musicSrv.playPrev();
        if (paused) paused = false;
        setSongTitleView();
        Button bt = (Button) findViewById(R.id.music_play_button);
        bt.setBackgroundResource(R.drawable.pause);
    }

    public void play_next(View view) {
        musicSrv.playNext();
        if (paused) paused = false;
        setSongTitleView();
        Button bt = (Button) findViewById(R.id.music_play_button);
        bt.setBackgroundResource(R.drawable.pause);
    }


    public void setSongTitleView() {
        TextView View = (TextView) findViewById(R.id.song_title_text);
        View.setText(musicSrv.getSongTitle());
    }

}
