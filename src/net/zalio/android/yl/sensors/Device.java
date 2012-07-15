package net.zalio.android.yl.sensors;

public class Device {
	
	public int mId;
	public String mTitle;
	public String mAbout;
	
	public Device() {
	}
	
	public Device(int id, String title, String about) {
		mId = id;
		mTitle = title;
		mAbout = about;
	}
	
	public String toString() {
		return String.format("{id:%d, title:%s, about:%s}", mId, mTitle, mAbout);
	}
}
