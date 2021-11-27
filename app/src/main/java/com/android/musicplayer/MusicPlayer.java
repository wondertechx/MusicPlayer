package com.android.musicplayer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.palette.graphics.Palette;
import de.hdodenhof.circleimageview.CircleImageView;
import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.method.MetaKeyKeyListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.musicplayer.Model.MusicFiles;
import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Random;

import static com.android.musicplayer.ApplicationClass.ACTION_NEXT;
import static com.android.musicplayer.ApplicationClass.ACTION_PLAY;
import static com.android.musicplayer.ApplicationClass.ACTION_PREVIOUS;
import static com.android.musicplayer.ApplicationClass.CHANNEL_ID_2;
import static com.android.musicplayer.MainActivity.musicFiles;
import static com.android.musicplayer.MainActivity.repeatBoolean;
import static com.android.musicplayer.MainActivity.shuffleBoolean;
import static com.android.musicplayer.adapter.MusicAdapter.mfiles;

public class MusicPlayer extends AppCompatActivity
        implements ActionPlaying, ServiceConnection {

    TextView songName, artistName, durationStart, durationEnd;
    ImageView back, options, previous, repeat, next, shuffle, heart;
    CircleImageView coverPhoto;
    CircleImageView blurPhoto;
    FloatingActionButton playPauseButton;
    SeekBar seekerbar;
    int position = -1;
    static ArrayList<MusicFiles> songList = new ArrayList<>();
    static Uri uri;
    //static MediaPlayer mediaPlayer;
    private Handler handler = new Handler();
    private Thread playThread, prevThread, nextThread;
    private View view1, view2, view3, view4;
    private BlurView blurView;
    MusicService musicService;
    MediaSessionCompat mediaSessionCompat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFullScreen();
        setContentView(R.layout.activity_music_player);
        getSupportActionBar().hide();

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        mediaSessionCompat = new MediaSessionCompat(getBaseContext(),"my audio");
        init();
        getIntentMethod();
        blurViews();

        back.setOnClickListener(view -> MusicPlayer.super.onBackPressed());

        heart.setOnClickListener(view -> {
            heart.setImageResource(R.drawable.heart_filled);
        });

        shuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (shuffleBoolean) {
                    shuffleBoolean = false;
                    shuffle.setImageResource(R.drawable.shuffle);
                } else {
                    shuffleBoolean = true;
                    shuffle.setImageResource(R.drawable.ic_shuffle_on);
                }
            }
        });

        repeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (repeatBoolean) {
                    repeatBoolean = false;
                    repeat.setImageResource(R.drawable.repeat);

                } else {
                    repeatBoolean = true;
                    repeat.setImageResource(R.drawable.ic_repeat_on);
                }
            }
        });


        seekerbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (musicService != null && b) {
                    musicService.seekTo(i * 1000);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        MusicPlayer.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (musicService != null) {
                    int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                    seekerbar.setProgress(mCurrentPosition);
                    durationEnd.setText(formattedTime(mCurrentPosition));
                }
                handler.postDelayed(this, 1000);
            }
        });


    }

    private void setFullScreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    protected void onResume() {

        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, this, BIND_AUTO_CREATE);
        playThreadBtn();
        nextThreadBtn();
        prevThreadBtn();

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(this);
    }

    private void prevThreadBtn() {
        prevThread = new Thread() {
            @Override
            public void run() {
                super.run();
                previous.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        prevBtnClicked();
                    }
                });
            }
        };
        prevThread.start();
    }

    public void prevBtnClicked() {
        if (musicService.isPlaying()) {
            musicService.stop();
            musicService.release();
            if (shuffleBoolean && !repeatBoolean) {
                position = getRandom(songList.size() - 1);
            } else if (!shuffleBoolean && !shuffleBoolean) {
                position = ((position - 1) < 0 ? (songList.size() - 1) : (position - 1));
            }

            uri = Uri.parse(songList.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            songName.setText(songList.get(position).getTitle());
            artistName.setText(songList.get(position).getArtist());
            seekerbar.setMax(musicService.getDuration() / 1000);
            MusicPlayer.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null) {
                        int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekerbar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000);
                }
            });
            musicService.onCompleted();
            musicService.showNotifcation(R.drawable.ic_pause);
            playPauseButton.setBackgroundResource(R.drawable.ic_pause);
            musicService.start();

        } else {
            musicService.stop();
            musicService.release();
            if (shuffleBoolean && !repeatBoolean) {
                position = getRandom(songList.size() - 1);
            } else if (!shuffleBoolean && !shuffleBoolean) {
                position = ((position - 1) < 0 ? (songList.size() - 1) : (position - 1));
            }
            uri = Uri.parse(songList.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            songName.setText(songList.get(position).getTitle());
            artistName.setText(songList.get(position).getArtist());
            seekerbar.setMax(musicService.getDuration() / 1000);
            MusicPlayer.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null) {
                        int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekerbar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000);
                }
            });
            musicService.onCompleted();
            musicService.showNotifcation(R.drawable.ic_play);
            playPauseButton.setBackgroundResource(R.drawable.ic_play);
            blurViews();
        }
    }

    public void nextThreadBtn() {
        nextThread = new Thread() {
            @Override
            public void run() {
                super.run();
                next.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        nextBtnClicked();
                    }
                });
            }
        };
        nextThread.start();
    }

    public void nextBtnClicked() {
        if (musicService.isPlaying()) {
            musicService.stop();
            musicService.release();
            if (shuffleBoolean && !repeatBoolean) {
                position = getRandom(songList.size() - 1);
            } else if (!shuffleBoolean && !shuffleBoolean) {
                position = ((position + 1) % songList.size());
            }

            uri = Uri.parse(songList.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            songName.setText(songList.get(position).getTitle());
            artistName.setText(songList.get(position).getArtist());
            seekerbar.setMax(musicService.getDuration() / 1000);
            MusicPlayer.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null) {
                        int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekerbar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000);
                }
            });
            musicService.onCompleted();
            musicService.showNotifcation(R.drawable.ic_pause);
            playPauseButton.setBackgroundResource(R.drawable.ic_pause);
            musicService.start();

        } else {
            musicService.stop();
            musicService.release();
            if (shuffleBoolean && !repeatBoolean) {
                position = getRandom(songList.size() - 1);
            } else if (!shuffleBoolean && !shuffleBoolean) {
                position = ((position + 1) % songList.size());
            }
            uri = Uri.parse(songList.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            songName.setText(songList.get(position).getTitle());
            artistName.setText(songList.get(position).getArtist());
            seekerbar.setMax(musicService.getDuration() / 1000);
            MusicPlayer.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null) {
                        int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekerbar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000);
                }
            });
            musicService.onCompleted();
            musicService.showNotifcation(R.drawable.ic_play);
            playPauseButton.setBackgroundResource(R.drawable.ic_play);
            blurViews();
        }
    }

    private int getRandom(int i) {
        Random random = new Random();

        return random.nextInt(i + 1);
    }

    private void playThreadBtn() {
        playThread = new Thread() {
            @Override
            public void run() {
                super.run();
                playPauseButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        playPauseButtonBtnClicked();
                    }
                });
            }
        };
        playThread.start();
    }

    public void playPauseButtonBtnClicked() {
        if (musicService.isPlaying()) {
            playPauseButton.setImageResource(R.drawable.ic_play);
            musicService.showNotifcation(R.drawable.ic_play);
            musicService.pause();
            seekerbar.setMax(musicService.getDuration() / 1000);
            MusicPlayer.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null) {
                        int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekerbar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000);
                }
            });

        } else {
            musicService.showNotifcation(R.drawable.ic_pause);
            playPauseButton.setImageResource(R.drawable.ic_pause);
            musicService.start();
            seekerbar.setMax(musicService.getDuration() / 1000);
            MusicPlayer.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null) {
                        int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekerbar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000);
                }
            });
        }
    }

    private String formattedTime(int mCurrentPosition) {
        String totalout = "";
        String totalnew = "";
        String seconds = String.valueOf(mCurrentPosition % 60);
        String minutes = String.valueOf(mCurrentPosition / 60);
        totalout = minutes + ":" + seconds;
        totalnew = minutes + ":" + "0" + seconds;
        if (seconds.length() == 1) {
            return totalnew;
        } else {
            return totalout;
        }

    }

    private void getIntentMethod() {
        position = getIntent().getIntExtra("position", -1);
        songList = mfiles;
        if (songList != null) {
            playPauseButton.setBackgroundResource(R.drawable.ic_pause);
            uri = Uri.parse(songList.get(position).getPath());

        }
        if (musicService != null) {
            musicService.stop();
            musicService.release();
            musicService.createMediaPlayer(position);
            musicService.start();
        } else {
            musicService.createMediaPlayer(position);
            musicService.start();
        }

        Intent intent = new Intent(this, MusicService.class);
        intent.putExtra("servicePosition", position);
        startService(intent);

    }

    private void init() {
        songName = findViewById(R.id.songName);
        artistName = findViewById(R.id.artistName);
        durationStart = findViewById(R.id.startTv);
        durationEnd = findViewById(R.id.endTv);
        coverPhoto = findViewById(R.id.songPhoto);
        back = findViewById(R.id.back);
        options = findViewById(R.id.imageView2);
        previous = findViewById(R.id.previous);
        repeat = findViewById(R.id.repeat);
        next = findViewById(R.id.next);
        shuffle = findViewById(R.id.shuffle);
        playPauseButton = findViewById(R.id.playButton);
        seekerbar = findViewById(R.id.seekBar);
        heart = findViewById(R.id.heart);
        view1 = findViewById(R.id.view);
        view2 = findViewById(R.id.view2);
        view3 = findViewById(R.id.view3);
        view4 = findViewById(R.id.view4);
        blurView = findViewById(R.id.blurView);
        blurPhoto = findViewById(R.id.blurPhoto);
    }

    private void metaData(Uri uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(uri.toString());
        int durationTotal = Integer.parseInt(songList.get(position).getDuration()) / 1000;
        durationEnd.setText(formattedTime(durationTotal));
        byte[] art = retriever.getEmbeddedPicture();
        Bitmap bitmap;
        if (art != null) {

            bitmap = BitmapFactory.decodeByteArray(art, 0, art.length);
            ImageAnimate(this, coverPhoto, bitmap);
            Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(@Nullable Palette palette) {
                    Palette.Swatch swatch1 = palette.getDominantSwatch();
                    Palette.Swatch swatch2 = palette.getVibrantSwatch();
                    if (swatch1 != null) {
                        view2.setBackgroundColor(palette.getDominantColor(1));
                        view3.setBackgroundColor(palette.getDominantColor(1));
                    } else {
                        view2.setBackgroundColor(getResources().getColor(R.color.primary));
                        view3.setBackgroundColor(getResources().getColor(R.color.secondary));
                    }
                    if (swatch2 != null) {
                        view1.setBackgroundColor(palette.getVibrantColor(1));
                        view4.setBackgroundColor(palette.getVibrantColor(1));
                    } else {
                        view1.setBackgroundColor(getResources().getColor(R.color.primary));
                        view4.setBackgroundColor(getResources().getColor(R.color.secondary));
                    }


                }
            });

        } else {
            Glide.with(this)
                    .asBitmap()
                    .load(R.drawable.music_image)
                    .into(coverPhoto);
        }
    }

    private void blurViews() {
        float radius = 25f;

        View decorView = getWindow().getDecorView();
        //ViewGroup you want to start blur from. Choose root as close to BlurView in hierarchy as possible.
        ViewGroup rootView = (ViewGroup) decorView.findViewById(android.R.id.content);
        //Set drawable to draw in the beginning of each blurred frame (Optional).
        //Can be used in case your layout has a lot of transparent space and your content
        //gets kinda lost after after blur is applied.
        Drawable windowBackground = decorView.getBackground();

        blurView.setupWith(rootView)
                .setFrameClearDrawable(windowBackground)
                .setBlurAlgorithm(new RenderScriptBlur(this))
                .setBlurRadius(radius)
                .setBlurAutoUpdate(true)
                .setHasFixedTransformationMatrix(true); // Or false if it's in a scrolling container or might be animated
    }

    private void ImageAnimate(Context context, ImageView imageView, Bitmap bitmap) {
        Animation animOut = AnimationUtils.loadAnimation(context, android.R.anim.fade_out);
        Animation animIn = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
        animOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Glide.with(context).load(bitmap).into(imageView);
                animIn.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                imageView.startAnimation(animIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        imageView.startAnimation(animOut);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        MusicService.MyBinder myBinder = (MusicService.MyBinder) iBinder;
        musicService = myBinder.getService();
        musicService.setCallBack(this);
        seekerbar.setMax(musicService.getDuration() / 1000);
        metaData(uri);
        songName.setText(songList.get(position).getTitle());
        artistName.setText(songList.get(position).getArtist());
        musicService.onCompleted();
        musicService.showNotifcation(R.drawable.ic_pause);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        musicService = null;
    }

}