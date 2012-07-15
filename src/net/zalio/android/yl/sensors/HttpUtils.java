package net.zalio.android.yl.sensors;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;

import android.util.Log;

public final class HttpUtils {
	public static final String TAG = "HttpUtils";
	
	public static final int HTTP_OK = 200;
	private static final String BASE_URL = "http://yeelink.net/mobile/";
	
	private HttpUtils() {
        // To forbidden instantiate this class.
    }
	
	/**
     * Login with username and password
     * 
     * @param username
     * @param password
     * @return ture:success, false:fail
     */
    public static boolean login(String username, String password) {
    	
    	// http://www.yeelink.net/mobile/login?login=demo&password=yeelinkdemo
    	
    	HttpClient client = new DefaultHttpClient();
    	final String urlStr = String.format("%s/login?login=%s&password=%s", BASE_URL, username, password);

        try {
        	
        	HttpGet httpRequest = new HttpGet(urlStr);

        	//String response = null;
    		HttpResponse httpResponse = client.execute(httpRequest);
    		
			//
    		int statusCode = httpResponse.getStatusLine().getStatusCode();
    		Log.w(TAG, "______login() status code = " + statusCode);
			if (statusCode != 200) {
				return false;
			}
			
			// response = {"status":"success","desc":"Login and password are ok","data":{"apikey":"9cdf51696fa9ddfacdf819033a5f2f63"}}
			String strResult = EntityUtils.toString(httpResponse.getEntity());
			Log.w(TAG, "______login() response = " + strResult);
			
			// response parse with json
			JSONObject jo = new JSONObject(strResult);
			if (jo.getString("status").equals("success") == false) {
				return false;
			}
			
			final String apiKey = ((JSONObject)jo.get("data")).getString("apikey");
			Log.w(TAG, "______apiKey = " + apiKey);
			GlobalVars.ApiKey = apiKey;
			GlobalVars.Username = username;
			//Monitor.putApiKey(apiKey);
    		//Monitor.setUsername(username);
    	} catch (IOException e) {
    		e.printStackTrace();
    		return false;
    	} catch (JSONException e) {
    		Log.w(TAG, "______json exception: " + e);
    		return false;
    	}
    	
    	return true;
    }
    
    /**
     * Fetch device list from server
     * 
     * @param username
     * @param password
     * @return ture:success, false:fail
     */
    public static ArrayList<Device> fetchDeviceList() {
    	
    	ArrayList<Device> deviceList = new ArrayList<Device>();
    	
    	// http://www.yeelink.net/mobile/list_devices?login=demo&apikey=9cdf51696fa9ddfacdf819033a5f2f63
    	
    	HttpClient client = new DefaultHttpClient();
    	final String urlStr = String.format("%s/list_devices?login=%s&apikey=%s", BASE_URL, GlobalVars.Username, GlobalVars.ApiKey);

        try {
        	
        	HttpGet httpRequest = new HttpGet(urlStr);
    		HttpResponse httpResponse = client.execute(httpRequest);
    		
			//
    		int statusCode = httpResponse.getStatusLine().getStatusCode();
    		Log.w(TAG, "______fetchDiviceList() status code = " + statusCode);
			if (statusCode != 200) {
				return null;
			}
			
			// response = {"status":"success","desc":"Login and password are ok","data":{"apikey":"9cdf51696fa9ddfacdf819033a5f2f63"}}
			String strResult = EntityUtils.toString(httpResponse.getEntity());
			Log.w(TAG, "______fetchDiviceList() response = " + strResult);
			
			// response parse with json
			JSONObject jo = new JSONObject(strResult);
			if (!jo.getString("status").equals("success")) {
				return null;
			}
			
			JSONArray jArray = jo.getJSONObject("data").getJSONArray("devices");
			JSONObject temp;
        	for(int i = 0; i < jArray.length(); i++) {
        		temp = (JSONObject)jArray.get(i);
        		Device device = new Device(
        				temp.getInt("id"), 
        				temp.getString("title"),
        				temp.getString("about"));
        		deviceList.add(device);
        		Log.w(TAG, "______deviceList[" + i + "] : " + device);
        	}
    		
    	} catch (IOException e) {
    		e.printStackTrace();
    		return null;
    	} catch (JSONException e) {
    		Log.w(TAG, "______json exception: " + e);
    		return null;
    	}
    	
    	return deviceList;
    }
    
