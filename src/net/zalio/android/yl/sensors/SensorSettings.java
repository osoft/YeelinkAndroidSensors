package net.zalio.android.yl.sensors;

import android.app.Activity;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.hardware.SensorManager;

public class SensorSettings extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        SharedPreferences appPrefs = getSharedPreferences( 
                                        Sensors.PREF_FILE,
                                        MODE_PRIVATE );
        boolean captureState = 
            appPrefs.getBoolean( Sensors.PREF_CAPTURE_STATE, false );
        if( captureState ) {
            CheckBox cb = (CheckBox)findViewById( R.id.settings_capture_cb );
            cb.setChecked( true );
        }
        int speedState = appPrefs.getInt( Sensors.PREF_SAMPLING_SPEED, 
                            SensorManager.SENSOR_DELAY_NORMAL );
        Spinner spinner = (Spinner)findViewById( 
                            R.id.settings_samplingspeed_spinner );
        spinnerData = new SpinnerData[4];
        spinnerData[0] = new SpinnerData( "Normal", 
                            SensorManager.SENSOR_DELAY_NORMAL );
        spinnerData[1] = new SpinnerData( "UI", 
                            SensorManager.SENSOR_DELAY_UI );
        spinnerData[2] = new SpinnerData( "Game", 
                            SensorManager.SENSOR_DELAY_GAME );
        spinnerData[3] = new SpinnerData( "Fastest", 
                            SensorManager.SENSOR_DELAY_FASTEST );
        ArrayAdapter<SpinnerData> adapter = 
            new ArrayAdapter<SpinnerData>( 
                this,
                android.R.layout.simple_spinner_item,
                spinnerData );
        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter( adapter );
        for( int i = 0 ; i < spinnerData.length ; ++i )
            if( speedState == spinnerData[i].getValue() ) {
                spinner.setSelection( i );
                break;
            }
    }

    protected void onPause() {
        super.onPause();
        SharedPreferences appPrefs = getSharedPreferences( 
                                        Sensors.PREF_FILE,
                                        MODE_PRIVATE );
        SharedPreferences.Editor ed = appPrefs.edit();
        CheckBox cb = (CheckBox)findViewById( R.id.settings_capture_cb );
        boolean captureState = cb.isChecked();
        Spinner spinner = (Spinner)findViewById( 
                            R.id.settings_samplingspeed_spinner );
        SpinnerData sd = (SpinnerData)spinner.getSelectedItem();
        ed.putBoolean( Sensors.PREF_CAPTURE_STATE, captureState );
        ed.putInt( Sensors.PREF_SAMPLING_SPEED, sd.getValue() );
        ed.commit();
    }

    class SpinnerData {
        SpinnerData( String name, int value ) {
            this.name = name;
            this.value = value;
        }

        public String toString() {
            return name;
        }

        public int getValue() {
            return value;
        }

        String name;
        int value;
    }

    private SpinnerData spinnerData[];
}
