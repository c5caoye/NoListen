package miaoyipu.nolisten;

import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import miaoyipu.nolisten.music.MusicService;
import miaoyipu.nolisten.music.Song;

public class FullscreenActivity extends AppCompatActivity {
    // We need a Runnable to update the bar regularly.
    private MusicService musicSrv;
    private boolean playbackPaused = false;
    private boolean shuffle = false;
    private SeekBar seekBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);
        musicSrv = Main2Activity.musicSrv;
        seekBar = (SeekBar) findViewById(R.id.music_seekBar);
        handleSeekBar();
        startSeekBar();
    }

    @Override
    protected void onStart() {
        super.onStart();

        setSongTitleView();
        setImageView();
        setPlayButton();

        musicSrv.getMediaPlayer().setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.reset();
                musicSrv.playNext();
                setSongTitleView();
                setImageView();
            }
        });
    }


    private void setPlayButton() {
        if (musicSrv.isPng()) {
            playbackPaused = false;
        } else {
            playbackPaused = true;
            View play_btn = findViewById(R.id.music_play_button);
            play_btn.setBackgroundResource(R.drawable.play_black);
        }
    }

    public void play_pause(View view) {
        if (!playbackPaused) {
            musicSrv.pausePlayer();
            playbackPaused = true;
            setSongTitleView();
            view.setBackgroundResource(R.drawable.play_black);
        } else {
            musicSrv.go();
            playbackPaused = false;
            setSongTitleView();
            view.setBackgroundResource(R.drawable.pause);

            /* start the seekBar */
//            final Handler mHandler = new Handler();
//            this.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    seekBar.setMax((int) musicSrv.getDur());
//                    int currentPosition = musicSrv.getPosn();
//                    seekBar.setProgress(currentPosition);
//                    mHandler.postDelayed(this, 1000);
//                }
//            });
        }
    }

    public void startSeekBar() {
        final Handler mHandler = new Handler();
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                seekBar.setMax(musicSrv.getDur());
                int currentPosition = musicSrv.getPosn();
                seekBar.setProgress(currentPosition);
                mHandler.postDelayed(this, 1000);
            }
        });
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
        playbackPaused = false;
        setSongTitleView();
        Button bt = (Button) findViewById(R.id.music_play_button);
        bt.setBackgroundResource(R.drawable.pause);

        setImageView();
    }

    public void play_next(View view) {
        musicSrv.playNext();
        playbackPaused = false;
        setSongTitleView();
        Button bt = (Button) findViewById(R.id.music_play_button);
        bt.setBackgroundResource(R.drawable.pause);

        setImageView();
    }

    public void setSongTitleView() {
        TextView View = (TextView) findViewById(R.id.song_title_text);
        View.setText(musicSrv.getSongTitle());
    }

    public void setImageView() {
        ImageView view = (ImageView) findViewById(R.id.imageView);
        int posn = musicSrv.getSongPosn();
        Song currSong = musicSrv.getSongs().get(posn);
        Bitmap cover = currSong.getCover();
        cover = Bitmap.createScaledBitmap(cover, 1000, 1000, false);
        view.setImageBitmap(cover);
    }

    /* Set up the seekBar */
    private void handleSeekBar() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (musicSrv.getMediaPlayer() != null && fromUser) {
                    musicSrv.seek(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
}
