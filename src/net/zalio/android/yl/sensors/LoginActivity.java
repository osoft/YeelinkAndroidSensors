package net.zalio.android.yl.sensors;
import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.content.Intent;

public class LoginActivity extends Activity {
	static final String LOG_TAG = "LoginActivity";

	private static final int MSG_LOGIN_FINISH = 100;
	private static final int MSG_DEVICE_CREATED = 101;

	private static final int LOGIN_SUCCESS = 1;
	private static final int LOGIN_FAIL = 0;
	
	
	Button mBtnSignin;
	SharedPreferences appPrefs;
	CheckBox mCbStore;
	private ProgressDialog mProgressDialog = null;
	
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
						mHandler.sendEmptyMessage(MSG_DEVICE_CREATED);
					}
				}.start();
				break;
			}
			case MSG_DEVICE_CREATED:{
				Intent intent = new Intent();
				intent.setComponent(new ComponentName(LoginActivity.this, Sensors.class));
				startActivity(intent);
				finish();
				break;
			}
			}
		}
	};
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(!GlobalVars.ApiKey.equals("") 
				&& GlobalVars.DeviceID != 0 
				&& !GlobalVars.Username.equals("")) {
			Intent intent = new Intent();
			intent.setComponent(new ComponentName(LoginActivity.this, Sensors.class));
			intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
			startActivity(intent);
			finish();
		}
		
		setContentView(R.layout.login_activity);
		
		appPrefs= getSharedPreferences(Sensors.PREF_FILE, MODE_PRIVATE);
		mBtnSignin = (Button)findViewById(R.id.btnSignin);
		mCbStore = (CheckBox)findViewById(R.id.cbStorepasswd);
		String username = appPrefs.getString("username", "");
		String password = appPrefs.getString("password", "");
		((EditText)findViewById(R.id.etUsername)).setText(username);
		if(!password.equals("")) {
			((EditText)findViewById(R.id.etPassword)).setText(password);
			mCbStore.setChecked(true);
		}
		mBtnSignin.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				final String username = 
						((EditText)findViewById(R.id.etUsername)).getText().toString();
				final String password = 
						((EditText)findViewById(R.id.etPassword)).getText().toString();
				Log.w(LOG_TAG, "____username = " + username);
				Log.w(LOG_TAG, "____password = " + password);
				boolean storePwd = 
						((CheckBox)findViewById(R.id.cbStorepasswd)).isChecked();
				Editor ed = appPrefs.edit();
				ed.putString("username", username);
				if(storePwd) {
					ed.putString("password", password);
				}
				ed.commit();
				
				// 1. show progress dialog
				mProgressDialog = ProgressDialog.show(LoginActivity.this, null,
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
			}
			
		});
	}
}
