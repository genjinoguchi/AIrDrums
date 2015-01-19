package com.gmt.airdrums;

import android.content.Context;
import android.media.MediaPlayer;

import java.util.Hashtable;

/**
 * Created by genji on 1/17/15.
 */

public class SoundManager {

    private Hashtable<String, MediaPlayer> sounds;
    private Context context;

    public SoundManager(Context context) {
        this.context = context;

        sounds = new Hashtable<String, MediaPlayer>();
        sounds.put("bd1", MediaPlayer.create(context, R.raw.bd1));
        sounds.put("bd2", MediaPlayer.create(context, R.raw.bd2));
        sounds.put("cb", MediaPlayer.create(context, R.raw.cb));
        sounds.put("ch", MediaPlayer.create(context, R.raw.ch));
        sounds.put("cp", MediaPlayer.create(context, R.raw.cp));
        sounds.put("cy1", MediaPlayer.create(context, R.raw.cy1));
        sounds.put("cy2", MediaPlayer.create(context, R.raw.cy2));
        sounds.put("ma", MediaPlayer.create(context, R.raw.ma));
        sounds.put("oh1", MediaPlayer.create(context, R.raw.oh1));
        sounds.put("oh2", MediaPlayer.create(context, R.raw.oh2));
        sounds.put("oh3", MediaPlayer.create(context, R.raw.oh3));
        sounds.put("sd1", MediaPlayer.create(context, R.raw.sd1));
        sounds.put("sd2", MediaPlayer.create(context, R.raw.sd2));
        sounds.put("sd3", MediaPlayer.create(context, R.raw.sd3));
        sounds.put("sd4", MediaPlayer.create(context, R.raw.sd4));

    }

    public void play(String sound) {
        sounds.get(sound).start();
    }


}
