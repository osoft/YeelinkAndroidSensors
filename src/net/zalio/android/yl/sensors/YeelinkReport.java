package net.zalio.android.yl.sensors;

import java.util.ArrayList;

import android.util.Log;
import android.util.SparseArray;

public class YeelinkReport {
	static final int YEELINK_INTERVAL = 10; // (seconds) interval force by YeeLink
	public static final String TAG = "YeelinkReport";
	static String latestSensor;
	static int latestSensorType;
	static float latestData[];
	static int latestSensorID = 0;
	static boolean sensorChanged = true;
	static boolean datatypeHashMapInited = false;
	static SparseArray<SensorProperties> typeMap;
	static ReportDataThread dataThread = null;
	static private boolean runFlag;
	
	public static void setRunFlag(boolean runFlag) {
		YeelinkReport.runFlag = runFlag;
	}
	
	static class SensorProperties{
		SensorProperties(String name, String tags[], String unit, String unitSymb, float minV, float maxV, int choice){
			typeName = name;
			unitName = unit;
			unitSymbol = unitSymb;
			minVal = minV;
			maxVal = maxV;
			this.tags = tags;
		}
		public String getTypeName() {
			return typeName;
		}
		public void setTypeName(String typeName) {
			this.typeName = typeName;
		}
		public String[] getTags() {
			return tags;
		}
		public void setTags(String[] tags) {
			this.tags = tags;
		}
		public String getUnitName() {
			return unitName;
		}
		public void setUnitName(String unitName) {
			this.unitName = unitName;
		}
		public String getUnitSymbol() {
			return unitSymbol;
		}
		public void setUnitSymbol(String unitSymbol) {
			this.unitSymbol = unitSymbol;
		}
		public float getMinVal() {
			return minVal;
		}
		public void setMinVal(float minVal) {
			this.minVal = minVal;
		}
		public float getMaxVal() {
			return maxVal;
		}
		public void setMaxVal(float maxVal) {
			this.maxVal = maxVal;
		}
		String typeName;
		String tags[];
		String unitName;
		String unitSymbol;
		float minVal;
		float maxVal;
		int dataPick;
	}
	
	static class ReportDataThread extends Thread{

		public void run(){
			while(runFlag){
				runFlag = false;
				if(sensorChanged || latestSensorID ==0){
					SensorProperties sp = typeMap.get(latestSensorType);
					if(sp == null){
						Log.w(TAG, "This type of sensor is not compatible to Yeelink.");
					}
					else{
						boolean sensorExisted = false;
						ArrayList<YeelinkSensor> sensorList = HttpUtils.fetchSensorList(GlobalVars.DeviceID);
						for(YeelinkSensor s:sensorList){
							if(s.mTitle.equals(latestSensor)){
								Log.i(TAG, "sensor existed on Yeelink, reusing");
								sensorExisted = true;
								latestSensorID = s.mId;
								break;
							}
						}
						if(sensorExisted == false){
							latestSensorID = HttpUtils.createSensor(
									GlobalVars.DeviceID, latestSensor, 
									sp.getTypeName(), sp.getTags(), sp.getUnitName(), 
									sp.getUnitSymbol(), sp.getMinVal(), sp.getMaxVal());
						}
						
						if(latestSensorID != 0){
							Log.i(TAG, "Sending data to Yeelink");
							HttpUtils.setSensorData(
									GlobalVars.DeviceID, latestSensorID, latestData[sp.dataPick]);
						}
						else{
							Log.e(TAG, "Unable to create such sensor on yeelink");
						}
					}

				}
				
				try {
					Thread.sleep(YEELINK_INTERVAL * 1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	static boolean reportSensorData(String sensorName, int sensorType, float data[]){
		if(!datatypeHashMapInited){
			initDataTypeHashMap();
		}
		if(!sensorName.equals(latestSensor)){
			sensorChanged = true;
		}
		latestSensor = sensorName;
		latestData = data;
		latestSensorType = sensorType;
		runFlag = true;
		if(null == dataThread){
			dataThread = new ReportDataThread();
			dataThread.start();
		}
		else if(!dataThread.isAlive()){
			dataThread = new ReportDataThread();
			dataThread.start();
		}
		return true;
	}
	
	static void initDataTypeHashMap(){
		if(typeMap == null){
			typeMap = new SparseArray<SensorProperties>();
			String lightTags[] = {"android", "light"};
			typeMap.put(android.hardware.Sensor.TYPE_LIGHT,
					new SensorProperties("LIGHT", lightTags, "lux", "LUX" , 0.0f, 120000.0f, 0));
			
			String proximityTags[] = {"android", "proximitys"};
			typeMap.put(android.hardware.Sensor.TYPE_PROXIMITY, 
					new SensorProperties("PROXIMITY", proximityTags, "cm", "cm", 0, 10, 0));
			
			String pressureTags[] = {"android", "pressure"};
			typeMap.put(android.hardware.Sensor.TYPE_PRESSURE, 
					new SensorProperties("PRESSURE", pressureTags, "millibar", "hPa", 500, 1000, 0));
			
			String tempTags[] = {"android", "temperature"};
			typeMap.put(android.hardware.Sensor.TYPE_AMBIENT_TEMPERATURE,
					new SensorProperties("TEMPERATURE", tempTags, "Celsius", "C", -20, 50, 0));
			
			String humidityTags[] = {"android", "humidity"};
			typeMap.put(android.hardware.Sensor.TYPE_RELATIVE_HUMIDITY,
					new SensorProperties("RELATIVE HUMIDITY", humidityTags, "Percent", "*100%", 0.00f, 1.00f, 0));
		}
		
		datatypeHashMapInited = true;
	}
}
