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
	
	@Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        // Display a notification about us starting.  We put an icon in the status bar.
        showNotification();
    }
	
    /**
     * Show a notification while this service is running.
     */
    @SuppressWarnings("deprecation")
	private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getString(R.string.textview_sampling);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.ic_launcher, text,
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, Sensors.class), 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, text,
                       text, contentIntent);

        // Send the notification.
        mNM.notify(text.toString(), NOTIFICATION, notification);
    }

	public int onStartCommand(Intent intent, int flags, int startId) {
		if(intent == null){
			stopSelf();
			return START_REDELIVER_INTENT;
		}
		super.onStartCommand( intent, flags, startId );
		if( Sensors.DEBUG )
			Log.d( LOG_TAG, "onStartCommand" );
		stopSampling();		// just in case the activity-level service management fails
      	sensorName = intent.getStringExtra( "sensorname" );
      	if( Sensors.DEBUG )
      		Log.d( LOG_TAG,"sensorName: "+sensorName );
        SharedPreferences appPrefs = getSharedPreferences( 
                                        Sensors.PREF_FILE,
                                        MODE_PRIVATE );
        rate = appPrefs.getInt(
                    Sensors.PREF_SAMPLING_SPEED, 
                    SensorManager.SENSOR_DELAY_NORMAL );
        if( Sensors.DEBUG )
        	Log.d( LOG_TAG, "rate: "+rate );

		screenOffBroadcastReceiver = new ScreenOffBroadcastReceiver();
		IntentFilter screenOffFilter = new IntentFilter();
		screenOffFilter.addAction( Intent.ACTION_SCREEN_OFF );
		if( KEEPAWAKE_HACK )
			registerReceiver( screenOffBroadcastReceiver, screenOffFilter );
		sensorManager = (SensorManager)getSystemService( SENSOR_SERVICE  );
		startSampling();
		if( Sensors.DEBUG )
			Log.d( LOG_TAG, "onStartCommand ends" );
		//return START_NOT_STICKY;
		return START_STICKY;
	}

	public void onDestroy() {
		Log.i(LOG_TAG, "SamplingService onDestroy()");
		super.onDestroy();
		if( Sensors.DEBUG )
			Log.d( LOG_TAG, "onDestroy" );
		stopSampling();
		if( KEEPAWAKE_HACK )
			unregisterReceiver( screenOffBroadcastReceiver );
		
        SharedPreferences appPrefs = getSharedPreferences( 
                Sensors.PREF_FILE,
                MODE_PRIVATE );
        Editor ed = appPrefs.edit();
        ed.putString("currentSensor", "");
        ed.commit();
        // Cancel the persistent notification.
        mNM.cancelAll();
	}

	public IBinder onBind(Intent intent) {
		return null;	// cannot bind
	}

