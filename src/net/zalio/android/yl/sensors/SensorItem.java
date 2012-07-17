package net.zalio.android.yl.sensors;

import android.hardware.Sensor;

class SensorItem {
    SensorItem( Sensor sensor ) {
        this.sensor = sensor;
        this.sampling = false;
        this.type = sensor.getType();
    }
    
    SensorItem( String name, int type){
    	this.sensorName = name;
    	this.type = type;
    	this.sampling = false;
    }

    public String getSensorName() {
    	if(sensor == null) {
    		return sensorName;
    	}
    	else {
    		return sensor.getName();    		
    	}
    	
    }

    Sensor getSensor() {
        return sensor;
    }

    void setSampling( boolean sampling ) {
    	this.sampling = sampling;
    }
    
    boolean getSampling() {
    	return sampling;
    }
    
    public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}



	private String sensorName;
	private Sensor sensor;
    private boolean sampling;
    private int type;
}