    /**
     * Fetch sensor list from server
     * 
     * @param username
     * @param password
     * @return sensor list
     */
    public static ArrayList<YeelinkSensor> fetchSensorList(final int deviceId) {
    	
    	ArrayList<YeelinkSensor> sensorList = new ArrayList<YeelinkSensor>();
    	
    	// http://www.yeelink.net/mobile/list_sensors?login=demo&apikey=9cdf51696fa9ddfacdf819033a5f2f63&device=5
    	
    	HttpClient client = new DefaultHttpClient();
    	final String urlStr = String.format("%s/list_sensors?login=%s&apikey=%s&device=%d", BASE_URL, GlobalVars.Username, GlobalVars.ApiKey, deviceId);

        try {
        	
        	HttpGet httpRequest = new HttpGet(urlStr);
    		HttpResponse httpResponse = client.execute(httpRequest);
    		
			//
    		int statusCode = httpResponse.getStatusLine().getStatusCode();
    		Log.w(TAG, "______fetchSensorList() status code = " + statusCode);
			if (statusCode != 200) {
				return null;
			}
			
			// response = {"status":"success","desc":"Login and password are ok","data":{"apikey":"9cdf51696fa9ddfacdf819033a5f2f63"}}
			String strResult = EntityUtils.toString(httpResponse.getEntity());
			Log.w(TAG, "______fetchSensorList() response = " + strResult);
			
			// response parse with json
			JSONObject jo = new JSONObject(strResult);
			if (!jo.getString("status").equals("success")) {
				return null;
			}
			
			JSONArray jArray = jo.getJSONObject("data").getJSONArray("sensors");
			JSONObject temp;
        	for(int i = 0; i < jArray.length(); i++) {
        		temp = (JSONObject)jArray.get(i);
        		YeelinkSensor sensor = new YeelinkSensor(
        				temp.getInt("id"), 
        				temp.getInt("sensor_type"), 
        				temp.getString("title"),
        				temp.getString("about"));
        		sensorList.add(sensor);
        		Log.w(TAG, "______sensorList[" + i + "] : " + sensor);
        	}
    		
    	} catch (IOException e) {
    		e.printStackTrace();
    		return null;
    	} catch (JSONException e) {
    		Log.w(TAG, "______json exception: " + e);
    		return null;
    	}
    	
    	return sensorList;
    }
    
