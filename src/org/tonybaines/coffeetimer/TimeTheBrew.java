package org.tonybaines.coffeetimer;

import java.io.IOException;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class TimeTheBrew extends Activity {
  private enum State {
    BEGIN, RUNNING, STOPPING, COMPLETE
  }

  private static final int DURATION_MS = (1000 * 60 * 4);
  private ProgressBar mProgress;
  private TextView mTextView;
  private State state = State.BEGIN;
  private Runnable alertRunnable;

  private PowerManager.WakeLock wl;

  final Handler handler = new Handler();

  private final OnClickListener goListener = new View.OnClickListener() {
    public void onClick(View v) {
      resetProgress();
      setButtonStates(false);
      final long startTime = timeNow();
      new Thread(new Runnable() {
        public void run() {
          setState(State.RUNNING);
          while (state == State.RUNNING && timeNow() < startTime + DURATION_MS) {
            safeSleep(100);
            int elapsedTimeMs = (int) (timeNow() - startTime);
            mProgress.setProgress(elapsedTimeMs);
            //mTextView.setText((int)((DURATION_MS - elapsedTimeMs)/1000) + "s");
          }
          if (state != State.STOPPING)
            setState(State.COMPLETE);
          else
            resetProgress();
        }
      }).start();
    }
  };

  private final OnClickListener stopListener = new View.OnClickListener() {
    public void onClick(View v) {
      stopTheClock();
      resetProgress();
    }
  };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    super.setContentView(R.layout.main);
    
    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);  
    wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "DoNotDimScreen");  

    mProgress = (ProgressBar) findViewById(R.id.ProgressBar01);
    mProgress.setMax(DURATION_MS);

    mTextView = (TextView) findViewById(R.id.TextView01);
    mTextView.setText(DURATION_MS/1000 + "s");
    
    Button goButton = (Button) findViewById(R.id.Button_Go);
    goButton.setOnClickListener(goListener);

    Button stopButton = (Button) findViewById(R.id.Button_Stop);
    stopButton.setOnClickListener(stopListener);

    resetProgress();

    final Dialog alertDialog = new AlertDialog.Builder(this)
        .setMessage("Time's up").setCancelable(false)
        .setPositiveButton("OK", null).create();

    alertRunnable = new Runnable() {
      @Override
      public void run() {
        alertDialog.show();
      }
    };

    alertWhenCompleteWith(handler);
  }

  @Override
  protected void onPause() {
    super.onPause();
    wl.release();
  }

  @Override
  protected void onResume() {
    super.onResume();
    wl.acquire();
  }

  private void setState(State newState) {
    Log.d(getLocalClassName(), "Moving from State " + state + " to " + newState);
    this.state = newState;
  }

  private void alertWhenCompleteWith(final Handler handler) {
    new Thread(new Runnable() {
      public void run() {
        waitWhileStartingOrRunning();
        if (state == State.COMPLETE) {
          try {
            handler.post(alertRunnable);
            ring();
            vibrate();
          } catch (Throwable e) {
            Log.e(getLocalClassName(), "Ooops", e);
          }
        }
        resetProgress();
      }

      private void waitWhileStartingOrRunning() {
        while (state == State.BEGIN || state == State.RUNNING
            || state == State.STOPPING) {
          safeSleep(100);
        }
      }
    }).start();
  }

  private final void vibrate() {
    Vibrator mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    mVibrator.vibrate(1000);
  }

  private final void ring() {
    try {
      Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
      MediaPlayer player = new MediaPlayer();
      player.setDataSource(this, alert);
      final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
      if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
        player.setAudioStreamType(AudioManager.STREAM_ALARM);
        player.setLooping(false);
        player.prepare();
        player.start();
      }
    } catch (IllegalStateException e) {
      Log.e(getLocalClassName(), "Failed to ring", e);
    } catch (IOException e) {
      Log.e(getLocalClassName(), "Failed to ring", e);
    }
  }

  private void resetProgress() {
    setButtonStates(true);
    mProgress.setProgress(0);
    setState(State.BEGIN);
  }

  private void stopTheClock() {
    setState(State.STOPPING);
    while (state == State.STOPPING) {
      safeSleep(100);
    }
  }

  private void safeSleep(long duration) {
    try {
      Thread.sleep(duration);
    } catch (InterruptedException e) {
      // Ignored
    }
  }

  private long timeNow() {
    return (new Date()).getTime();
  }

  private void setButtonStates(boolean goActive) {
    Button goButton = (Button) findViewById(R.id.Button_Go);
    goButton.setClickable(goActive);
    Button stopButton = (Button) findViewById(R.id.Button_Stop);
    stopButton.setClickable(!goActive);
  }
}
