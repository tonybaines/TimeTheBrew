package org.tonybaines.coffeetimer;

import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

public class TimeTheBrew extends Activity {
  private enum State {
    BEGIN, RUNNING, STOPPING, COMPLETE
  }

  private static final int DURATION_MS = (1000 * 60 * 4);
  private ProgressBar mProgress;
  private State state = State.BEGIN;

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
            mProgress.setProgress((int) (timeNow() - startTime));
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

  private Runnable alertRunnable;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    mProgress = (ProgressBar) findViewById(R.id.ProgressBar01);
    mProgress.setMax(DURATION_MS);

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

  private void setState(State newState) {
    Log.d(getLocalClassName(), "Moving from State " + state + " to " + newState);
    this.state = newState;
  }

  private void alertWhenCompleteWith(final Handler handler) {
    new Thread(new Runnable() {
      public void run() {
        waitWhileStartingOrRunning();
        if (state == State.COMPLETE)
          handler.post(alertRunnable);
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
