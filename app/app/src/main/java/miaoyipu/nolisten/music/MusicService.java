package miaoyipu.nolisten.music;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Random;

import miaoyipu.nolisten.MainActivity;
import miaoyipu.nolisten.R;

/**
 * Created by cy804 on 2017-01-13.
 */
public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener,
        AudioManager.OnAudioFocusChangeListener {

    private final IBinder musicBind = new MusicBinder();

    private MediaPlayer player;
    private String mediaFile; //path to the audio file.
    private ArrayList<Song> songs;
    private int songPosn; //current position.

    private String songTitle = "";
    private static final int NOTIFY_ID = 1;

    private boolean shuffle=false;
    private Random rand;

    public void onCreate() {
        super.onCreate(); //create the service
        songPosn = 0;
        player = new MediaPlayer();
        initMusicPlayer();

        rand = new Random();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        player.stop();
        player.release();
        return false;
    }

    @Override
    /* Invoked when a playback of a media source has completed*/
    public void onCompletion(MediaPlayer mp) {
        if(player.getCurrentPosition() > 0) {
            mp.reset();
            playNext();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();

        Intent noIntent = new Intent(this, MainActivity.class);
        noIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivities(this, 0, new Intent[]{noIntent}, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this);

        builder.setContentIntent(pendInt)
                .setSmallIcon(R.drawable.play)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentText("Now Playing")
                .setContentText(songTitle);
        Notification not = builder.build();

        startForeground(NOTIFY_ID, not);
    }

    @Override
    /* Invoked when there has been an error during an asy. operation */
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
        return false;
    }

    public void initMusicPlayer() {
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
        player.setOnBufferingUpdateListener(this);
        player.setOnSeekCompleteListener(this);
        player.setOnInfoListener(this);


        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    public void setList(ArrayList<Song> theSongs) {
        songs = theSongs;
    }

    @Override
    /* Invoked when the audio focus of the system is updated */
    public void onAudioFocusChange(int focusChange) {

    }

    @Override
    /* Invoked indicating buffering status of a media
        resource being streamed over the network
     */
    public void onBufferingUpdate(MediaPlayer mp, int percent) {

    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    /*Invoked indicating the completion of a seek operation */
    public void onSeekComplete(MediaPlayer mp) {

    }

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    public void playSong() {
        player.reset();
        Song playSong = songs.get(songPosn);

        songTitle = playSong.getTitle();
        long currSong = playSong.getId();
        Uri trackUri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, currSong
        );

        try{
            player.setDataSource(getApplicationContext(), trackUri);
        } catch (Exception e) {
            Log.e("MUSIC SERVICE", "ERROR setting data source", e);
        }

        player.prepareAsync();
    }

    public void setSong(int songIndex) {
        songPosn = songIndex;
    }

    public int getPosn() {
        return player.getCurrentPosition();
    }

    public int getDur() {
        return player.getDuration();
    }

    public boolean isPng() {
        return player.isPlaying();
    }

    public void pausePlayer() {
        player.pause();
    }

    public void seek(int posn) {
        player.seekTo(posn);
    }

    public void go() {
        player.start();
    }

    public void playPrev() {
        songPosn--;
        if (songPosn<0) songPosn=songs.size()- 1;
        playSong();
    }

    public void playNext() {
        if (shuffle) {
            int newSong = songPosn;
            while (newSong == songPosn) {
                newSong = rand.nextInt(songs.size());
            }
            songPosn = newSong;
        } else {
            songPosn++;
            if (songPosn >= songs.size()) songPosn = 0;
        }
        playSong();
    }

    public void setShuffle() {
        if (shuffle) shuffle = false;
        else shuffle = true;
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
    }
}
