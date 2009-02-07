package playground.jjoubert.CommercialTraffic;

public class GPSPoint implements Comparable<GPSPoint>{
	int vehID;
	int time;
	double longitude;
	double latitude;
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
	public double getLongitude() {
		return longitude;
	}
	public void setLongitude(double x) {
		this.longitude = x;
	}
	public double getLatitude() {
		return latitude;
	}
	public void setLatitude(double y) {
		this.latitude = y;
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
