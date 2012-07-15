package net.zalio.android.yl.sensors;


public class YeelinkSensor {
	
	public int mId;
	public int mSensorType;
	public String mTitle;
	public String mAbout;
	public boolean mOn;
	
	
	public YeelinkSensor() {
	}
	
	public YeelinkSensor(int id, int sensorType, String title, String about) {
		mId = id;
		mSensorType = sensorType;
		mTitle = title;
		mAbout = about;
		mOn = false;
	}
	
	public String toString() {
		return String.format("{id:%d, sensorType:%d, title:%s, about:%s}", mId, mSensorType, mTitle, mAbout);
	}
}