    /**
     * Fetch sensor (type = 0) from server
     * 
     * @param username
     * @param password
     * @return ture:success, false:fail
     */
    public static ChartData fetchSensorData(final int sensorId, final String during) {
    	
    	ChartData chartData = new ChartData();
    	
    	ArrayList<Point> pointList = new ArrayList<Point>();
    	
    	// http://www.yeelink.net/mobile/sensor_data?login=demo&apikey=9cdf51696fa9ddfacdf819033a5f2f63&sensor=7&time=1hr
    	
    	HttpClient client = new DefaultHttpClient();
    	final String urlStr = String.format("%s/sensor_data?login=%s&apikey=%s&sensor=%d&time=%s", BASE_URL, GlobalVars.Username, GlobalVars.ApiKey, sensorId, during);

        try {
        	
        	HttpGet httpRequest = new HttpGet(urlStr);
    		HttpResponse httpResponse = client.execute(httpRequest);
    		
			//
    		int statusCode = httpResponse.getStatusLine().getStatusCode();
    		Log.w(TAG, "______fetchSensorData() status code = " + statusCode);
			if (statusCode != 200) {
				return null;
			}
			
			String strResult = EntityUtils.toString(httpResponse.getEntity());
			Log.w(TAG, "______fetchSensorData() response = " + strResult);
			
			// response parse with json
			JSONObject jo = new JSONObject(strResult);
			
			JSONObject element = (JSONObject)jo.getJSONArray("elements").get(0);
			String elementType = element.getString("type");
			Log.w(TAG, "______elementType: " + elementType);
			JSONArray valuesArray = element.getJSONArray("values");
        	for(int i = 0; i < valuesArray.length(); i++) {
        		JSONObject temp = (JSONObject)valuesArray.get(i);
        		int x = temp.getInt("x");
        		int y = temp.getInt("y");
        		Log.w(TAG, "______x=" + x + ", y=" + y);
        		
        		pointList.add(new Point(x, y));
        	}
        	String elementText = element.getString("text");
        	Log.w(TAG, "______elementText: " + elementText);
        	JSONObject elementDotStyle = element.getJSONObject("dot-style");
        	String elementDotStyleType = elementDotStyle.getString("type");
        	Log.w(TAG, "______elementDotStyleType: " + elementDotStyleType);
        	int elementDotStyleDotSize = elementDotStyle.getInt("dot-size");
        	Log.w(TAG, "______elementDotStyleDotSize: " + elementDotStyleDotSize);
        	String elementDotStyleTip = elementDotStyle.getString("tip");
        	Log.w(TAG, "______elementDotStyleTip: " + elementDotStyleTip);
			
			JSONObject xAxis = jo.getJSONObject("x_axis");
			int xAxisSteps = xAxis.getInt("steps");
        	Log.w(TAG, "______xAxisSteps: " + xAxisSteps);
        	int xAxisMin = xAxis.getInt("min");
        	chartData.min_x = xAxisMin;
        	Log.w(TAG, "______xAxisMin: " + xAxisMin);
        	int xAxisMax = xAxis.getInt("max");
        	chartData.max_x = xAxisMax;
        	Log.w(TAG, "______xAxisMax: " + xAxisMax);
			
			
			JSONObject yAxis = jo.getJSONObject("y_axis");
			int yAxisSteps = yAxis.getInt("steps");
        	Log.w(TAG, "______yAxisSteps: " + yAxisSteps);
        	int yAxisMin = yAxis.getInt("min");
        	chartData.min_y = yAxisMin;
        	Log.w(TAG, "______yAxisMin: " + yAxisMin);
        	int yAxisMax = yAxis.getInt("max");
        	chartData.max_y = yAxisMax;
        	Log.w(TAG, "______yAxisMax: " + yAxisMax);
			
			String bgColor = jo.getString("bg_colour");
			Log.w(TAG, "______fetchSensorData() bg_colour = " + bgColor);
    		
    	} catch (IOException e) {
    		e.printStackTrace();
    		return null;
    	} catch (JSONException e) {
    		Log.w(TAG, "______json exception: " + e);
    		return null;
    	}
    	
    	chartData.points = pointList;
    	
    	return chartData;
    }
    
    
    /**
     * Fetch sensor (type = 5) from server
     * 
     * @param deviceId
     * @param sensorId
     * @return ture: on, false: off
     */
    public static boolean getSensorStatus(final int deviceId, final int sensorId) {
    	
    	// http://www.yeelink.net/v1.0/device/3/sensor/5/datapoints
    	
    	HttpRequest req = null;
    	
    	HttpClient client = new DefaultHttpClient();
    	final String urlStr = String.format("http://www.yeelink.net/v1.0/device/%d/sensor/%d/datapoints", deviceId, sensorId);

        try {
        	HttpHost target = new HttpHost("www.yeelink.net");
        	HttpGet get = new HttpGet(urlStr);
        	
            req = get;
            req.addHeader("U-ApiKey", GlobalVars.ApiKey);
            
    		HttpResponse httpResponse = client.execute(target, req);
    		
			//
    		int statusCode = httpResponse.getStatusLine().getStatusCode();
    		Log.w(TAG, "______getSensorStatus() status code = " + statusCode);
			if (statusCode != 200) {
				return false;
			}
			
			String strResult = EntityUtils.toString(httpResponse.getEntity());
			Log.w(TAG, "______getSensorStatus() response = " + strResult);
			
			// response parse with json
			JSONObject jo = new JSONObject(strResult);
			String timestamp = jo.getString("timestamp");
			Log.w(TAG, "______timestamp = " + timestamp);
			int value = jo.getInt("value");
			Log.w(TAG, "______value = " + value);
			
			if (value == 1) {
				return true;
			}
    		
    	} catch (IOException e) {
    		e.printStackTrace();
    		return false;
    	} catch (JSONException e) {
    		Log.w(TAG, "______json exception: " + e);
    		return false;
    	}
    	
    	return false;
    }
    
    
    /**
     * Set sensor (type = 5) status
     * 
     * @param deviceId
     * @param sensorId
     * @param on
     * @return ture: success, false: fail
     */
    public static boolean setSensorStatus(final int deviceId, final int sensorId, boolean on) {
    	
    	// http://www.yeelink.net/v1.0/device/3/sensor/5/datapoints
    	
    	HttpRequest req = null;
    	
    	HttpClient client = new DefaultHttpClient();
    	final String urlStr = String.format("http://www.yeelink.net/v1.0/device/%d/sensor/%d/datapoints", deviceId, sensorId);
    	String entitybody = String.format("{\"value\":%d}", (on ? 1 : 0));
    	
        try {
        	HttpHost target = new HttpHost("www.yeelink.net");
        	HttpPost post = new HttpPost(urlStr);
        	ByteArrayEntity entity = new ByteArrayEntity(entitybody.getBytes());
        	post.setEntity(entity);
        	
        	req = post;
            req.addHeader("U-ApiKey", GlobalVars.ApiKey);
            
        	HttpResponse httpResponse = client.execute(target, req);
    		
			//
    		int statusCode = httpResponse.getStatusLine().getStatusCode();
    		Log.w(TAG, "______setSensorStatus() status code = " + statusCode);
			if (statusCode != 200) {
				return false;
			}
			
			//String strResult = EntityUtils.toString(httpResponse.getEntity());
			//Log.w(TAG, "______setSensorStatus() response = " + strResult);
			
			// response parse with json
			//JSONObject jo = new JSONObject(strResult);
    		
    	} catch (IOException e) {
    		e.printStackTrace();
    		return false;
    	}
    	
    	return true;
    }
    
