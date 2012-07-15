package net.zalio.android.yl.sensors;


public class Point {
	
	public int x;
	public int y;
	
	public Point() {
	}
	
	public Point(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	public String toString() {
		return String.format("[%d, %d]", x, y);
	}
}
