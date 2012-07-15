package net.zalio.android.yl.sensors;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

public class SensorListAdapter extends BaseAdapter {

    public SensorListAdapter(Context context,
						List<SensorItem> sensors ) { 
		inflater = LayoutInflater.from( context );
        this.context = context;
        this.sensors = sensors;
    }

    public int getCount() {                        
        return sensors.size();
    }

    public Object getItem(int position) {     
        return sensors.get(position);
    }

    public long getItemId(int position) {  
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) { 
        SensorItem item = sensors.get(position);
        View v = null;
        if( convertView != null )
        	v = convertView;
        else
        	v = inflater.inflate( R.layout.sensor_row, parent, false);
        String sensorName = item.getSensorName();
        TextView sensorNameTV = (TextView)v.findViewById( R.id.sensorname);
        sensorNameTV.setText( sensorName );
        boolean sampling = item.getSampling();
    	TextView samplingStatusTV = (TextView)v.findViewById( R.id.samplingstatus );
    	CheckBox cbEnable = (CheckBox)v.findViewById(R.id.cbEnable);
    	cbEnable.setChecked(sampling);
    	if( sampling ){
    		samplingStatusTV.setVisibility( View.VISIBLE);
    	}
    	else{
    		samplingStatusTV.setVisibility( View.INVISIBLE);
    	}

    	return v;
    }

    @SuppressWarnings("unused")
	private Context context;
    private List<SensorItem> sensors;
	private LayoutInflater inflater;

}