    /**
     * Set sensor (type = 5) status
     * 
     * @param deviceId
     * @param sensorId
     * @param on
     * @return ture: success, false: fail
     */
    public static boolean setSensorData(final int deviceId, final int sensorId, float value) {
    	
    	// http://www.yeelink.net/v1.0/device/3/sensor/5/datapoints
    	
    	HttpRequest req = null;
    	
    	HttpClient client = new DefaultHttpClient();
    	final String urlStr = String.format("http://www.yeelink.net/v1.0/device/%d/sensor/%d/datapoints", deviceId, sensorId);
    	String entitybody = String.format("{\"value\":%f}", value);
    	
        try {
        	HttpHost target = new HttpHost("www.yeelink.net");
        	HttpPost post = new HttpPost(urlStr);
        	ByteArrayEntity entity = new ByteArrayEntity(entitybody.getBytes());
        	post.setEntity(entity);
        	
        	req = post;
            req.addHeader("U-ApiKey", GlobalVars.ApiKey);
            
        	HttpResponse httpResponse = client.execute(target, req);
    		
			//
    		int statusCode = httpResponse.getStatusLine().getStatusCode();
    		Log.w(TAG, "setSensorData() status code = " + statusCode);
			if (statusCode != 200) {
				return false;
			}
			
			//String strResult = EntityUtils.toString(httpResponse.getEntity());
			//Log.w(TAG, "______setSensorStatus() response = " + strResult);
			
			// response parse with json
			//JSONObject jo = new JSONObject(strResult);
    		
    	} catch (IOException e) {
    		e.printStackTrace();
    		return false;
    	}
    	
    	return true;
    }
    
    
    /**
     * Create a device on Yeelink Server
     * 
     * @param name
     * @param desc
     * @param tags
     * @return {@code int} device id
     */
    public static int createDevice(String name, String desc, String tags[]) {
    	
    	//ArrayList<Device> deviceList = new ArrayList<Device>();
    	
    	// http://www.yeelink.net/mobile/list_devices?login=demo&apikey=9cdf51696fa9ddfacdf819033a5f2f63
    	final String URLCreateDevice = "http://api.yeelink.net/v1.0/devices";
    	HttpClient client = new DefaultHttpClient();
    	//final String urlStr = String.format("%s/list_devices?login=%s&apikey=%s", BASE_URL, GlobalVars.Username, GlobalVars.ApiKey);
    	String entitybody = String.format("{\"title\":\"%s\", \"about\":\"%s\", \"tags\":[\"%s\"], \"location\":{\"local\":\"N/A\", \"latitude\":0, \"longitude\":0}}", 
    			name, desc, "TEST");
    	Log.i(TAG, entitybody);

        try {
        	HttpHost target = new HttpHost("www.yeelink.net");
        	HttpPost post = new HttpPost(URLCreateDevice);
			ByteArrayEntity entity = new ByteArrayEntity(entitybody.getBytes());
        	post.setEntity(entity);
        	
        	HttpRequest req = post;
            req.addHeader("U-ApiKey", GlobalVars.ApiKey);
            
        	HttpResponse httpResponse = client.execute(target, req);
    		
			//
    		int statusCode = httpResponse.getStatusLine().getStatusCode();
    		Log.w(TAG, "createDevice() status code = " + statusCode);
			if (statusCode != 200) {
				return 0;
			}
			
			// response = {"status":"success","desc":"Login and password are ok","data":{"apikey":"9cdf51696fa9ddfacdf819033a5f2f63"}}
			String strResult = EntityUtils.toString(httpResponse.getEntity());
			Log.w(TAG, "v() response = " + strResult);
			
			// response parse with json
			JSONObject jo = new JSONObject(strResult);
			Log.w(TAG, "createDevice() device_id = " + jo.getString("device_id"));
			return Integer.parseInt(jo.getString("device_id"));
    		
    	} catch (IOException e) {
    		e.printStackTrace();
    		return 0;
    	} catch (JSONException e) {
    		Log.w(TAG, "______json exception: " + e);
    		return 0;
    	}
    }
    
