package co.jsaltd.rhat1.countdown;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay;
import com.google.android.things.contrib.driver.pwmspeaker.Speaker;
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat;

import java.io.IOException;
import java.text.DecimalFormat;

public class CountdownActivity extends Activity {
	private static final String TAG = CountdownActivity.class.getSimpleName();

	private static final int DEFAULT_COUNTDOWN = 10000; // 10 seconds
	private static final double C5_SHARP = 554.36;
	private static final double B4 = 493.88;

	private DecimalFormat mNumberFormatter = new DecimalFormat("00.00");

	private boolean mIsRunning;
	private int mRemainingTimeInMillis = DEFAULT_COUNTDOWN;
	private Handler mHandler;

	// hardware
	private AlphanumericDisplay mDisplay;
	private Speaker mSpeaker;
	private Button mButtonA;
	private Button mButtonB;
	private Button mButtonC;

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * lifecycle
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "Countdown Device Started");

		initializeHardware();

		// configure Handler
		HandlerThread handlerThread = new HandlerThread("pwm-playback");
		handlerThread.start();
		mHandler = new Handler(handlerThread.getLooper());
	}

	private void initializeHardware() {
		// initialize display
		try {
			mDisplay = new AlphanumericDisplay(BoardDefaults.getI2cBus());
			mDisplay.setEnabled(true);
			mDisplay.display("RDY?");
			Log.d(TAG, "Initialized I2C Display");
		} catch (IOException e) {
			throw new RuntimeException("Error initializing display", e);
		}

		// initialize buzzer
		try {
			mSpeaker = new Speaker(BoardDefaults.getPwmPin());
			mSpeaker.stop(); // in case the PWM pin was enabled already
		} catch (IOException e) {
			Log.e(TAG, "Error initializing speaker");
			return; // don't initialize the handler
		}

		// initialize capacitive touch buttons
		// Detect Button A press.
		try {
			mButtonA = RainbowHat.openButton(RainbowHat.BUTTON_A);
			mButtonA.setOnButtonEventListener(new Button.OnButtonEventListener() {
				@Override public void onButtonEvent(Button button, boolean pressed) {
					if(!pressed) return;
					Log.d(TAG, "Button A pressed. " + (mIsRunning ? "Stopping…" : "Starting…"));
					if(!mIsRunning) startCountdown();
					else stopCountdown();
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Detect Button B press.
		try {
			mButtonB = RainbowHat.openButton(RainbowHat.BUTTON_B);
			mButtonB.setOnButtonEventListener(new Button.OnButtonEventListener() {
				@Override public void onButtonEvent(Button button, boolean pressed) {
					if(!pressed) return;
					Log.d(TAG, "Button B pressed.");
					mRemainingTimeInMillis += 1000;
					Log.d(TAG, "Adding a second to the countdown.");
					if(!mIsRunning) updateDisplay();
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Detect Button C press.
		try {
			mButtonC = RainbowHat.openButton(RainbowHat.BUTTON_C);
			mButtonC.setOnButtonEventListener(new Button.OnButtonEventListener() {
				@Override public void onButtonEvent(Button button, boolean pressed) {
					if(!pressed) return;
					Log.d(TAG, "Button C pressed.");
					mRemainingTimeInMillis = DEFAULT_COUNTDOWN;
					Log.d(TAG, "Resetting countdown to default.");
					if(!mIsRunning) updateDisplay();
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override protected void onStart() {
		super.onStart();
	}

	@Override protected void onStop() {
		super.onStop();
	}

	@Override protected void onDestroy() {
		super.onDestroy();

		// close the buttons
		if (mButtonA != null) {
			try {
				mButtonA.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (mButtonB != null) {
			try {
				mButtonB.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (mButtonC != null) {
			try {
				mButtonC.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// close display
		if (mDisplay != null) {
			try {
				mDisplay.clear();
				mDisplay.setEnabled(false);
				mDisplay.close();
			} catch (IOException e) {
				Log.e(TAG, "Error closing display", e);
			} finally {
				mDisplay = null;
			}
		}

		// close speaker
		if (mSpeaker != null) {
			try {
				mSpeaker.close();
			} catch (IOException e) {
				Log.e(TAG, "Error closing speaker", e);
			} finally {
				mSpeaker = null;
			}
		}
	}

	private void startCountdown() {
		mIsRunning = true;
		mHandler.post(mPlaybackRunnable);
		mHandler.post(mDecrementCounterRunnable);
	}

	private void stopCountdown() {
		mIsRunning = false;
		mHandler.removeCallbacks(mDecrementCounterRunnable);

		if (mSpeaker != null) {

			try {
				mSpeaker.stop();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * countdown runnable
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private Runnable mDecrementCounterRunnable = new Runnable() {
		@Override public void run() {
			final long DECREMENT_COUNTDOWN_DELAY_IN_MILLIS = 10;
			mRemainingTimeInMillis -= DECREMENT_COUNTDOWN_DELAY_IN_MILLIS;

			if (mRemainingTimeInMillis >= 0) {
				updateDisplay();
				if(mIsRunning) mHandler.postDelayed(mDecrementCounterRunnable, DECREMENT_COUNTDOWN_DELAY_IN_MILLIS);
			} else {
				stopCountdown();
			}
		}
	};

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * display
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private void updateDisplay() {
		try {
			mDisplay.display(mNumberFormatter.format(mRemainingTimeInMillis / 1000f));
			if(mRemainingTimeInMillis % 1000 == 0 && mIsRunning) {
				mHandler.removeCallbacks(mPlaybackRunnable);
				mHandler.post(mPlaybackRunnable);
			}
		} catch (IOException e) {
			Log.d(TAG, "Something bad happened writing to display: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * buzzer
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private Runnable mPlaybackRunnable = new Runnable() {

		@Override public void run() {
			if (mSpeaker == null) return;

			try {
				mSpeaker.stop();

				if (mRemainingTimeInMillis != 0) {
					if((mRemainingTimeInMillis / 1000) % 2 == 0){
						mSpeaker.play(B4);
					} else {
						mSpeaker.play(C5_SHARP);
					}
				}
			} catch (IOException e) {
				Log.e(TAG, "Error playing speaker", e);
			}
		}
	};
}
