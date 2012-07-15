package net.zalio.android.yl.sensors;

import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.widget.TextView;
import java.util.List;
import java.io.*;

public class SensorMonitor extends Activity implements SensorEventListener {
    static final String LOG_TAG = "SENSORMONITOR";
	static final String SAMPLINGSTARTED_KEY = "samplingStarted";
	static final String SENSORNAME_KEY = "sensorName";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
		samplingStarted = false;
		sensorName = null;
        setContentView( R.layout.monitor );
        Intent i = getIntent();
        if( i != null ) {
            sensorName = i.getStringExtra( "sensorname" );
            if( Sensors.DEBUG )
            	Log.d( LOG_TAG,"sensorName: "+sensorName );
            if( sensorName != null ) {
                TextView t = (TextView)findViewById( R.id.sensorname );
                t.setText( sensorName );
            }
        }
		if( savedInstanceState != null ) {
			samplingStarted = savedInstanceState.getBoolean( SAMPLINGSTARTED_KEY, false );
			sensorName = savedInstanceState.getString( SENSORNAME_KEY );
		}
        SharedPreferences appPrefs = getSharedPreferences( 
                                        Sensors.PREF_FILE,
                                        MODE_PRIVATE );
        captureState = appPrefs.getBoolean( Sensors.PREF_CAPTURE_STATE, false );
        captureStateText = null;
        if( captureState ) {
            File captureFileName = new File(Environment.getExternalStorageDirectory().getPath(), "capture.csv" );
            captureStateText = "Capture: "+captureFileName.getAbsolutePath();
            try {
// if we are restarting (e.g. due to orientation change), we append to the log file instead of overwriting it
                captureFile = new PrintWriter( new FileWriter( captureFileName, samplingStarted ) );
            } catch( IOException ex ) {
                Log.e( LOG_TAG, ex.getMessage(), ex );
                captureStateText = "Capture: "+ex.getMessage();
            }
        } else
            captureStateText = "Capture: OFF";
        rate = appPrefs.getInt(
                    Sensors.PREF_SAMPLING_SPEED, 
                    SensorManager.SENSOR_DELAY_NORMAL );
        captureStateText += "; rate: "+getRateName( rate );
        TextView t = (TextView)findViewById( R.id.capturestate );
        t.setText( captureStateText );
    }

    protected void onStart() {
        super.onStart();
        if( Sensors.DEBUG )
        	Log.d( LOG_TAG, "onStart" );
		startSampling();
    }

	protected void onResume() {
		super.onResume();
        if( Sensors.DEBUG )
        	Log.d( LOG_TAG, "onResume" );
		startSampling(); 
	}

    protected void onPause() {
        super.onPause();
        if( Sensors.DEBUG )
        	Log.d( LOG_TAG, "onPause" );
		stopSampling();
    }

	protected void onStop() {
		super.onStop();
        if( Sensors.DEBUG )
        	Log.d( LOG_TAG, "onStop" );
	}

	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState( outState );
        if( Sensors.DEBUG )
        	Log.d( LOG_TAG, "onSaveInstanceState" );
		outState.putBoolean( SAMPLINGSTARTED_KEY, samplingStarted );
		if( sensorName != null )
			outState.putString( SENSORNAME_KEY, sensorName );
	}

// SensorEventListener
    public void onAccuracyChanged (Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent sensorEvent) {
        StringBuilder b = new StringBuilder();
        for( int i = 0 ; i < sensorEvent.values.length ; ++i ) {
            if( i > 0 )
                b.append( " , " );
            b.append( Float.toString( sensorEvent.values[i] ) );
        }
        if( Sensors.DEBUG )
        	Log.d( LOG_TAG, "onSensorChanged: ["+b+"]" );
        if( captureFile != null ) {
    			captureFile.print( Long.toString( sensorEvent.timestamp) );
                for( int i = 0 ; i < sensorEvent.values.length ; ++i ) {
                    captureFile.print( "," );
                    captureFile.print( Float.toString( sensorEvent.values[i] ) );
                }
                captureFile.println();
        }
        long currentMillisec = System.currentTimeMillis();
        if( baseMillisec < 0 ) {
            baseMillisec = currentMillisec;
            samplesPerSec = 0;
        } else
        if( ( currentMillisec - baseMillisec ) < 1000L )
            ++samplesPerSec;
        else {
            TextView t = (TextView)findViewById( R.id.capturestate );
            t.setText( captureStateText+" ("+samplesPerSec+" Hz)" );
            int count = sensorEvent.values.length < fields.length ?
                    sensorEvent.values.length :
                    fields.length;
            for( int i = 0 ; i < count ; ++i ) {
                t = (TextView)findViewById( fields[i] );
                t.setText( "["+i+"]: "+Float.toString( sensorEvent.values[i] ) );
            }
            samplesPerSec = 1;
            baseMillisec = currentMillisec;
        }
    }

    private String getRateName( int sensorRate ) {
        String result = "N/A";
        switch( sensorRate ) {
            case SensorManager.SENSOR_DELAY_UI:
                result = "UI";
                break;

            case SensorManager.SENSOR_DELAY_NORMAL:
                result = "Normal";
                break;

            case SensorManager.SENSOR_DELAY_GAME:
                result = "Game";
                break;

            case SensorManager.SENSOR_DELAY_FASTEST:
                result = "Fastest";
                break;
        }
        return result;
    }

	private void stopSampling() {
		if( !samplingStarted )
			return;
        if( sensorManager != null )
            sensorManager.unregisterListener( this );
        if( captureFile != null ) {
            captureFile.close();
			captureFile = null;
        }
		if( samplingWakeLock != null ) {
			samplingWakeLock.release();
			samplingWakeLock = null;
			Log.d( LOG_TAG, "PARTIAL_WAKE_LOCK released" );
		}
		samplingStarted = false;
	}

	private void startSampling() {
		if( samplingStarted )
			return;
        if( sensorName != null ) {
            sensorManager = 
                (SensorManager)getSystemService( SENSOR_SERVICE  );
            List<Sensor> sensors = sensorManager.getSensorList( Sensor.TYPE_ALL );
            Sensor ourSensor = null;
            for( int i = 0 ; i < sensors.size() ; ++i )
                if( sensorName.equals( sensors.get( i ).getName() ) ) {
                    ourSensor = sensors.get( i );
                    break;
                }
            if( ourSensor != null ) {
                    baseMillisec = -1L;
                    sensorManager.registerListener( 
                            this, 
                            ourSensor,
                            rate );
			}
// Obtain partial wakelock so that sampling does not stop even if the device goes to sleep
			PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
			samplingWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SensorMonitor");
			samplingWakeLock.acquire();
			Log.d( LOG_TAG, "PARTIAL_WAKE_LOCK acquired" );
			samplingStarted = true;
        }
	}

	private PowerManager.WakeLock samplingWakeLock;
    private String sensorName;
    private boolean captureState = false;
    private int rate = SensorManager.SENSOR_DELAY_UI;
    private SensorManager sensorManager;
    private PrintWriter captureFile;
    private long baseMillisec = -1L;
    private long samplesPerSec = 0;
    private String captureStateText;
    final static int fields[] = {
        R.id.f1,
        R.id.f2,
        R.id.f3,
        R.id.f4,
        R.id.f5,
        R.id.f6
    };
	boolean samplingStarted = false;
}