    static class Ylsensor{
    	public String title;
    	String about;
    	public String tags[];
    	
    	public static class unit{
    		public String name;
    		public String symbol;
    	}
    	public static class range{
    		public float min_value;
    		public float max_value;
    	}
    	
    	public unit unit = new unit();
    	public range range = new range();
    }
    
    /**
     * Create a sensor on Yeelink Server
     * 
     * @param deviceid
     * @param name
     * @param desc
     * @param tags
     * @return {@code int} sensor id
     */
    public static int createSensor(int deviceid, String name, String desc, String tags[],
    		String unitName, String unitSymbol, float minVal, float maxVal) {
    	
    	//ArrayList<Device> deviceList = new ArrayList<Device>();
    	final String URLCreateSensor = 
    			String.format("http://api.yeelink.net/v1.0/device/%d/sensors", deviceid);
    	HttpClient client = new DefaultHttpClient();
    	//final String urlStr = String.format("%s/list_devices?login=%s&apikey=%s", BASE_URL, GlobalVars.Username, GlobalVars.ApiKey);
    	Gson gson = new Gson();
    	Ylsensor ys = new Ylsensor();
    	ys.title = name;
    	ys.about = desc;
    	ys.tags = tags;
    	ys.unit.name = unitName;
    	ys.unit.symbol = unitSymbol;
    	ys.range.min_value = minVal;
    	ys.range.max_value = maxVal;
   
    	
    	String entitybody = gson.toJson(ys).toString();
    	Log.i(TAG, "createSensor URL:" + URLCreateSensor);
    	Log.i(TAG, "createSensor: " + entitybody);

        try {
        	HttpHost target = new HttpHost("www.yeelink.net");
        	HttpPost post = new HttpPost(URLCreateSensor);
			ByteArrayEntity entity = new ByteArrayEntity(entitybody.getBytes());
        	post.setEntity(entity);
        	
        	HttpRequest req = post;
            req.addHeader("U-ApiKey", GlobalVars.ApiKey);
            
        	HttpResponse httpResponse = client.execute(target, req);
    		
			//
    		int statusCode = httpResponse.getStatusLine().getStatusCode();
    		Log.w(TAG, "createSensor() status code = " + statusCode);
			if (statusCode != 200) {
				return 0;
			}
			
			// response = {"status":"success","desc":"Login and password are ok","data":{"apikey":"9cdf51696fa9ddfacdf819033a5f2f63"}}
			String strResult = EntityUtils.toString(httpResponse.getEntity());
			Log.w(TAG, "v() response = " + strResult);
			
			// response parse with json
			JSONObject jo = new JSONObject(strResult);
			Log.w(TAG, "createSensor() Sensor ID= " + jo.getString("sensor_id"));
			return Integer.parseInt(jo.getString("sensor_id"));
    		
    	} catch (IOException e) {
    		e.printStackTrace();
    		return 0;
    	} catch (JSONException e) {
    		Log.w(TAG, "______json exception: " + e);
    		return 0;
    	}
    }
}