// SensorEventListener
    public void onAccuracyChanged (Sensor sensor, int accuracy) {
    }

    @SuppressWarnings("deprecation")
	public void onSensorChanged(SensorEvent sensorEvent) {
		++logCounter;
    	if( !MINIMAL_ENERGY ) {
    		if( Sensors.DEBUG ){
    			StringBuilder b = new StringBuilder();
    			for( int i = 0 ; i < sensorEvent.values.length ; ++i ) {
    				if( i > 0 )
    					b.append( " , " );
    				b.append( Float.toString( sensorEvent.values[i] ) );
    			}
    			Log.d( LOG_TAG, sensorName + ": "+new Date().toLocaleString() + " ["+b+"]" );
    			YeelinkReport.reportSensorData(sensorName, ourSensor.getType(), sensorEvent.values);
    		}
    		if( captureFile != null ) {
    			captureFile.print( Long.toString( sensorEvent.timestamp) );
    			for( int i = 0 ; i < sensorEvent.values.length ; ++i ) {
    				captureFile.print( "," );
    				captureFile.print( Float.toString( sensorEvent.values[i] ) );
    			}
    			captureFile.println();
    		} 
    	} else {
    		++logCounter;
    		if( ( logCounter % MINIMAL_ENERGY_LOG_PERIOD ) == 0L )
    			Log.d( LOG_TAG, "logCounter: "+logCounter+" at "+new Date().toString());
    	}
    }

    
	private void stopSampling() {
		if( !samplingStarted )
			return;
		if( generateUserActivityThread != null ) {
			generateUserActivityThread.stopThread();
			generateUserActivityThread = null;
		}
        if( sensorManager != null ) {
//        	if( Config.DEBUG )
//        		Log.d( LOG_TAG, "unregisterListener/SamplingService" );
            sensorManager.unregisterListener( this );
		}
        if( captureFile != null ) {
            captureFile.close();
			captureFile = null;
        }
		samplingStarted = false;
		sampingInProgressWakeLock.release();
		sampingInProgressWakeLock = null;
		Date samplingStoppedTimeStamp = new Date();
		long secondsEllapsed = 
			( samplingStoppedTimeStamp.getTime() -
			  samplingStartedTimeStamp.getTime() ) / 1000L;
		Log.d(LOG_TAG, "Sampling started: "+
				samplingStartedTimeStamp.toString()+
				"; Sampling stopped: "+
				samplingStoppedTimeStamp.toString()+
				" ("+secondsEllapsed+" seconds) "+
				"; samples collected: "+logCounter );
	}

	private void startSampling() {
		if( samplingStarted )
			return;
        if( sensorName != null ) {
            List<Sensor> sensors = sensorManager.getSensorList( Sensor.TYPE_ALL );
            ourSensor = null;;
            for( int i = 0 ; i < sensors.size() ; ++i )
                if( sensorName.equals( sensors.get( i ).getName() ) ) {
                    ourSensor = sensors.get( i );
                    break;
                }
            if( ourSensor != null ) {
            	if( Sensors.DEBUG )
            		Log.d( LOG_TAG, "registerListener/SamplingService" );
           		sensorManager.registerListener( 
                            this, 
                            ourSensor,
                            rate );
			}
            samplingStartedTimeStamp = new Date();
            PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
			sampingInProgressWakeLock = 
				pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SamplingInProgress");
			sampingInProgressWakeLock.acquire();
			captureFile = null;
			if( Sensors.DEBUG )
				Log.d( LOG_TAG, "Capture file created" );
            File captureFileName = new File(Environment.getExternalStorageDirectory().getPath(), "capture.csv" );
            try {
                captureFile = new PrintWriter( new FileWriter( captureFileName, false ) );
            } catch( IOException ex ) {
                Log.e( LOG_TAG, ex.getMessage(), ex );
            }
			samplingStarted = true;
        }
	}

	static public String getCurrentSensor(){
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

	class ScreenOffBroadcastReceiver extends BroadcastReceiver {
		private static final String LOG_TAG = "ScreenOffBroadcastReceiver";

		public void onReceive(Context context, Intent intent) {
			if( Sensors.DEBUG )
				Log.d( LOG_TAG, "onReceive: "+intent );
			if( sensorManager != null && samplingStarted ) {
				if( generateUserActivityThread != null ) {
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
			if( Sensors.DEBUG )
				Log.d( LOG_TAG, "Waiting 2 sec for switching back the screen ..." );
			try {
				Thread.sleep( 2000L );
			} catch( InterruptedException ex ) {}
			if( Sensors.DEBUG )
				Log.d( LOG_TAG, "User activity generation thread started" );

			PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
			userActivityWakeLock = 
				pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, 
						"GenerateUserActivity");
			userActivityWakeLock.acquire();
			if( Sensors.DEBUG )
				Log.d( LOG_TAG, "User activity generation thread exiting" );
		}

		public void stopThread() {
			if( Sensors.DEBUG )
				Log.d( LOG_TAG, "User activity wake lock released" );
			userActivityWakeLock.release();
			userActivityWakeLock = null;
		}

		PowerManager.WakeLock userActivityWakeLock;
	}
}

