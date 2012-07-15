package net.zalio.android.yl.sensors;

import android.hardware.Sensor;

class SensorItem {
    SensorItem( Sensor sensor ) {
        this.sensor = sensor;
        this.sampling = false;
        this.type = sensor.getType();
    }

    public String getSensorName() {
        return sensor.getName();
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



	private Sensor sensor;
    private boolean sampling;
    private int type;
}
