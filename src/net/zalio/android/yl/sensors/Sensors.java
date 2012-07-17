package net.zalio.android.yl.sensors;

import java.util.ArrayList;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class Sensors extends ListActivity {
	public static final String PREF_CAPTURE_STATE = "captureState";
	public static final String PREF_SAMPLING_SPEED = "samplingSpeed";
	public static final boolean DEBUG = true;
	public static final String PREF_FILE = "prefs";
	static final int MENU_SETTINGS = 1;
	static final int MENU_LOGIN = 2;
	static final String LOG_TAG = "SENSORS";
	private ProgressDialog mProgressDialog = null;

	private static final int MSG_LOGIN_FINISH = 100;

	private static final int LOGIN_SUCCESS = 1;
	private static final int LOGIN_FAIL = 0;

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_LOGIN_FINISH: {
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
				new Thread() {
					@Override
					public void run() {
						String tags[] = { "android", "sensors" };
						ArrayList<Device> deviceArray = HttpUtils.fetchDeviceList();
						for(Device d:deviceArray){
							if(d.mTitle.equals("Android")){
								GlobalVars.DeviceID = d.mId;
								break;
							}
						}
						if(GlobalVars.DeviceID == 0){
							GlobalVars.DeviceID = HttpUtils.createDevice("Android",
									"Android Sensors", tags);	
						}
					}
				}.start();
				break;
			}
			}
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(LOG_TAG, "onCreate");
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			samplingServicePosition = savedInstanceState.getInt(
					SAMPLING_SERVICE_POSITION_KEY, -1);
			samplingServiceRunning = samplingServicePosition >= 0;
			Log.d(LOG_TAG, "reinitialized samplingServiceRunning: "
					+ samplingServiceRunning);
		}
		setContentView(R.layout.main);
		
		// Construct sensor list
		SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		ArrayList<SensorItem> items = new ArrayList<SensorItem>();
		List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
		for (int i = 0; i < sensors.size(); ++i) {
			items.add(new SensorItem(sensors.get(i)));
		}
		items.add(new SensorItem("GPS", 0xffff));
		if (samplingServiceRunning) {
			SensorItem item = items.get(samplingServicePosition);
			item.setSampling(true);
		} else {
			String activeSensor = "";
			ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
			List<RunningServiceInfo> list = am.getRunningServices(128);
			for (RunningServiceInfo info : list) {
				if (info.service.getClassName().equals(
						SamplingService.class.getName())) {
					activeSensor = SamplingService.getCurrentSensor();
				}
			}

			for (int i = 0; i < items.size(); i++) {
				if (items.get(i).getSensorName().equals(activeSensor)) {
					samplingServiceRunning = true;
					samplingServicePosition = i;
					items.get(i).setSampling(true);
					break;
				}
			}
		}
		
		// Fill listview
		ListView lv = getListView();
		listAdapter = new SensorListAdapter(this, items);
		lv.setAdapter(listAdapter);
		// Set up the long click handler
		lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> av, View v, int pos,
					long id) {
				Log.i(LOG_TAG, "onItemLongClick");
				onLongListItemClick(v, pos, id);
				return true;
			}
		});
	}

	@Override
	protected void onNewIntent(Intent intent) {
		Log.d(LOG_TAG, "onNewIntent");
	}

	// Save the information that
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt(SAMPLING_SERVICE_POSITION_KEY, samplingServicePosition);
	}

	protected void onDestroy() {
		super.onDestroy();
		if (Sensors.DEBUG)
			Log.d(LOG_TAG, "onDestroy");
	}

	// @Override
	// public boolean onKeyDown(int keyCode, KeyEvent event) {
	// if (keyCode == KeyEvent.KEYCODE_BACK) {
	// Intent intent = new Intent(Intent.ACTION_MAIN);
	// intent.addCategory(Intent.CATEGORY_HOME);
	// startActivity(intent);
	// return true;
	// }
	// return false;
	// }

	protected void onLongListItemClick(View v, int pos, long id) {
		if (Sensors.DEBUG)
			Log.d(LOG_TAG, "onLongListItemClick pos: " + pos + "; id: " + id);
		// If sampling is running on another sensor
		if (samplingServiceRunning && (pos != samplingServicePosition))
			startSamplingService(pos);
		else
		// If sampling is running on the same sensor
		if (samplingServiceRunning && (pos == samplingServicePosition)) {
			Log.i(LOG_TAG, "Trying to stop service!");
			stopSamplingService();
		} else
		// If no sampling is running then just start the sampling on the sensor
		if (!samplingServiceRunning)
			startSamplingService(pos);
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_SETTINGS, 1, R.string.menu_settings);
		//menu.add(0, MENU_LOGIN, 1, R.string.menu_login);
		return result;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch (id) {
		case MENU_LOGIN:
			final String username = "";
			final String password = "";
			Log.w(LOG_TAG, "____username = " + username);
			Log.w(LOG_TAG, "____password = " + password);

			// 1. show progress dialog
			mProgressDialog = ProgressDialog.show(Sensors.this, null,
			// "Please wait while logining...", //message
					getString(R.string.login_wait), true, // indeterminate
					false, // 是否可通过返回键取消对话框
					null);

			// 2. start thread to login with username and password
			new Thread() {
				public void run() {
					boolean ret = HttpUtils.login(username, password);
					mHandler.sendMessage(mHandler.obtainMessage(
							MSG_LOGIN_FINISH, ret ? LOGIN_SUCCESS : LOGIN_FAIL,
							0));
				}
			}.start();
			break;
		case MENU_SETTINGS:
			Intent i = new Intent();
			i.setClassName("net.zalio.android.yl.sensors",
					"net.zalio.android.yl.sensors.SensorSettings");
			startActivity(i);
			break;
		}
		return true;
	}

	protected void onListItemClick(ListView l, View v, int position, long id) {
		Log.i(LOG_TAG, "onListItemClick");
		// YeelinkSensor sensor = ((SensorItem) listAdapter.getItem(position))
		// .getSensor();
		// String sensorName = sensor.getName();
		// Intent i = new Intent();
		// i.setClassName("net.zalio.android.yl.sensors",
		// "net.zalio.android.yl.sensors.SensorMonitor");
		// i.putExtra("sensorname", sensorName);
		// startActivity(i);
		//
		onLongListItemClick(v, position, id);
	}

	private void startSamplingService(int position) {
		Log.i(LOG_TAG, "startSamplingService()");
		stopSamplingService();
		SensorItem item = (SensorItem) listAdapter.getItem(position);
		Sensor sensor = item.getSensor();
		//String sensorName = sensor.getName();
		String sensorName = item.getSensorName();
		Intent i = new Intent();
		i.setClassName("net.zalio.android.yl.sensors",
				"net.zalio.android.yl.sensors.SamplingService");
		i.putExtra("sensorname", sensorName);
		startService(i);
		samplingServiceRunning = true;
		samplingServicePosition = position;
		item.setSampling(true);
		listAdapter.notifyDataSetChanged();
		SharedPreferences appPrefs = getSharedPreferences(Sensors.PREF_FILE,
				MODE_PRIVATE);
		Editor ed = appPrefs.edit();
		ed.putString("currentSensor", sensorName);
		ed.commit();
	}

	private void stopSamplingService() {
		Log.i(LOG_TAG, "stopSamplingService()");
		if (samplingServiceRunning) {
			Intent i = new Intent();
			i.setClassName("net.zalio.android.yl.sensors",
					"net.zalio.android.yl.sensors.SamplingService");
			stopService(i);
			SensorItem item = (SensorItem) listAdapter
					.getItem(samplingServicePosition);
			item.setSampling(false);
			samplingServiceRunning = false;
			samplingServicePosition = -1;
			listAdapter.notifyDataSetChanged();
		}
		SharedPreferences appPrefs = getSharedPreferences(Sensors.PREF_FILE,
				MODE_PRIVATE);
		Editor ed = appPrefs.edit();
		ed.putString("currentSensor", "");
		ed.commit();
	}

	private SensorListAdapter listAdapter;
	private boolean samplingServiceRunning = false;
	private int samplingServicePosition = 0;
	private static final String SAMPLING_SERVICE_POSITION_KEY = "samplingServicePositon";
}
