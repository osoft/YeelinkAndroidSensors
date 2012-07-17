package net.zalio.android.yl.sensors;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
//import android.util.Config;
import android.util.Log;

public class SamplingService extends Service implements SensorEventListener {
	static final String LOG_TAG = "SAMPLINGSERVICE";
	static final boolean KEEPAWAKE_HACK = false;
	static final boolean MINIMAL_ENERGY = false;
	static final long MINIMAL_ENERGY_LOG_PERIOD = 15000L;
	private static final int NOTIFICATION = 12345;
	private NotificationManager mNM;
	private LocationListener gpsListener = null;
	private LocationListener networkListener = null;
	// 变量定义
	private LocationManager locationManager;

	@Override
	public void onCreate() {
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// Display a notification about us starting. We put an icon in the
		// status bar.
		showNotification();
	}

	/**
	 * Show a notification while this service is running.
	 */
	@SuppressWarnings("deprecation")
	private void showNotification() {
		// In this sample, we'll use the same text for the ticker and the
		// expanded notification
		CharSequence text = getString(R.string.textview_sampling);

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.ic_launcher,
				text, System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this
		// notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, Sensors.class), 0);

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this, text, text, contentIntent);
		notification.flags |= Notification.FLAG_NO_CLEAR
				| Notification.FLAG_ONGOING_EVENT;
		// Send the notification.
		mNM.notify(text.toString(), NOTIFICATION, notification);
	}

	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent == null) {
			stopSelf();
			return START_REDELIVER_INTENT;
		}
		super.onStartCommand(intent, flags, startId);
		if (Sensors.DEBUG)
			Log.d(LOG_TAG, "onStartCommand");
		stopSampling(); // just in case the activity-level service management
						// fails
		sensorName = intent.getStringExtra("sensorname");
		if (Sensors.DEBUG)
			Log.d(LOG_TAG, "sensorName: " + sensorName);
		SharedPreferences appPrefs = getSharedPreferences(Sensors.PREF_FILE,
				MODE_PRIVATE);
		rate = appPrefs.getInt(Sensors.PREF_SAMPLING_SPEED,
				SensorManager.SENSOR_DELAY_NORMAL);
		if (Sensors.DEBUG)
			Log.d(LOG_TAG, "rate: " + rate);

		screenOffBroadcastReceiver = new ScreenOffBroadcastReceiver();
		IntentFilter screenOffFilter = new IntentFilter();
		screenOffFilter.addAction(Intent.ACTION_SCREEN_OFF);
		if (KEEPAWAKE_HACK)
			registerReceiver(screenOffBroadcastReceiver, screenOffFilter);

		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		startSampling();

		if (Sensors.DEBUG)
			Log.d(LOG_TAG, "onStartCommand ends");
		// return START_NOT_STICKY;
		return START_STICKY;
	}

	public void onDestroy() {
		Log.i(LOG_TAG, "SamplingService onDestroy()");
		super.onDestroy();
		if (Sensors.DEBUG)
			Log.d(LOG_TAG, "onDestroy");
		stopSampling();
		if (KEEPAWAKE_HACK)
			unregisterReceiver(screenOffBroadcastReceiver);

		SharedPreferences appPrefs = getSharedPreferences(Sensors.PREF_FILE,
				MODE_PRIVATE);
		Editor ed = appPrefs.edit();
		ed.putString("currentSensor", "");
		ed.commit();
		// Cancel the persistent notification.
		mNM.cancelAll();
	}

	public IBinder onBind(Intent intent) {
		return null; // cannot bind
	}

	// SensorEventListener
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@SuppressWarnings("deprecation")
	public void onSensorChanged(SensorEvent sensorEvent) {
		++logCounter;
		if (!MINIMAL_ENERGY) {
			if (Sensors.DEBUG) {
				StringBuilder b = new StringBuilder();
				for (int i = 0; i < sensorEvent.values.length; ++i) {
					if (i > 0)
						b.append(" , ");
					b.append(Float.toString(sensorEvent.values[i]));
				}
				Log.d(LOG_TAG, sensorName + ": " + new Date().toLocaleString()
						+ " [" + b + "]");
				YeelinkReport.reportSensorData(sensorName, ourSensor.getType(),
						sensorEvent.values);
			}
			if (captureFile != null) {
				captureFile.print(Long.toString(sensorEvent.timestamp));
				for (int i = 0; i < sensorEvent.values.length; ++i) {
					captureFile.print(",");
					captureFile.print(Float.toString(sensorEvent.values[i]));
				}
				captureFile.println();
			}
		} else {
			++logCounter;
			if ((logCounter % MINIMAL_ENERGY_LOG_PERIOD) == 0L)
				Log.d(LOG_TAG, "logCounter: " + logCounter + " at "
						+ new Date().toString());
		}
	}

	private void stopSampling() {
		if (!samplingStarted)
			return;
		if (generateUserActivityThread != null) {
			generateUserActivityThread.stopThread();
			generateUserActivityThread = null;
		}
		if (sensorManager != null) {
			// if( Config.DEBUG )
			// Log.d( LOG_TAG, "unregisterListener/SamplingService" );
			sensorManager.unregisterListener(this);
		}

		// Added to stop GPS tracking
		if (gpsListener != null) {
			locationManager.removeUpdates(gpsListener);
			gpsListener = null;
		}

		if (captureFile != null) {
			captureFile.close();
			captureFile = null;
		}
		samplingStarted = false;
		sampingInProgressWakeLock.release();
		sampingInProgressWakeLock = null;
		Date samplingStoppedTimeStamp = new Date();
		long secondsEllapsed = (samplingStoppedTimeStamp.getTime() - samplingStartedTimeStamp
				.getTime()) / 1000L;
		Log.d(LOG_TAG,
				"Sampling started: " + samplingStartedTimeStamp.toString()
						+ "; Sampling stopped: "
						+ samplingStoppedTimeStamp.toString() + " ("
						+ secondsEllapsed + " seconds) "
						+ "; samples collected: " + logCounter);
	}

	private void startSampling() {
		if (samplingStarted)
			return;
		if (sensorName != null) {
			List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
			ourSensor = null;

			for (int i = 0; i < sensors.size(); ++i) {
				if (sensorName.equals(sensors.get(i).getName())) {
					ourSensor = sensors.get(i);
					break;
				}
			}

			if (ourSensor != null) {
				if (Sensors.DEBUG)
					Log.d(LOG_TAG, "registerListener/SamplingService");
				sensorManager.registerListener(this, ourSensor, rate);
			} else if (sensorName.equals("GPS")) {
				registerLocationListener();
			}
			samplingStartedTimeStamp = new Date();
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			sampingInProgressWakeLock = pm.newWakeLock(
					PowerManager.PARTIAL_WAKE_LOCK, "SamplingInProgress");
			sampingInProgressWakeLock.acquire();
			// captureFile = null;
			// if (Sensors.DEBUG)
			// Log.d(LOG_TAG, "Capture file created");
			// File captureFileName = new File(Environment
			// .getExternalStorageDirectory().getPath(), "capture.csv");
			// try {
			// captureFile = new PrintWriter(new FileWriter(captureFileName,
			// false));
			// } catch (IOException ex) {
			// Log.e(LOG_TAG, ex.getMessage(), ex);
			// }
			samplingStarted = true;
		}
	}

	static public String getCurrentSensor() {
		return sensorName;
	}

	static private String sensorName;
	private int rate = SensorManager.SENSOR_DELAY_UI;
	private SensorManager sensorManager;
	private PrintWriter captureFile;
	private boolean samplingStarted = false;
	private ScreenOffBroadcastReceiver screenOffBroadcastReceiver = null;
	private Sensor ourSensor;
	private GenerateUserActivityThread generateUserActivityThread = null;
	private long logCounter = 0;
	private PowerManager.WakeLock sampingInProgressWakeLock;
	private Date samplingStartedTimeStamp;
	private Location currentLocation;

	class ScreenOffBroadcastReceiver extends BroadcastReceiver {
		private static final String LOG_TAG = "ScreenOffBroadcastReceiver";

		public void onReceive(Context context, Intent intent) {
			if (Sensors.DEBUG)
				Log.d(LOG_TAG, "onReceive: " + intent);
			if (sensorManager != null && samplingStarted) {
				if (generateUserActivityThread != null) {
					generateUserActivityThread.stopThread();
					generateUserActivityThread = null;
				}
				generateUserActivityThread = new GenerateUserActivityThread();
				generateUserActivityThread.start();
			}
		}
	}

	class GenerateUserActivityThread extends Thread {
		public void run() {
			if (Sensors.DEBUG)
				Log.d(LOG_TAG,
						"Waiting 2 sec for switching back the screen ...");
			try {
				Thread.sleep(2000L);
			} catch (InterruptedException ex) {
			}
			if (Sensors.DEBUG)
				Log.d(LOG_TAG, "User activity generation thread started");

			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			userActivityWakeLock = pm.newWakeLock(
					PowerManager.SCREEN_DIM_WAKE_LOCK
							| PowerManager.ACQUIRE_CAUSES_WAKEUP,
					"GenerateUserActivity");
			userActivityWakeLock.acquire();
			if (Sensors.DEBUG)
				Log.d(LOG_TAG, "User activity generation thread exiting");
		}

		public void stopThread() {
			if (Sensors.DEBUG)
				Log.d(LOG_TAG, "User activity wake lock released");
			userActivityWakeLock.release();
			userActivityWakeLock = null;
		}

		PowerManager.WakeLock userActivityWakeLock;
	}

	private class MyLocationListener implements LocationListener {
		@Override
		public void onLocationChanged(Location location) {
			// Called when a new location is found by the location provider.
			Log.v("GPSTEST",
					"Got New Location of provider:" + location.getProvider());
			if (currentLocation != null) {
				if (isBetterLocation(location, currentLocation)) {
					Log.v("GPSTEST", "It's a better location");
					currentLocation = location;
					showLocation(location);
				} else {
					Log.v("GPSTEST", "Not very good!");
				}
			} else {
				Log.v("GPSTEST", "It's first location");
				currentLocation = location;
				showLocation(location);
			}
			// 移除基于LocationManager.NETWORK_PROVIDER的监听器
			if (LocationManager.NETWORK_PROVIDER.equals(location.getProvider())) {
				locationManager.removeUpdates(this);
			}
		}

		// 后3个方法此处不做处理
		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {

		}
	};

	private void showLocation(Location location) {
		// 纬度
		Log.v("GPSTEST", "Latitude:" + location.getLatitude());
		// 经度
		Log.v("GPSTEST", "Longitude:" + location.getLongitude());
		// 精确度
		Log.v("GPSTEST", "Accuracy:" + location.getAccuracy());
		// Location还有其它属性，请自行探索
		float data[] = { (float) location.getLatitude(),
				(float) location.getLongitude(), location.getSpeed() };

		YeelinkReport.reportSensorData("GPS", 0xffff, data);
	}

	private static final int CHECK_INTERVAL = 1000 * 30;

	protected boolean isBetterLocation(Location location,
			Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > CHECK_INTERVAL;
		boolean isSignificantlyOlder = timeDelta < -CHECK_INTERVAL;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location,
		// use the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must
			// be worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation
				.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and
		// accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate
				&& isFromSameProvider) {
			return true;
		}
		return false;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

	private void registerLocationListener() {
		networkListener = new MyLocationListener();
		locationManager.requestLocationUpdates(
				LocationManager.NETWORK_PROVIDER, 3000, 0, networkListener);
		gpsListener = new MyLocationListener();
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				5000, 0, gpsListener);
	}
}
