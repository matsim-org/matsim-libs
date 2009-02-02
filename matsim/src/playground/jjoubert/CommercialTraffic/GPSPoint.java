package playground.jjoubert.CommercialTraffic;

import java.util.Comparator;

public class GPSPoint implements Comparable<GPSPoint>{
	int vehID;
	int time;
	double x;
	double y;
	int status;
	int speed;
	public int getVehID() {
		return vehID;
	}
	public void setVehID(int vehID) {
		this.vehID = vehID;
	}
	public int getTime() {
		return time;
	}
	public void setTime(int time) {
		this.time = time;
	}
	public double getX() {
		return x;
	}
	public void setX(double x) {
		this.x = x;
	}
	public double getY() {
		return y;
	}
	public void setY(double y) {
		this.y = y;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public int getSpeed() {
		return speed;
	}
	public void setSpeed(int speed) {
		this.speed = speed;
	}
	public int compareTo(GPSPoint o) {
		return (this.time - o.time);
	}
}
